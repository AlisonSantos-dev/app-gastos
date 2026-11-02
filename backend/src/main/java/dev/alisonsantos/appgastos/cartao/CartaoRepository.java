package dev.alisonsantos.appgastos.cartao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    /**
     * Busca um cartão pela conta e pelos 4 últimos dígitos.
     * Usado na sincronização: se não existir, o cartão é criado como PENDENTE.
     *
     * "ContaId" navega pelo relacionamento: cartao.conta.id
     */
    Optional<Cartao> findByContaIdAndUltimosDigitos(Long contaId, String ultimosDigitos);

    /** Usado para listar os cartões PENDENTES que precisam de cadastro. */
    List<Cartao> findByStatus(StatusCartao status);
}
