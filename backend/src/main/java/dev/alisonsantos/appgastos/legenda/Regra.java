package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
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
import java.time.LocalDate;

/**
 * Uma regra da legenda.
 *
 * "Quando a compra tiver [tipoChave = valorChave] (e, se informado,
 * for no cartão [cartaoFinal]), ela é: [descricao], [categoria], de [pessoa]."
 *
 * Os resultados são opcionais (mas pelo menos um é obrigatório):
 * uma regra pode só dar nome, só mudar o dono (ou dividir a compra),
 * só definir a categoria, ou tudo junto.
 */
@Entity
@Table(name = "regra")
public class Regra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave", nullable = false, length = 30)
    private TipoChave tipoChave;

    /** Já normalizado (ver TipoChave.normalizar), para comparar rápido. */
    @Column(name = "valor_chave", nullable = false, length = 255)
    private String valorChave;

    /** Opcional: a regra só vale para o cartão com este final. */
    @Column(name = "cartao_final", length = 4)
    private String cartaoFinal;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "categoria", length = 60)
    private String categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    /**
     * Em vez de um dono, a regra pode DIVIDIR a compra com uma divisão
     * salva (ex.: CNPJ do Spotify → "Spotify Família", entre 5 pessoas).
     * Pessoa e divisão são alternativas: a regra tem uma ou outra.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "divisao_salva_id")
    private DivisaoSalva divisaoSalva;

    /** Desempate entre regras do mesmo nível: maior vence. */
    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Regra() {
    }

    /** O valor da chave é normalizado na hora, de acordo com o tipo. */
    public Regra(TipoChave tipoChave, String valorChave) {
        definirChave(tipoChave, valorChave);
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

    /** Troca o tipo e o valor juntos, para o valor sempre ficar normalizado. */
    public void definirChave(TipoChave tipoChave, String valorChave) {
        this.tipoChave = tipoChave;
        this.valorChave = tipoChave.normalizar(valorChave);
    }

    /** Tem data de início ou de fim: é uma regra temporária. */
    public boolean isTemporaria() {
        return vigenciaInicio != null || vigenciaFim != null;
    }

    /** A regra vale nesta data? Sem vigência, vale sempre. */
    public boolean isVigenteEm(LocalDate data) {
        if (data == null) {
            return !isTemporaria();
        }
        boolean depoisDoInicio = vigenciaInicio == null || !data.isBefore(vigenciaInicio);
        boolean antesDoFim = vigenciaFim == null || !data.isAfter(vigenciaFim);
        return depoisDoInicio && antesDoFim;
    }

    public void definirVigencia(LocalDate inicio, LocalDate fim) {
        this.vigenciaInicio = inicio;
        this.vigenciaFim = fim;
    }

    // ------------------------------------------------------------------
    // Getters e setters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public TipoChave getTipoChave() {
        return tipoChave;
    }

    public String getValorChave() {
        return valorChave;
    }

    public String getCartaoFinal() {
        return cartaoFinal;
    }

    public void setCartaoFinal(String cartaoFinal) {
        this.cartaoFinal = cartaoFinal;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public DivisaoSalva getDivisaoSalva() {
        return divisaoSalva;
    }

    public void setDivisaoSalva(DivisaoSalva divisaoSalva) {
        this.divisaoSalva = divisaoSalva;
    }

    /** A regra define o destino (um dono ou uma divisão)? */
    public boolean defineDestino() {
        return pessoa != null || divisaoSalva != null;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(int prioridade) {
        this.prioridade = prioridade;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
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
        Regra outra = (Regra) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
