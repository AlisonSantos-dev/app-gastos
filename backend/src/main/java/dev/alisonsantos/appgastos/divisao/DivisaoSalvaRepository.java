package dev.alisonsantos.appgastos.divisao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DivisaoSalvaRepository extends JpaRepository<DivisaoSalva, Long> {

    @EntityGraph(attributePaths = {"partes", "partes.pessoa"})
    List<DivisaoSalva> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
