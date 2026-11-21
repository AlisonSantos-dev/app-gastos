package dev.alisonsantos.appgastos.fatura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    /** Usado na sincronização para saber se a fatura já existe. */
    Optional<Fatura> findByPluggyBillId(String pluggyBillId);

    /** Faturas de uma conta, da mais recente para a mais antiga. */
    List<Fatura> findByContaIdOrderByDataVencimentoDesc(Long contaId);

    /**
     * Soma dos gastos de cada fatura de uma conta, numa consulta só.
     * É a base da conciliação: comparar essa soma com o total oficial.
     *
     * Escrita em JPQL (com os nomes das classes e atributos Java, e não
     * das tabelas), porque é uma agregação que o nome do método não expressa.
     */
    @Query("""
            SELECT t.fatura.id AS faturaId,
                   COUNT(t) AS quantidade,
                   SUM(t.valor) AS soma
            FROM Transacao t
            WHERE t.fatura.conta.id = :contaId
              AND t.contaNosGastos = true
            GROUP BY t.fatura.id
            """)
    List<SomaFatura> somarGastosPorFatura(@Param("contaId") Long contaId);

    /** Projeção: o Spring preenche esta interface com o resultado da consulta acima. */
    interface SomaFatura {
        Long getFaturaId();

        Long getQuantidade();

        BigDecimal getSoma();
    }
}
