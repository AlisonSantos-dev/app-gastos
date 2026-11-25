package dev.alisonsantos.appgastos.recebimento;

import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Um pagamento que uma pessoa fez ao titular, referente a um mês.
 * Ex.: o Pix de R$ 2.118,14 da Marlene, quitando novembro.
 */
@Entity
@Table(name = "recebimento")
public class Recebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /** O mês que este pagamento quita (sempre o dia 1). */
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    /** O Pix recebido no extrato (vazio = dinheiro ou outro meio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id")
    private Transacao transacao;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Recebimento() {
    }

    public Recebimento(Pessoa pessoa, YearMonth competencia, BigDecimal valor, LocalDate data,
                       Transacao transacao, String observacao) {
        this.pessoa = pessoa;
        this.competencia = competencia.atDay(1);
        this.valor = valor;
        this.data = data;
        this.transacao = transacao;
        this.observacao = observacao;
    }

    @PrePersist
    protected void aoCriar() {
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public String getObservacao() {
        return observacao;
    }
}
