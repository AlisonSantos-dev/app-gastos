package dev.alisonsantos.appgastos.fatura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    /** Usado na sincronização para saber se a fatura já existe. */
    Optional<Fatura> findByPluggyBillId(String pluggyBillId);

    /** Faturas de uma conta, da mais recente para a mais antiga. */
    List<Fatura> findByContaIdOrderByDataVencimentoDesc(Long contaId);
}
