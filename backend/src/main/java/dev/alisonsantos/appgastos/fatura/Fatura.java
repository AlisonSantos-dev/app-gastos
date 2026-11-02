package dev.alisonsantos.appgastos.fatura;

import dev.alisonsantos.appgastos.conta.Conta;
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

/**
 * Fatura de uma conta de cartão de crédito.
 *
 * Valores em dinheiro usam BigDecimal, nunca double: o double tem erros de
 * arredondamento (0.1 + 0.2 = 0.30000000000000004), e com dinheiro isso
 * vira centavo sumindo.
 */
@Entity
@Table(name = "fatura")
public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    /** Id da fatura na Pluggy. Vazio enquanto a fatura está aberta. */
    @Column(name = "pluggy_bill_id", length = 64, unique = true)
    private String pluggyBillId;

    /** LocalDate: só a data, sem hora (ex.: 2026-10-02). */
    @Column(name = "data_fechamento")
    private LocalDate dataFechamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    /** Total informado pelo banco. Usado para conferir o fechamento do mês. */
    @Column(name = "total_oficial", precision = 14, scale = 2)
    private BigDecimal totalOficial;

    @Column(name = "pagamento_minimo", precision = 14, scale = 2)
    private BigDecimal pagamentoMinimo;

    /** Juros, IOF e tarifas da fatura. */
    @Column(name = "encargos", nullable = false, precision = 14, scale = 2)
    private BigDecimal encargos = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusFatura status = StatusFatura.ABERTA;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Fatura() {
    }

    public Fatura(Conta conta, LocalDate dataVencimento) {
        this.conta = conta;
        this.dataVencimento = dataVencimento;
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

    public Long getId() {
        return id;
    }

    public Conta getConta() {
        return conta;
    }

    public String getPluggyBillId() {
        return pluggyBillId;
    }

    public void setPluggyBillId(String pluggyBillId) {
        this.pluggyBillId = pluggyBillId;
    }

    public LocalDate getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDate dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public BigDecimal getTotalOficial() {
        return totalOficial;
    }

    public void setTotalOficial(BigDecimal totalOficial) {
        this.totalOficial = totalOficial;
    }

    public BigDecimal getPagamentoMinimo() {
        return pagamentoMinimo;
    }

    public void setPagamentoMinimo(BigDecimal pagamentoMinimo) {
        this.pagamentoMinimo = pagamentoMinimo;
    }

    public BigDecimal getEncargos() {
        return encargos;
    }

    public void setEncargos(BigDecimal encargos) {
        this.encargos = encargos;
    }

    public StatusFatura getStatus() {
        return status;
    }

    public void setStatus(StatusFatura status) {
        this.status = status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fatura outra = (Fatura) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
