package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.pessoa.Pessoa;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Um modelo de divisão com nome, para usar de novo.
 * Ex.: "Spotify Família" = Alison, Gabriella, Lorena, Paulo e Guilherme, em partes iguais.
 */
@Entity
@Table(name = "divisao_salva")
public class DivisaoSalva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    /** IGUAL ou PERCENTUAL (valores fixos não servem para um modelo). */
    @Enumerated(EnumType.STRING)
    @Column(name = "modo", nullable = false, length = 20)
    private ModoDivisao modo;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @OneToMany(mappedBy = "divisao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<DivisaoSalvaParte> partes = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected DivisaoSalva() {
    }

    public DivisaoSalva(String nome, ModoDivisao modo) {
        this.nome = nome;
        this.modo = modo;
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

    /** Uma pessoa do modelo (o percentual só no modo PERCENTUAL). */
    public record Integrante(Pessoa pessoa, BigDecimal percentual) {
    }

    /** Troca as pessoas do modelo, na ordem informada. */
    public void definirPartes(List<Integrante> integrantes) {
        partes.clear();
        for (int i = 0; i < integrantes.size(); i++) {
            Integrante integrante = integrantes.get(i);
            BigDecimal percentual = modo == ModoDivisao.PERCENTUAL ? integrante.percentual() : null;
            partes.add(new DivisaoSalvaParte(this, integrante.pessoa(), percentual, i));
        }
    }

    /** Converte o modelo no formato que a CalculadoraDivisao entende. */
    public List<CalculadoraDivisao.Entrada> paraEntradas() {
        return partes.stream()
                .map(parte -> new CalculadoraDivisao.Entrada(parte.getPessoa(), null, parte.getPercentual()))
                .toList();
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

    public ModoDivisao getModo() {
        return modo;
    }

    public void setModo(ModoDivisao modo) {
        this.modo = modo;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public List<DivisaoSalvaParte> getPartes() {
        return Collections.unmodifiableList(partes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DivisaoSalva outra = (DivisaoSalva) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
