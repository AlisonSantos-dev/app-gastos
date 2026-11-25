package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.ContaRepository;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.divisao.DivisaoSalvaRepository;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadastro das contas fixas.
 */
@Service
public class ContaFixaService {

    private final ContaFixaRepository repository;
    private final ContaRepository contaRepository;
    private final DivisaoSalvaRepository divisaoSalvaRepository;
    private final PessoaService pessoaService;

    public ContaFixaService(ContaFixaRepository repository, ContaRepository contaRepository,
                            DivisaoSalvaRepository divisaoSalvaRepository, PessoaService pessoaService) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.divisaoSalvaRepository = divisaoSalvaRepository;
        this.pessoaService = pessoaService;
    }

    @Transactional(readOnly = true)
    public List<ContaFixaResponse> listar(boolean incluirInativas) {
        return repository.findAllByOrderByGrupoAscNomeAsc().stream()
                .filter(c -> incluirInativas || c.isAtiva())
                .map(ContaFixaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaFixaResponse buscar(Long id) {
        return ContaFixaResponse.de(encontrar(id));
    }

    @Transactional
    public ContaFixaResponse criar(ContaFixaRequest pedido) {
        String nome = pedido.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe uma conta fixa chamada \"" + nome + "\".");
        }
        ContaFixa conta = new ContaFixa(nome, pedido.diaVencimento(), pedido.inicio());
        preencher(conta, pedido);
        return ContaFixaResponse.de(repository.save(conta));
    }

    @Transactional
    public ContaFixaResponse atualizar(Long id, ContaFixaRequest pedido) {
        ContaFixa conta = encontrar(id);
        String nome = pedido.nome().trim();
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe outra conta fixa chamada \"" + nome + "\".");
        }
        conta.setNome(nome);
        conta.setDiaVencimento(pedido.diaVencimento());
        preencher(conta, pedido);
        return ContaFixaResponse.de(conta);
    }

    /** "Excluir" = desativar: os meses passados continuam no histórico. */
    @Transactional
    public void desativar(Long id) {
        encontrar(id).setAtiva(false);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private void preencher(ContaFixa conta, ContaFixaRequest pedido) {
        if ((pedido.pessoaId() == null) == (pedido.divisaoSalvaId() == null)) {
            throw new RegraNegocioException(
                    "Informe de quem é a conta: pessoaId OU divisaoSalvaId (exatamente um dos dois).");
        }
        if (pedido.fim() != null && pedido.fim().isBefore(pedido.inicio())) {
            throw new RegraNegocioException("O mês de fim não pode ser antes do mês de início.");
        }

        conta.setGrupo(textoOuNulo(pedido.grupo()));
        conta.setConta(pedido.contaId() == null ? null : contaCorrente(pedido.contaId()));
        conta.setValorEsperado(pedido.valorEsperado());
        conta.definirDestino(
                pedido.pessoaId() == null ? null : pessoaAtiva(pedido.pessoaId()),
                pedido.divisaoSalvaId() == null ? null : divisaoAtiva(pedido.divisaoSalvaId()));
        conta.definirChavePagamento(pedido.chavePagamento());
        conta.setCategoria(textoOuNulo(pedido.categoria()));
        conta.definirVigencia(pedido.inicio(), pedido.fim());
    }

    private Conta contaCorrente(Long id) {
        Conta conta = contaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta", id));
        if (conta.getTipo() != TipoConta.CORRENTE) {
            throw new RegraNegocioException("A conta de pagamento precisa ser uma conta corrente (não um cartão).");
        }
        return conta;
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }

    private DivisaoSalva divisaoAtiva(Long id) {
        DivisaoSalva divisao = divisaoSalvaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Divisão salva", id));
        if (!divisao.isAtiva()) {
            throw new RegraNegocioException("A divisão \"" + divisao.getNome() + "\" está desativada.");
        }
        return divisao;
    }

    private ContaFixa encontrar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta fixa", id));
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
