package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaFixaMesRepository extends JpaRepository<ContaFixaMes, Long> {

    /** As contas de um mês, já com a conta fixa e o destino (pessoa ou divisão). */
    @EntityGraph(attributePaths = {"contaFixa", "contaFixa.conta", "contaFixa.pessoa", "contaFixa.divisaoSalva",
            "contaFixa.divisaoSalva.partes", "contaFixa.divisaoSalva.partes.pessoa"})
    List<ContaFixaMes> findByCompetencia(LocalDate competencia);

    /** O último valor pago de uma conta (para prever o próximo mês das contas variáveis). */
    Optional<ContaFixaMes> findFirstByContaFixaIdAndValorPagoIsNotNullOrderByCompetenciaDesc(Long contaFixaId);

    /** Esta transação já paga alguma conta? */
    boolean existsByTransacaoId(Long transacaoId);

    /**
     * Os possíveis pagamentos de contas fixas num período: saídas das
     * contas correntes que contam nos gastos e que ainda não pagam
     * nenhuma conta.
     *
     * NOT EXISTS: "que não apareça em nenhuma conta do mês". Garante que
     * um pagamento já usado não seja oferecido de novo.
     */
    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta c
            WHERE c.tipo = dev.alisonsantos.appgastos.conta.TipoConta.CORRENTE
              AND t.tipoMovimento = dev.alisonsantos.appgastos.transacao.TipoMovimento.DEBITO
              AND t.contaNosGastos = true
              AND t.dataTransacao >= :inicio
              AND t.dataTransacao < :fim
              AND NOT EXISTS (SELECT m.id FROM ContaFixaMes m WHERE m.transacao = t)
            """)
    List<Transacao> buscarPagamentosCandidatos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
