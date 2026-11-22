package dev.alisonsantos.appgastos.importacao;

import dev.alisonsantos.appgastos.legenda.Regra;
import dev.alisonsantos.appgastos.legenda.TipoChave;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma regra sugerida pelo importador, esperando a sua aprovação.
 */
@Entity
@Table(name = "sugestao_regra")
public class SugestaoRegra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave", nullable = false, length = 30)
    private TipoChave tipoChave;

    @Column(name = "valor_chave", nullable = false, length = 255)
    private String valorChave;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "secao_planilha", length = 20)
    private SecaoPlanilha secaoPlanilha;

    @Column(name = "compras", nullable = false)
    private int compras;

    @Column(name = "confianca_descricao", nullable = false, precision = 5, scale = 2)
    private BigDecimal confiancaDescricao;

    @Column(name = "confianca_pessoa", precision = 5, scale = 2)
    private BigDecimal confiancaPessoa;

    @Column(name = "transacoes_afetadas", nullable = false)
    private int transacoesAfetadas;

    @Column(name = "ultima_compra")
    private LocalDate ultimaCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSugestao status = StatusSugestao.PENDENTE;

    /** A regra criada a partir desta sugestão, quando aprovada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regra_id")
    private Regra regra;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected SugestaoRegra() {
    }

    /** Cria a sugestão a partir do que o GeradorSugestoes calculou. */
    public SugestaoRegra(SugestaoCalculada calculada, Pessoa pessoaSugerida) {
        this.tipoChave = TipoChave.DESCRICAO_CONTEM;
        this.valorChave = calculada.valorChave();
        this.descricao = calculada.descricao();
        this.pessoa = pessoaSugerida;
        this.secaoPlanilha = calculada.secao();
        this.compras = calculada.compras();
        this.confiancaDescricao = BigDecimal.valueOf(calculada.confiancaDescricao());
        this.confiancaPessoa = pessoaSugerida == null ? null : BigDecimal.valueOf(calculada.confiancaSecao());
        this.transacoesAfetadas = calculada.transacoesAfetadas();
        this.ultimaCompra = calculada.ultimaCompra();
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

    public void aprovar(Regra regraCriada) {
        this.status = StatusSugestao.APROVADA;
        this.regra = regraCriada;
    }

    public void descartar() {
        this.status = StatusSugestao.DESCARTADA;
    }

    public boolean isPendente() {
        return status == StatusSugestao.PENDENTE;
    }

    public Long getId() {
        return id;
    }

    public TipoChave getTipoChave() {
        return tipoChave;
    }

    public String getValorChave() {
        return valorChave;
    }

    public String getDescricao() {
        return descricao;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public SecaoPlanilha getSecaoPlanilha() {
        return secaoPlanilha;
    }

    public int getCompras() {
        return compras;
    }

    public BigDecimal getConfiancaDescricao() {
        return confiancaDescricao;
    }

    public BigDecimal getConfiancaPessoa() {
        return confiancaPessoa;
    }

    public int getTransacoesAfetadas() {
        return transacoesAfetadas;
    }

    public LocalDate getUltimaCompra() {
        return ultimaCompra;
    }

    public StatusSugestao getStatus() {
        return status;
    }

    public Regra getRegra() {
        return regra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SugestaoRegra outra = (SugestaoRegra) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
