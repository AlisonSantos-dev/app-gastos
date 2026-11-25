package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.Texto;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Uma conta fixa: algo que se paga todo mês, fora do cartão.
 * Ex.: "Sanepar", grupo "Casa dos pais", vence dia 17, da Marlene.
 */
@Entity
@Table(name = "conta_fixa")
public class ContaFixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    /** Agrupa na tela: "Casa dos pais", "Nossa casa"... */
    @Column(name = "grupo", length = 60)
    private String grupo;

    /** A conta corrente de onde ela costuma ser paga. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private Conta conta;

    @Column(name = "dia_vencimento", nullable = false)
    private short diaVencimento;

    /** Valor fixo (ex.: financiamento). Null = variável (ex.: água, luz). */
    @Column(name = "valor_esperado", precision = 14, scale = 2)
    private BigDecimal valorEsperado;

    /** De quem é: uma pessoa... */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    /** ...ou uma divisão salva (ex.: "Pais 50/50"). Nunca os dois. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "divisao_salva_id")
    private DivisaoSalva divisaoSalva;

    /** Trecho da descrição do pagamento no extrato (ex.: "SANEPAR"), já normalizado. */
    @Column(name = "chave_pagamento", length = 255)
    private String chavePagamento;

    @Column(name = "categoria", length = 60)
    private String categoria;

    /** Primeiro mês (sempre o dia 1). */
    @Column(name = "inicio", nullable = false)
    private LocalDate inicio;

    /** Último mês, se a conta acabou (sempre o dia 1). */
    @Column(name = "fim")
    private LocalDate fim;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ContaFixa() {
    }

    public ContaFixa(String nome, int diaVencimento, YearMonth inicio) {
        this.nome = nome;
        this.diaVencimento = (short) diaVencimento;
        this.inicio = inicio.atDay(1);
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

    /** A conta existe neste mês? (ativa, e entre o início e o fim) */
    public boolean isVigenteEm(YearMonth competencia) {
        YearMonth primeiro = YearMonth.from(inicio);
        YearMonth ultimo = fim == null ? null : YearMonth.from(fim);
        return ativa
                && !competencia.isBefore(primeiro)
                && (ultimo == null || !competencia.isAfter(ultimo));
    }

    /**
     * O vencimento num mês. Se o dia não existir no mês (dia 31 em
     * fevereiro), vence no último dia do mês.
     */
    public LocalDate vencimentoEm(YearMonth competencia) {
        int dia = Math.min(diaVencimento, competencia.lengthOfMonth());
        return competencia.atDay(dia);
    }

    /** Define de quem é a conta: uma pessoa OU uma divisão. */
    public void definirDestino(Pessoa pessoa, DivisaoSalva divisaoSalva) {
        this.pessoa = pessoa;
        this.divisaoSalva = divisaoSalva;
    }

    public void definirVigencia(YearMonth inicio, YearMonth fim) {
        this.inicio = inicio.atDay(1);
        this.fim = fim == null ? null : fim.atDay(1);
    }

    /** Guarda a chave já normalizada (maiúsculas, sem acento), para comparar rápido. */
    public void definirChavePagamento(String chave) {
        String normalizada = Texto.normalizar(chave);
        this.chavePagamento = normalizada.isEmpty() ? null : normalizada;
    }

    // ------------------------------------------------------------------
    // Getters e setters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public Conta getConta() {
        return conta;
    }

    public void setConta(Conta conta) {
        this.conta = conta;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(int diaVencimento) {
        this.diaVencimento = (short) diaVencimento;
    }

    public BigDecimal getValorEsperado() {
        return valorEsperado;
    }

    public void setValorEsperado(BigDecimal valorEsperado) {
        this.valorEsperado = valorEsperado;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public DivisaoSalva getDivisaoSalva() {
        return divisaoSalva;
    }

    public String getChavePagamento() {
        return chavePagamento;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public YearMonth getInicio() {
        return YearMonth.from(inicio);
    }

    public YearMonth getFim() {
        return fim == null ? null : YearMonth.from(fim);
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContaFixa outra = (ContaFixa) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
