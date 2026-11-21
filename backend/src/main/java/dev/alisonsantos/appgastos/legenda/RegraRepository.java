package dev.alisonsantos.appgastos.legenda;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface RegraRepository extends JpaRepository<Regra, Long> {

    /**
     * As regras ativas, já com a pessoa carregada (@EntityGraph).
     * É o que a legenda usa para identificar as compras.
     */
    @EntityGraph(attributePaths = "pessoa")
    List<Regra> findByAtivaTrue();

    /** Todas as regras (ativas e desativadas), com a pessoa. */
    @EntityGraph(attributePaths = "pessoa")
    List<Regra> findAllByOrderByTipoChaveAscValorChaveAsc();

    /** Quantas transações cada regra identificou. Mostra quais regras estão "trabalhando". */
    @Query("""
            SELECT t.regra.id AS regraId, COUNT(t) AS usos
            FROM Transacao t
            WHERE t.regra IS NOT NULL
            GROUP BY t.regra.id
            """)
    List<UsoRegra> contarUsos();

    /**
     * As descrições de gastos que nenhuma regra reconhece, das mais
     * frequentes para as menos. É a lista de "próximas regras a criar":
     * uma regra para o topo dessa lista resolve muitas compras de uma vez.
     */
    @Query("""
            SELECT t.descricaoBanco AS descricao,
                   COUNT(t) AS quantidade,
                   SUM(t.valor) AS total,
                   MAX(t.dataTransacao) AS ultima
            FROM Transacao t
            WHERE t.contaNosGastos = true
              AND t.regra IS NULL
              AND t.origemDescricao <> dev.alisonsantos.appgastos.transacao.OrigemDescricao.MANUAL
            GROUP BY t.descricaoBanco
            ORDER BY COUNT(t) DESC, MAX(t.dataTransacao) DESC
            """)
    List<DescricaoPendente> buscarDescricoesSemRegra(Pageable limite);

    interface UsoRegra {
        Long getRegraId();

        Long getUsos();
    }

    interface DescricaoPendente {
        String getDescricao();

        Long getQuantidade();

        BigDecimal getTotal();

        Instant getUltima();
    }
}
