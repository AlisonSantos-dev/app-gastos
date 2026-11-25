package dev.alisonsantos.appgastos.fechamento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Um mês publicado: a "foto" congelada das linhas de cada pessoa.
 */
@Entity
@Table(name = "fechamento")
public class Fechamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "publicado_em", nullable = false)
    private Instant publicadoEm;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "fechamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemFechamento> itens = new ArrayList<>();

    protected Fechamento() {
    }

    /** Publica o mês com as linhas calculadas agora. */
    public Fechamento(YearMonth competencia, List<LinhaFechamento> linhas) {
        this.competencia = competencia.atDay(1);
        this.publicadoEm = Instant.now();
        this.total = linhas.stream().map(LinhaFechamento::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        linhas.forEach(linha -> itens.add(new ItemFechamento(this, linha)));
    }

    /** As linhas da foto, de volta no formato que a calculadora entende. */
    public List<LinhaFechamento> linhas() {
        return itens.stream().map(ItemFechamento::paraLinha).toList();
    }

    public Long getId() {
        return id;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<ItemFechamento> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
