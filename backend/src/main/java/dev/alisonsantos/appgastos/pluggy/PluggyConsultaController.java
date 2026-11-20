package dev.alisonsantos.appgastos.pluggy;

import dev.alisonsantos.appgastos.pluggy.dto.PluggyAccount;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyBill;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyTransaction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de diagnóstico para conferir as consultas à Pluggy.
 *
 * Eles NÃO salvam nada no banco: só buscam na Pluggy e mostram um resumo.
 * Servem para testar a integração agora e para investigar problemas de
 * sincronização depois. Na Fase 4 passam a exigir login de admin.
 *
 * GET /api/pluggy/contas
 * GET /api/pluggy/transacoes?accountId=...&de=2026-10-01&ate=2026-10-31
 * GET /api/pluggy/faturas?accountId=...
 */
@RestController
@RequestMapping("/api/pluggy")
public class PluggyConsultaController {

    private static final int TAMANHO_AMOSTRA = 5;

    private final PluggyApiClient apiClient;
    private final PluggyProperties properties;

    public PluggyConsultaController(PluggyApiClient apiClient, PluggyProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // Contas de todos os bancos configurados no .env
    // ------------------------------------------------------------------

    @GetMapping("/contas")
    public ResponseEntity<?> contas() {
        if (properties.itemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensagem", "Nenhum banco configurado. Defina PLUGGY_ITEM_IDS no .env."));
        }

        List<ContaResumo> contas = new ArrayList<>();
        for (String itemId : properties.itemIds()) {
            for (PluggyAccount conta : apiClient.listarContas(itemId.trim())) {
                contas.add(new ContaResumo(
                        conta.itemId(),
                        conta.id(),
                        conta.type(),
                        conta.subtype(),
                        conta.name(),
                        ultimosDigitos(conta.number())));
            }
        }
        return ResponseEntity.ok(contas);
    }

    // ------------------------------------------------------------------
    // Resumo das transações de uma conta
    // ------------------------------------------------------------------

    @GetMapping("/transacoes")
    public ResumoTransacoes transacoes(
            @RequestParam String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        List<PluggyTransaction> transacoes = apiClient.listarTransacoes(accountId, de, ate);

        BigDecimal soma = transacoes.stream()
                .map(PluggyTransaction::amount)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransacaoResumo> amostra = transacoes.stream()
                .sorted(Comparator.comparing(PluggyTransaction::date,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TAMANHO_AMOSTRA)
                .map(this::resumir)
                .toList();

        return new ResumoTransacoes(transacoes.size(), soma, amostra);
    }

    // ------------------------------------------------------------------
    // Faturas de uma conta de cartão
    // ------------------------------------------------------------------

    @GetMapping("/faturas")
    public List<FaturaResumo> faturas(@RequestParam String accountId) {
        return apiClient.listarFaturas(accountId).stream()
                .sorted(Comparator.comparing(PluggyBill::dueDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(fatura -> new FaturaResumo(
                        fatura.id(),
                        fatura.billClosingDate(),
                        fatura.dueDate(),
                        fatura.totalAmount()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Erros da Pluggy viram 502 com a mensagem em português
    // ------------------------------------------------------------------

    @ExceptionHandler(PluggyException.class)
    public ResponseEntity<Map<String, String>> erroPluggy(PluggyException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("mensagem", e.getMessage()));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private TransacaoResumo resumir(PluggyTransaction t) {
        String cartao = t.creditCardMetadata() == null ? null : t.creditCardMetadata().cardNumber();
        String parcela = null;
        if (t.creditCardMetadata() != null && t.creditCardMetadata().totalInstallments() != null) {
            parcela = t.creditCardMetadata().installmentNumber() + "/" + t.creditCardMetadata().totalInstallments();
        }
        return new TransacaoResumo(t.date(), t.description(), t.amount(), cartao, parcela, t.status());
    }

    /** Mostra só os 4 últimos dígitos de um número de conta ou cartão. */
    private String ultimosDigitos(String numero) {
        if (numero == null || numero.length() <= 4) {
            return numero;
        }
        return "..." + numero.substring(numero.length() - 4);
    }

    // ------------------------------------------------------------------
    // Respostas (records dentro do controller, porque só ele usa)
    // ------------------------------------------------------------------

    public record ContaResumo(String itemId, String accountId, String tipo, String subtipo,
                              String nome, String final4) {
    }

    public record TransacaoResumo(Instant data, String descricao, BigDecimal valor,
                                  String cartao, String parcela, String status) {
    }

    public record ResumoTransacoes(int quantidade, BigDecimal soma, List<TransacaoResumo> maisRecentes) {
    }

    public record FaturaResumo(String id, Instant fechamento, Instant vencimento, BigDecimal total) {
    }
}
