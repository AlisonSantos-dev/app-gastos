package dev.alisonsantos.appgastos.transacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
