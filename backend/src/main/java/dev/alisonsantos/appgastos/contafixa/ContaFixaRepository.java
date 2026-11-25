package dev.alisonsantos.appgastos.contafixa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaFixaRepository extends JpaRepository<ContaFixa, Long> {

    @EntityGraph(attributePaths = {"conta", "pessoa", "divisaoSalva"})
    List<ContaFixa> findAllByOrderByGrupoAscNomeAsc();

    /** As ativas, já com tudo o que o cálculo do mês precisa. */
    @EntityGraph(attributePaths = {"conta", "pessoa", "divisaoSalva", "divisaoSalva.partes", "divisaoSalva.partes.pessoa"})
    List<ContaFixa> findByAtivaTrue();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
