package dev.alisonsantos.appgastos.pessoa;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Regras do cadastro de pessoas.
 *
 * Uma pessoa NUNCA é apagada do banco, só desativada: as transações
 * antigas continuam apontando para ela, e o histórico dos fechamentos
 * passados não pode mudar.
 */
@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar(boolean incluirInativas) {
        List<Pessoa> pessoas = incluirInativas
                ? pessoaRepository.findAll(Sort.by("nome"))
                : pessoaRepository.findByAtivoTrueOrderByNomeAsc();
        return pessoas.stream().map(PessoaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscar(Long id) {
        return PessoaResponse.de(encontrar(id));
    }

    @Transactional
    public PessoaResponse criar(PessoaRequest pedido) {
        String nome = pedido.nome().trim();
        if (pessoaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe uma pessoa chamada \"" + nome + "\".");
        }

        Pessoa pessoa = new Pessoa(nome, pedido.tipo());
        aplicarOpcionais(pessoa, pedido);
        return PessoaResponse.de(pessoaRepository.save(pessoa));
    }

    @Transactional
    public PessoaResponse atualizar(Long id, PessoaRequest pedido) {
        Pessoa pessoa = encontrar(id);
        String nome = pedido.nome().trim();
        if (pessoaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe outra pessoa chamada \"" + nome + "\".");
        }

        pessoa.setNome(nome);
        pessoa.setTipo(pedido.tipo());
        aplicarOpcionais(pessoa, pedido);
        // Sem save(): a pessoa veio do banco nesta transação, então o
        // Hibernate grava as mudanças sozinho no final (dirty checking).
        return PessoaResponse.de(pessoa);
    }

    /** "Excluir" = desativar. A pessoa some das listas, mas o histórico fica. */
    @Transactional
    public void desativar(Long id) {
        encontrar(id).setAtivo(false);
    }

    /** Usado também por outros serviços (ex.: definir o dono de um cartão). */
    @Transactional(readOnly = true)
    public Pessoa encontrar(Long id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pessoa", id));
    }

    /** Os campos opcionais: cor, nome no extrato, ativo e titular. */
    private void aplicarOpcionais(Pessoa pessoa, PessoaRequest pedido) {
        pessoa.setCor(pedido.cor() == null ? null : pedido.cor().toUpperCase(Locale.ROOT));
        pessoa.setNomeNoExtrato(textoOuNulo(pedido.nomeNoExtrato()));
        if (pedido.ativo() != null) {
            pessoa.setAtivo(pedido.ativo());
        }
        if (pedido.titular() != null) {
            pessoa.setTitular(pedido.titular());
        }
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

}
