package dev.alisonsantos.appgastos.recebimento;

import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {

    @EntityGraph(attributePaths = {"pessoa", "transacao"})
    List<Recebimento> findByCompetenciaOrderByDataAsc(LocalDate competencia);

    boolean existsByTransacaoId(Long transacaoId);

    /** Quanto cada pessoa já pagou de cada mês. */
    @Query("""
            SELECT r.pessoa.id AS pessoaId, r.competencia AS competencia, SUM(r.valor) AS total
            FROM Recebimento r
            GROUP BY r.pessoa.id, r.competencia
            """)
    List<RecebidoNoMes> recebidosPorPessoaEMes();

    /**
     * Os Pix (e outras entradas) recebidos desde uma data que ainda não
     * viraram recebimento: os candidatos às sugestões.
     */
    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta
            WHERE t.tipoLancamento = dev.alisonsantos.appgastos.transacao.TipoLancamento.RECEBIMENTO
              AND t.dataTransacao >= :desde
              AND t.contraparteNome IS NOT NULL
              AND NOT EXISTS (SELECT r.id FROM Recebimento r WHERE r.transacao = t)
            ORDER BY t.dataTransacao DESC
            """)
    List<Transacao> entradasSemRecebimento(@Param("desde") Instant desde);

    interface RecebidoNoMes {
        Long getPessoaId();

        LocalDate getCompetencia();

        BigDecimal getTotal();
    }
}
