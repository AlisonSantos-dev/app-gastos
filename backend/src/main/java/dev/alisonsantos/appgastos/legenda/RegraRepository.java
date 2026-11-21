package dev.alisonsantos.appgastos.legenda;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegraRepository extends JpaRepository<Regra, Long> {

    /**
     * As regras ativas, já com a pessoa carregada (@EntityGraph).
     * É o que a legenda usa para identificar as compras.
     */
    @EntityGraph(attributePaths = "pessoa")
    List<Regra> findByAtivaTrue();

    /** Quantas transações cada regra identificou. Mostra quais regras estão "trabalhando". */
    @Query("""
            SELECT t.regra.id AS regraId, COUNT(t) AS usos
            FROM Transacao t
            WHERE t.regra IS NOT NULL
            GROUP BY t.regra.id
            """)
    List<UsoRegra> contarUsos();

    interface UsoRegra {
        Long getRegraId();

        Long getUsos();
    }
}
