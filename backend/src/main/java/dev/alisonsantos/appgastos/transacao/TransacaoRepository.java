package dev.alisonsantos.appgastos.transacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    /**
     * Usado na sincronização: se a transação já existe, é atualizada
     * (ex.: passou de PENDENTE para CONFIRMADA); se não, é criada.
     */
    Optional<Transacao> findByPluggyTransactionId(String pluggyTransactionId);

    /** Transações de uma fatura, em ordem cronológica. */
    List<Transacao> findByFaturaIdOrderByDataTransacaoAsc(Long faturaId);

    /** Transações de uma conta num período, em ordem cronológica. */
    List<Transacao> findByContaIdAndDataTransacaoBetweenOrderByDataTransacaoAsc(
            Long contaId, Instant inicio, Instant fim);
}
