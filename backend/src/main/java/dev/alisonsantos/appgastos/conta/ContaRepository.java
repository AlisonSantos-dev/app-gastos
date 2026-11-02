package dev.alisonsantos.appgastos.conta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    /**
     * Busca a conta pelo id da Pluggy.
     * Usado na sincronização para saber se a conta já existe ou se é nova.
     *
     * Optional: deixa explícito que a conta pode não existir,
     * em vez de devolver null.
     */
    Optional<Conta> findByPluggyAccountId(String pluggyAccountId);
}
