package dev.alisonsantos.appgastos.cartao;

import dev.alisonsantos.appgastos.conta.Conta;
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
 * Um cartão (físico, virtual ou adicional) de uma conta de cartão de crédito.
 *
 * Guarda apenas os 4 últimos dígitos. O banco tem uma regra (CHECK) que
 * recusa qualquer valor que não seja exatamente 4 números.
 */
@Entity
@Table(name = "cartao")
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A conta de cartão a que este cartão pertence. Obrigatória. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Column(name = "ultimos_digitos", nullable = false, length = 4)
    private String ultimosDigitos;

    /** Nome amigável, ex.: "Virtual Gabi". */
    @Column(name = "apelido", length = 60)
    private String apelido;

    /** Pode ficar nulo até o cartão ser cadastrado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoCartao tipo;

    @Column(name = "cor", length = 7)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_dono", nullable = false, length = 20)
    private ModoDono modoDono = ModoDono.FIXO;

    /** Quem recebe as compras deste cartão quando o modo é FIXO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_padrao_id")
    private Pessoa donoPadrao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusCartao status = StatusCartao.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Cartao() {
    }

    /**
     * Cria um cartão novo, como acontece quando a sincronização encontra
     * um final que ainda não existe. Ele nasce PENDENTE, no modo FIXO,
     * sem dono definido.
     */
    public Cartao(Conta conta, String ultimosDigitos) {
        this.conta = conta;
        this.ultimosDigitos = ultimosDigitos;
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

    public String getUltimosDigitos() {
        return ultimosDigitos;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public TipoCartao getTipo() {
        return tipo;
    }

    public void setTipo(TipoCartao tipo) {
        this.tipo = tipo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public ModoDono getModoDono() {
        return modoDono;
    }

    public void setModoDono(ModoDono modoDono) {
        this.modoDono = modoDono;
    }

    public Pessoa getDonoPadrao() {
        return donoPadrao;
    }

    public void setDonoPadrao(Pessoa donoPadrao) {
        this.donoPadrao = donoPadrao;
    }

    public StatusCartao getStatus() {
        return status;
    }

    public void setStatus(StatusCartao status) {
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
        Cartao outro = (Cartao) o;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
