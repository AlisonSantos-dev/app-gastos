package dev.alisonsantos.appgastos.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acesso ao banco para Pessoa.
 *
 * Só de estender JpaRepository, o Spring já cria os métodos básicos:
 * save, findById, findAll, deleteById, count...
 *
 * Os métodos abaixo são "consultas derivadas": o Spring lê o nome do método
 * e monta o SQL sozinho.
 */
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    /** SELECT * FROM pessoa WHERE ativo = true ORDER BY nome ASC */
    List<Pessoa> findByAtivoTrueOrderByNomeAsc();

    /** Já existe alguém com esse nome? (sem diferenciar maiúsculas) */
    boolean existsByNomeIgnoreCase(String nome);

    /** Já existe OUTRA pessoa com esse nome? Usado na edição. */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
