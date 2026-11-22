package dev.alisonsantos.appgastos.transacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JpaSpecificationExecutor: permite montar consultas com filtros opcionais
 * (conta, cartão, período, tipo...), combinando só os que vierem preenchidos.
 */
public interface TransacaoRepository extends JpaRepository<Transacao, Long>,
        JpaSpecificationExecutor<Transacao> {

    /**
     * Usado na sincronização: se a transação já existe, é atualizada
     * (ex.: passou de PENDENTE para CONFIRMADA); se não, é criada.
     */
    Optional<Transacao> findByPluggyTransactionId(String pluggyTransactionId);

    /**
     * Todas as transações de uma conta. Usado na sincronização para
     * carregar tudo de uma vez, em vez de uma consulta por transação.
     */
    List<Transacao> findByContaId(Long contaId);

    /** Todas as transações de um cartão. Usado para reaplicar o dono quando ele muda. */
    List<Transacao> findByCartaoId(Long cartaoId);

    /** As transações de uma conta que não têm cartão (ex.: débitos da conta corrente). */
    List<Transacao> findByContaIdAndCartaoIsNull(Long contaId);

    /**
     * Todas as parcelas de uma mesma compra.
     *
     * A Pluggy não manda um "id da compra", então as parcelas são
     * reconhecidas pelo que elas têm em comum: o mesmo cartão, a mesma
     * data da compra, o mesmo número de parcelas e a mesma descrição.
     */
    @Query("""
            SELECT t FROM Transacao t
            WHERE t.cartao.id = :cartaoId
              AND t.dataCompra = :dataCompra
              AND t.parcelaTotal = :parcelaTotal
              AND t.descricaoBanco = :descricaoBanco
            ORDER BY t.parcelaNumero
            """)
    List<Transacao> findParcelasDaMesmaCompra(@Param("cartaoId") Long cartaoId,
                                              @Param("dataCompra") Instant dataCompra,
                                              @Param("parcelaTotal") Short parcelaTotal,
                                              @Param("descricaoBanco") String descricaoBanco);

    /**
     * Marca como revisados, de uma vez, todos os gastos anteriores a uma data.
     * É a "limpeza do histórico": o que você já conferiu na planilha
     * não precisa passar pela caixa de entrada.
     *
     * @Modifying: é um UPDATE, e não um SELECT. Ele roda direto no banco,
     * sem carregar as transações na memória (bem mais rápido para milhares).
     */
    @Modifying
    @Query("""
            UPDATE Transacao t
            SET t.revisada = true, t.revisadaEm = :agora
            WHERE t.revisada = false
              AND t.contaNosGastos = true
              AND t.dataTransacao < :limite
            """)
    int marcarRevisadasAntesDe(@Param("limite") Instant limite, @Param("agora") Instant agora);

    /** Transações de uma fatura, em ordem cronológica. */
    List<Transacao> findByFaturaIdOrderByDataTransacaoAsc(Long faturaId);

    /** Transações de uma conta num período, em ordem cronológica. */
    List<Transacao> findByContaIdAndDataTransacaoBetweenOrderByDataTransacaoAsc(
            Long contaId, Instant inicio, Instant fim);

    /**
     * A consulta com filtros da tela de transações.
     *
     * @EntityGraph: já traz a conta, o cartão e a fatura na MESMA consulta.
     * Sem isso, para mostrar 50 transações o Hibernate faria até 150
     * consultas extras (o famoso problema "N+1").
     */
    @Override
    @EntityGraph(attributePaths = {"conta", "cartao", "fatura"})
    Page<Transacao> findAll(Specification<Transacao> especificacao, Pageable paginacao);
}
