package dev.alisonsantos.appgastos.conta;

import dev.alisonsantos.appgastos.pessoa.Pessoa;
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

import java.time.Instant;

/**
 * Uma conta bancária ou de cartão de crédito vinda da Pluggy.
 *
 * Os ids da Pluggy ficam guardados para ligar os dados do banco ao que vem
 * da API, sem duplicar a conta a cada sincronização.
 */
@Entity
@Table(name = "conta")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id da conexão com o banco no Meu Pluggy. */
    @Column(name = "pluggy_item_id", length = 64)
    private String pluggyItemId;

    /** Id da conta na Pluggy. Único: não pode haver duas contas com o mesmo. */
    @Column(name = "pluggy_account_id", length = 64, unique = true)
    private String pluggyAccountId;

    @Column(name = "banco", nullable = false, length = 60)
    private String banco;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoConta tipo;

    /**
     * Quem recebe por padrão os lançamentos sem cartão, como os de débito.
     *
     * LAZY: a pessoa só é buscada no banco quando alguém usa este campo.
     * Evita consultas desnecessárias.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_padrao_id")
    private Pessoa donoPadrao;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Conta() {
    }

    public Conta(String banco, String nome, TipoConta tipo) {
        this.banco = banco;
        this.nome = nome;
        this.tipo = tipo;
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

    public String getPluggyItemId() {
        return pluggyItemId;
    }

    public void setPluggyItemId(String pluggyItemId) {
        this.pluggyItemId = pluggyItemId;
    }

    public String getPluggyAccountId() {
        return pluggyAccountId;
    }

    public void setPluggyAccountId(String pluggyAccountId) {
        this.pluggyAccountId = pluggyAccountId;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public Pessoa getDonoPadrao() {
        return donoPadrao;
    }

    public void setDonoPadrao(Pessoa donoPadrao) {
        this.donoPadrao = donoPadrao;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
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
        Conta outra = (Conta) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
