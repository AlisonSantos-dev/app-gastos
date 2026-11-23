package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.pessoa.Pessoa;
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

/** Uma pessoa de uma divisão salva. Criada e trocada só pela DivisaoSalva. */
@Entity
@Table(name = "divisao_salva_parte")
public class DivisaoSalvaParte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "divisao_salva_id", nullable = false)
    private DivisaoSalva divisao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /** Só no modo PERCENTUAL. */
    @Column(name = "percentual", precision = 7, scale = 4)
    private BigDecimal percentual;

    /** A posição da pessoa (decide quem recebe os centavos que sobram). */
    @Column(name = "ordem", nullable = false)
    private int ordem;

    protected DivisaoSalvaParte() {
    }

    DivisaoSalvaParte(DivisaoSalva divisao, Pessoa pessoa, BigDecimal percentual, int ordem) {
        this.divisao = divisao;
        this.pessoa = pessoa;
        this.percentual = percentual;
        this.ordem = ordem;
    }

    public Long getId() {
        return id;
    }

    public DivisaoSalva getDivisao() {
        return divisao;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public int getOrdem() {
        return ordem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DivisaoSalvaParte outra = (DivisaoSalvaParte) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
