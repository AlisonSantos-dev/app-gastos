package dev.alisonsantos.appgastos.pessoa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Quem recebe o destino das compras.
 *
 * Mapeia a tabela "pessoa", criada pela migration V1.
 * O Hibernate não cria nem altera a tabela: ele só confere se esta classe
 * bate com ela (ddl-auto: validate).
 */
@Entity
@Table(name = "pessoa")
public class Pessoa {

    /** Gerado pelo banco (IDENTITY), por isso não tem setter. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    /** Guardado como texto ('PESSOA' ou 'GRUPO'), e não como número. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPessoa tipo;

    /** Cor de identificação no app, em hexadecimal (ex.: #60A5FA). */
    @Column(name = "cor", length = 7)
    private String cor;

    /** Como o nome aparece nos Pix recebidos. Usado para sugerir recebimentos. */
    @Column(name = "nome_no_extrato", length = 150)
    private String nomeNoExtrato;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** Construtor vazio exigido pelo JPA. Protegido para não ser usado no código. */
    protected Pessoa() {
    }

    /** Construtor com os campos obrigatórios. */
    public Pessoa(String nome, TipoPessoa tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    /** Executado automaticamente antes de inserir no banco. */
    @PrePersist
    protected void aoCriar() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /** Executado automaticamente antes de atualizar no banco. */
    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoPessoa getTipo() {
        return tipo;
    }

    public void setTipo(TipoPessoa tipo) {
        this.tipo = tipo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getNomeNoExtrato() {
        return nomeNoExtrato;
    }

    public void setNomeNoExtrato(String nomeNoExtrato) {
        this.nomeNoExtrato = nomeNoExtrato;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /**
     * Duas pessoas são iguais se tiverem o mesmo id.
     * Antes de salvar (id nulo), cada objeto só é igual a ele mesmo.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pessoa outra = (Pessoa) o;
        return id != null && id.equals(outra.id);
    }

    /** Constante por classe, para não mudar quando o id é gerado ao salvar. */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
