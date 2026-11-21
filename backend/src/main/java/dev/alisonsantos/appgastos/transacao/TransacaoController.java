package dev.alisonsantos.appgastos.transacao;

import dev.alisonsantos.appgastos.comum.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * GET /api/transacoes
 *
 * Exemplos:
 *   /api/transacoes                                   (as 50 mais recentes)
 *   /api/transacoes?cartao=4948&somenteGastos=true
 *   /api/transacoes?faturaId=1&pagina=0&tamanho=100
 *   /api/transacoes?tipo=ESTORNO
 *   /api/transacoes?de=2026-11-01&ate=2026-11-30&texto=uber
 */
@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoConsultaService consultaService;

    public TransacaoController(TransacaoConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    public PaginaResponse<TransacaoResponse> listar(
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) String cartao,
            @RequestParam(required = false) Long faturaId,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(defaultValue = "false") boolean somenteGastos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {

        TransacaoFiltro filtro = new TransacaoFiltro(contaId, cartao, faturaId, tipo, somenteGastos, de, ate, texto);
        return consultaService.buscar(filtro, pagina, tamanho);
    }
}
