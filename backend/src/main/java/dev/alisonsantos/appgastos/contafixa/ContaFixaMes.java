package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.transacao.Transacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A conta fixa de um mês: a "Sanepar de novembro".
 */
@Entity
@Table(name = "conta_fixa_mes")
public class ContaFixaMes {

    /** A situação que a tela mostra (inclui ATRASADA, que é calculada). */
    public enum Situacao {
        PENDENTE,
        ATRASADA,
        PAGA,
        DISPENSADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_fixa_id", nullable = false)
    private ContaFixa contaFixa;

    /** O mês (sempre o dia 1). */
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "vencimento", nullable = false)
    private LocalDate vencimento;

    @Column(name = "valor_previsto", precision = 14, scale = 2)
    private BigDecimal valorPrevisto;

    @Column(name = "valor_pago", precision = 14, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    /** O pagamento no extrato (Parte B: encontrado automaticamente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id")
    private Transacao transacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusContaMes status = StatusContaMes.PENDENTE;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ContaFixaMes() {
    }

    public ContaFixaMes(ContaFixa contaFixa, YearMonth competencia, BigDecimal valorPrevisto) {
        this.contaFixa = contaFixa;
        this.competencia = competencia.atDay(1);
        this.vencimento = contaFixa.vencimentoEm(competencia);
        this.valorPrevisto = valorPrevisto;
    }

    @PrePersist
    protected void aoCriar() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    // ------------------------------------------------------------------
    // Regras de negócio
    // ------------------------------------------------------------------

    /** Registra o pagamento (com ou sem a transação do extrato). */
    public void registrarPagamento(BigDecimal valor, LocalDate data, Transacao transacaoDoExtrato) {
        this.valorPago = valor;
        this.dataPagamento = data;
        this.transacao = transacaoDoExtrato;
        this.status = StatusContaMes.PAGA;
    }

    /** Este mês não tem essa conta (ex.: isenta, sem cobrança). */
    public void dispensar() {
        limparPagamento();
        this.status = StatusContaMes.DISPENSADA;
    }

    /** Volta a ficar pendente (desfaz o pagamento ou a dispensa). */
    public void reabrir() {
        limparPagamento();
        this.status = StatusContaMes.PENDENTE;
    }

    /** O valor que vale para as contas do mês: o pago, ou o previsto se ainda não pagou. */
    public BigDecimal valorEfetivo() {
        if (status == StatusContaMes.DISPENSADA) {
            return BigDecimal.ZERO;
        }
        return valorPago != null ? valorPago : valorPrevisto;
    }

    /** A situação para mostrar. ATRASADA = pendente com o vencimento já passado. */
    public Situacao situacao(LocalDate hoje) {
        return switch (status) {
            case PAGA -> Situacao.PAGA;
            case DISPENSADA -> Situacao.DISPENSADA;
            case PENDENTE -> vencimento.isBefore(hoje) ? Situacao.ATRASADA : Situacao.PENDENTE;
        };
    }

    private void limparPagamento() {
        this.valorPago = null;
        this.dataPagamento = null;
        this.transacao = null;
    }

    // ------------------------------------------------------------------
    // Getters e setters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public ContaFixa getContaFixa() {
        return contaFixa;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public void setValorPrevisto(BigDecimal valorPrevisto) {
        this.valorPrevisto = valorPrevisto;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public StatusContaMes getStatus() {
        return status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContaFixaMes outra = (ContaFixaMes) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
