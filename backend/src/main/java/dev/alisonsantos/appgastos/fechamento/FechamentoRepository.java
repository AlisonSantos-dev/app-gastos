package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.fatura.Fatura;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechamentoRepository extends JpaRepository<Fechamento, Long> {

    @EntityGraph(attributePaths = {"itens", "itens.pessoa"})
    Optional<Fechamento> findByCompetencia(LocalDate competencia);

    boolean existsByCompetencia(LocalDate competencia);

    // ------------------------------------------------------------------
    // O que entra no mês
    // ------------------------------------------------------------------

    /** Os gastos das faturas que vencem no mês (competência = mês do vencimento). */
    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.fatura f
            JOIN FETCH t.conta
            LEFT JOIN FETCH t.cartao
            LEFT JOIN FETCH t.pessoa
            WHERE f.dataVencimento >= :inicio
              AND f.dataVencimento < :fim
              AND t.contaNosGastos = true
            """)
    List<Transacao> comprasDasFaturas(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /**
     * Os gastos da conta corrente no mês, MENOS os pagamentos de contas
     * fixas: esses entram pela conta fixa (com o rateio dela), e não podem
     * ser somados duas vezes.
     */
    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta c
            LEFT JOIN FETCH t.pessoa
            WHERE c.tipo = dev.alisonsantos.appgastos.conta.TipoConta.CORRENTE
              AND t.contaNosGastos = true
              AND t.dataTransacao >= :inicio
              AND t.dataTransacao < :fim
              AND NOT EXISTS (SELECT m.id FROM ContaFixaMes m WHERE m.transacao = t)
            """)
    List<Transacao> gastosDaContaCorrente(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    /** As faturas que vencem no mês (para a conferência). */
    @Query("""
            SELECT f FROM Fatura f
            JOIN FETCH f.conta
            WHERE f.dataVencimento >= :inicio
              AND f.dataVencimento < :fim
            """)
    List<Fatura> faturasDoMes(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // ------------------------------------------------------------------
    // Para os recebimentos
    // ------------------------------------------------------------------

    /** Quanto cada pessoa deve em cada mês publicado. */
    @Query("""
            SELECT i.pessoa.id AS pessoaId, f.competencia AS competencia, SUM(i.valor) AS total
            FROM ItemFechamento i
            JOIN i.fechamento f
            WHERE i.pessoa IS NOT NULL
            GROUP BY i.pessoa.id, f.competencia
            """)
    List<DevidoNoMes> devidosPorPessoaEMes();

    interface DevidoNoMes {
        Long getPessoaId();

        LocalDate getCompetencia();

        BigDecimal getTotal();
    }
}
