package dev.alisonsantos.appgastos.caixa;

import dev.alisonsantos.appgastos.comum.PaginaResponse;
import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.edicao.PropagadorParcelas;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A caixa de entrada: os gastos que precisam da sua revisão.
 *
 * Um gasto está na caixa quando AINDA NÃO FOI REVISADO e:
 *   - está SEM DONO (cartão compartilhado ou conta sem dono), ou
 *   - está SEM LEGENDA (nenhuma regra reconheceu, e você não deu nome à mão).
 *
 * Ele sai quando você escolhe o dono ou confirma. Nas compras parceladas,
 * a ação vale para todas as parcelas, e as futuras já chegam revisadas.
 */
@Service
public class CaixaService {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final int TAMANHO_MAXIMO = 200;

    private final TransacaoRepository transacaoRepository;
    private final PessoaService pessoaService;
    private final PropagadorParcelas propagadorParcelas;

    public CaixaService(TransacaoRepository transacaoRepository, PessoaService pessoaService,
                        PropagadorParcelas propagadorParcelas) {
        this.transacaoRepository = transacaoRepository;
        this.pessoaService = pessoaService;
        this.propagadorParcelas = propagadorParcelas;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaResponse<ItemCaixaResponse> listar(MotivoPendencia motivo, int pagina, int tamanho) {
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), tamanhoSeguro,
                Sort.by(Sort.Direction.DESC, "dataTransacao").and(Sort.by(Sort.Direction.DESC, "id")));

        return PaginaResponse.de(transacaoRepository.findAll(pendentes(motivo), paginacao)
                .map(ItemCaixaResponse::de));
    }

    @Transactional(readOnly = true)
    public ResumoCaixaResponse resumo() {
        return new ResumoCaixaResponse(
                transacaoRepository.count(pendentes(null)),
                transacaoRepository.count(pendentes(MotivoPendencia.SEM_DONO)),
                transacaoRepository.count(pendentes(MotivoPendencia.SEM_LEGENDA)));
    }

    // ------------------------------------------------------------------
    // Ações
    // ------------------------------------------------------------------

    /** O clique no botão da pessoa: define o dono (à mão) e tira da caixa. */
    @Transactional
    public ResultadoConfirmacao definirDono(Long id, Long pessoaId) {
        Transacao transacao = encontrar(id);
        if (!transacao.isContaNosGastos()) {
            throw new RegraNegocioException("Só gastos têm dono. Esta transação é " + transacao.getTipoLancamento() + ".");
        }

        Pessoa pessoa = pessoaService.encontrar(pessoaId);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }

        List<Transacao> parcelas = propagadorParcelas.parcelasDaMesmaCompra(transacao);
        for (Transacao parcela : parcelas) {
            parcela.definirPessoa(pessoa, OrigemPessoa.MANUAL);
            parcela.marcarRevisada();
        }
        return new ResultadoConfirmacao(parcelas.size(), List.of());
    }

    /** Confirma uma ou várias compras como estão. Só as que têm dono podem ser confirmadas. */
    @Transactional
    public ResultadoConfirmacao confirmar(List<Long> ids) {
        Set<Transacao> confirmadas = new LinkedHashSet<>();
        List<String> avisos = new ArrayList<>();

        for (Long id : new LinkedHashSet<>(ids)) {
            Transacao transacao = transacaoRepository.findById(id).orElse(null);
            if (transacao == null) {
                avisos.add("Transação " + id + ": não encontrada.");
                continue;
            }
            if (transacao.isContaNosGastos() && transacao.getPessoa() == null) {
                avisos.add("Transação " + id + " (" + transacao.getDescricaoExibicao()
                        + "): escolha o dono antes de confirmar.");
                continue;
            }
            for (Transacao parcela : propagadorParcelas.parcelasDaMesmaCompra(transacao)) {
                parcela.marcarRevisada();
                confirmadas.add(parcela);
            }
        }
        return new ResultadoConfirmacao(confirmadas.size(), avisos);
    }

    /** Limpeza do histórico: confirma todos os gastos até a data, inclusive. */
    @Transactional
    public ResultadoConfirmacao confirmarAte(LocalDate data) {
        Instant limite = data.plusDays(1).atStartOfDay(FUSO_BRASIL).toInstant();
        int quantidade = transacaoRepository.marcarRevisadasAntesDe(limite, Instant.now());
        return new ResultadoConfirmacao(quantidade, List.of());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /**
     * O filtro da caixa de entrada. Mesma regra do MotivoPendencia.de(...),
     * só que escrita como consulta ao banco.
     */
    private Specification<Transacao> pendentes(MotivoPendencia motivo) {
        return (root, query, cb) -> {
            Predicate gastoNaoRevisado = cb.and(
                    cb.isTrue(root.<Boolean>get("contaNosGastos")),
                    cb.isFalse(root.<Boolean>get("revisada")));

            // Sem dono e não dividida (a divisão também resolve o dono)
            Predicate semDono = cb.and(
                    cb.isNull(root.get("pessoa")),
                    cb.or(cb.isNull(root.get("origemPessoa")),
                            cb.notEqual(root.get("origemPessoa"), OrigemPessoa.DIVISAO)));
            Predicate semLegenda = cb.and(
                    cb.isNull(root.get("regra")),
                    cb.notEqual(root.get("origemDescricao"), OrigemDescricao.MANUAL));

            Predicate pendencia;
            if (motivo == MotivoPendencia.SEM_DONO) {
                pendencia = semDono;
            } else if (motivo == MotivoPendencia.SEM_LEGENDA) {
                pendencia = semLegenda;
            } else {
                pendencia = cb.or(semDono, semLegenda);
            }
            return cb.and(gastoNaoRevisado, pendencia);
        };
    }

    private Transacao encontrar(Long id) {
        return transacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Transação", id));
    }
}
