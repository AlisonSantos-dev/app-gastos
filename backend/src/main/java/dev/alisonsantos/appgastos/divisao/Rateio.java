package dev.alisonsantos.appgastos.divisao;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A parte de uma pessoa numa compra dividida.
 *
 * Quem cria e apaga os rateios é a própria Transacao (aplicarDivisao /
 * desfazerDivisao): por isso este construtor não é público para o resto
 * do app usar à vontade.
 */
@Entity
@Table(name = "rateio")
public class Rateio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transacao_id", nullable = false)
    private Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    /** De 0 a 100. */
    @Column(name = "percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal percentual;

    protected Rateio() {
    }

    /**
     * @param fracao a proporção de 0 a 1 (ex.: 0.2 para 20%)
     */
    public Rateio(Transacao transacao, Pessoa pessoa, BigDecimal valor, BigDecimal fracao) {
        this.transacao = transacao;
        this.pessoa = pessoa;
        this.valor = valor;
        this.percentual = fracao.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    /** A proporção de 0 a 1. */
    public BigDecimal getFracao() {
        return percentual.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rateio outro = (Rateio) o;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
