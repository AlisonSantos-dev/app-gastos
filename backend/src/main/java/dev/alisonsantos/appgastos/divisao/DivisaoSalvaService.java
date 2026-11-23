package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.legenda.LegendaService;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cadastro das divisões salvas.
 *
 * Mudou uma divisão (ex.: entrou mais uma pessoa no Spotify Família)?
 * A legenda é reaplicada na hora, e as compras divididas por regra com
 * esse modelo são recalculadas.
 */
@Service
public class DivisaoSalvaService {

    /** Usado só para validar o modelo (a soma dos percentuais, as pessoas...). */
    private static final BigDecimal VALOR_DE_TESTE = new BigDecimal("100.00");

    private final DivisaoSalvaRepository repository;
    private final PessoaService pessoaService;
    private final LegendaService legendaService;

    public DivisaoSalvaService(DivisaoSalvaRepository repository, PessoaService pessoaService,
                               LegendaService legendaService) {
        this.repository = repository;
        this.pessoaService = pessoaService;
        this.legendaService = legendaService;
    }

    @Transactional(readOnly = true)
    public List<DivisaoSalvaResponse> listar(boolean incluirInativas) {
        return repository.findAllByOrderByNomeAsc().stream()
                .filter(d -> incluirInativas || d.isAtiva())
                .map(DivisaoSalvaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public DivisaoSalvaResponse buscar(Long id) {
        return DivisaoSalvaResponse.de(encontrar(id));
    }

    @Transactional
    public DivisaoSalvaResponse criar(DivisaoSalvaRequest pedido) {
        String nome = pedido.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe uma divisão chamada \"" + nome + "\".");
        }
        DivisaoSalva divisao = new DivisaoSalva(nome, modoValido(pedido.modo()));
        divisao.definirPartes(integrantes(pedido));
        validarCalculo(divisao);
        return DivisaoSalvaResponse.de(repository.save(divisao));
    }

    @Transactional
    public DivisaoSalvaResponse atualizar(Long id, DivisaoSalvaRequest pedido) {
        DivisaoSalva divisao = encontrar(id);
        String nome = pedido.nome().trim();
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe outra divisão chamada \"" + nome + "\".");
        }
        divisao.setNome(nome);
        divisao.setModo(modoValido(pedido.modo()));
        divisao.definirPartes(integrantes(pedido));
        validarCalculo(divisao);

        // As compras divididas por regra com este modelo são recalculadas
        legendaService.reaplicarTodas();
        return DivisaoSalvaResponse.de(divisao);
    }

    /** "Excluir" = desativar (as regras que usam este modelo continuam apontando para ele). */
    @Transactional
    public void desativar(Long id) {
        encontrar(id).setAtiva(false);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private ModoDivisao modoValido(ModoDivisao modo) {
        if (modo == ModoDivisao.VALOR) {
            throw new RegraNegocioException(
                    "Divisões salvas usam IGUAL ou PERCENTUAL: valores fixos não servem para compras de valores diferentes.");
        }
        return modo;
    }

    private List<DivisaoSalva.Integrante> integrantes(DivisaoSalvaRequest pedido) {
        return pedido.partes().stream()
                .map(parte -> new DivisaoSalva.Integrante(pessoaAtiva(parte.pessoaId()), parte.percentual()))
                .toList();
    }

    /**
     * Valida o modelo usando a própria CalculadoraDivisao, com um valor de teste:
     * se ela aceitar (pessoas distintas, percentuais somando 100...), o modelo é válido.
     */
    private void validarCalculo(DivisaoSalva divisao) {
        CalculadoraDivisao.calcular(divisao.getModo(), divisao.paraEntradas(), VALOR_DE_TESTE);
    }

    private DivisaoSalva encontrar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Divisão salva", id));
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }
}
