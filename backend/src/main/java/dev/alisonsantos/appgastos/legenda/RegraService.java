package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cadastro das regras da legenda.
 *
 * Toda mudança (criar, editar, desativar) reaplica a legenda em todas as
 * transações na hora: o efeito da regra aparece imediatamente, inclusive
 * nas compras antigas.
 */
@Service
public class RegraService {

    private static final int TAMANHO_CNPJ = 14;
    private static final int TAMANHO_MCC = 4;

    private final RegraRepository regraRepository;
    private final PessoaService pessoaService;
    private final LegendaService legendaService;

    public RegraService(RegraRepository regraRepository, PessoaService pessoaService,
                        LegendaService legendaService) {
        this.regraRepository = regraRepository;
        this.pessoaService = pessoaService;
        this.legendaService = legendaService;
    }

    @Transactional(readOnly = true)
    public List<RegraResponse> listar(boolean incluirInativas) {
        Map<Long, Long> usos = usosPorRegra();
        return regraRepository.findAllByOrderByTipoChaveAscValorChaveAsc().stream()
                .filter(regra -> incluirInativas || regra.isAtiva())
                .map(regra -> RegraResponse.de(regra, usos.getOrDefault(regra.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegraResponse buscar(Long id) {
        return RegraResponse.de(encontrar(id), usosPorRegra().getOrDefault(id, 0L));
    }

    @Transactional
    public RegraResponse criar(RegraRequest pedido) {
        Regra regra = criarSemReaplicar(pedido);
        legendaService.reaplicarTodas();
        return RegraResponse.de(regra, usosPorRegra().getOrDefault(regra.getId(), 0L));
    }

    /**
     * Cria várias regras de uma vez e reaplica a legenda UMA vez só no final.
     * Usado pelo importador da planilha: reaplicar a cada regra seria lento.
     */
    @Transactional
    public List<RegraResponse> criarVarias(List<RegraRequest> pedidos) {
        List<Regra> criadas = pedidos.stream().map(this::criarSemReaplicar).toList();
        legendaService.reaplicarTodas();

        Map<Long, Long> usos = usosPorRegra();
        return criadas.stream()
                .map(regra -> RegraResponse.de(regra, usos.getOrDefault(regra.getId(), 0L)))
                .toList();
    }

    private Regra criarSemReaplicar(RegraRequest pedido) {
        Regra regra = new Regra(pedido.tipoChave(), pedido.valorChave());
        preencherEValidar(regra, pedido);
        return regraRepository.save(regra);
    }

    @Transactional
    public RegraResponse atualizar(Long id, RegraRequest pedido) {
        Regra regra = encontrar(id);
        regra.definirChave(pedido.tipoChave(), pedido.valorChave());
        preencherEValidar(regra, pedido);

        legendaService.reaplicarTodas();
        return RegraResponse.de(regra, usosPorRegra().getOrDefault(id, 0L));
    }

    /**
     * "Excluir" = desativar. As compras que ela identificava voltam a ser
     * decididas pelas outras regras (ou pelo banco e pelo cartão).
     */
    @Transactional
    public void desativar(Long id) {
        encontrar(id).setAtiva(false);
        legendaService.reaplicarTodas();
    }

    // ------------------------------------------------------------------
    // Validações
    // ------------------------------------------------------------------

    private void preencherEValidar(Regra regra, RegraRequest pedido) {
        String descricao = textoOuNulo(pedido.descricao());
        String categoria = textoOuNulo(pedido.categoria());
        Pessoa pessoa = pedido.pessoaId() == null ? null : pessoaAtiva(pedido.pessoaId());

        validarValorDaChave(regra);

        if (descricao == null && categoria == null && pessoa == null) {
            throw new RegraNegocioException(
                    "A regra precisa definir pelo menos uma coisa: descricao, categoria ou pessoaId.");
        }
        if (regra.getTipoChave() == TipoChave.MCC && (descricao != null || pessoa != null)) {
            throw new RegraNegocioException(
                    "Regras de MCC só definem a categoria: o MCC é amplo demais para dar nome ou dono.");
        }
        if (pedido.vigenciaInicio() != null && pedido.vigenciaFim() != null
                && pedido.vigenciaFim().isBefore(pedido.vigenciaInicio())) {
            throw new RegraNegocioException("O fim da vigência não pode ser antes do início.");
        }

        regra.setCartaoFinal(pedido.cartaoFinal());
        regra.setDescricao(descricao);
        regra.setCategoria(categoria);
        regra.setPessoa(pessoa);
        regra.setPrioridade(pedido.prioridade() == null ? 0 : pedido.prioridade());
        regra.definirVigencia(pedido.vigenciaInicio(), pedido.vigenciaFim());

        validarDuplicada(regra);
    }

    private void validarValorDaChave(Regra regra) {
        String valor = regra.getValorChave();
        switch (regra.getTipoChave()) {
            case CNPJ -> {
                if (valor.length() != TAMANHO_CNPJ) {
                    throw new RegraNegocioException("O CNPJ precisa ter 14 dígitos.");
                }
            }
            case MCC -> {
                if (valor.length() != TAMANHO_MCC) {
                    throw new RegraNegocioException("O MCC precisa ter 4 dígitos (ex.: 5411).");
                }
            }
            default -> {
                if (valor.isEmpty()) {
                    throw new RegraNegocioException("O valor da chave não pode ficar vazio.");
                }
            }
        }
    }

    /** Não pode existir outra regra ativa com a mesma chave, o mesmo cartão e a mesma vigência. */
    private void validarDuplicada(Regra regra) {
        regraRepository.findByAtivaTrue().stream()
                .filter(outra -> !Objects.equals(outra.getId(), regra.getId()))
                .filter(outra -> outra.getTipoChave() == regra.getTipoChave()
                        && outra.getValorChave().equals(regra.getValorChave())
                        && Objects.equals(outra.getCartaoFinal(), regra.getCartaoFinal())
                        && Objects.equals(outra.getVigenciaInicio(), regra.getVigenciaInicio())
                        && Objects.equals(outra.getVigenciaFim(), regra.getVigenciaFim()))
                .findFirst()
                .ifPresent(outra -> {
                    throw new RegraNegocioException(
                            "Já existe uma regra igual (id " + outra.getId() + "). Edite ela em vez de criar outra.");
                });
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Regra encontrar(Long id) {
        return regraRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Regra", id));
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }

    private Map<Long, Long> usosPorRegra() {
        return regraRepository.contarUsos().stream()
                .collect(Collectors.toMap(RegraRepository.UsoRegra::getRegraId, RegraRepository.UsoRegra::getUsos));
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
