package dev.alisonsantos.appgastos.transacao;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.fatura.Fatura;
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

/**
 * Um lançamento vindo da Pluggy: compra no cartão, estorno, pagamento de
 * fatura, tarifa, Pix, boleto, débito...
 *
 * É a entidade central do app. Ela guarda o que veio do banco (que nunca
 * muda) e o que o app define em cima disso: a descrição amigável, o destino
 * (pessoa) e a classificação.
 */
@Entity
@Table(name = "transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id da transação na Pluggy. Único: garante que o sync não duplica. */
    @Column(name = "pluggy_transaction_id", nullable = false, length = 64, unique = true)
    private String pluggyTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    /** Cartão usado. Vazio em lançamentos de conta corrente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    private Cartao cartao;

    /** Fatura a que a compra pertence. Vazio fora do cartão. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fatura_id")
    private Fatura fatura;

    // ------------------------------------------------------------------
    // Quando aconteceu e quanto
    // ------------------------------------------------------------------

    /** Data em que o lançamento caiu (a parcela do mês, por exemplo). */
    @Column(name = "data_transacao", nullable = false)
    private Instant dataTransacao;

    /** Data da compra original. Nas parcelas, é a data da 1ª parcela. */
    @Column(name = "data_compra")
    private Instant dataCompra;

    /** Valor como veio da Pluggy. No cartão: compra positiva, estorno negativo. */
    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 10)
    private TipoMovimento tipoMovimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusTransacao status;

    // ------------------------------------------------------------------
    // Parcelamento
    // Short porque a coluna é SMALLINT no banco: o Hibernate confere
    // se os tipos batem, e Integer seria recusado.
    // ------------------------------------------------------------------

    @Column(name = "parcela_numero")
    private Short parcelaNumero;

    @Column(name = "parcela_total")
    private Short parcelaTotal;

    // ------------------------------------------------------------------
    // Descrição
    // ------------------------------------------------------------------

    /** Descrição original do banco. Nunca é alterada (sem setter). */
    @Column(name = "descricao_banco", nullable = false, length = 255)
    private String descricaoBanco;

    /** Descrição amigável, definida por regra ou manualmente. */
    @Column(name = "descricao", length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_descricao", nullable = false, length = 20)
    private OrigemDescricao origemDescricao = OrigemDescricao.BANCO;

    // ------------------------------------------------------------------
    // Destino
    // ------------------------------------------------------------------

    /** A pessoa a quem a transação é destinada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_pessoa", length = 20)
    private OrigemPessoa origemPessoa;

    // ------------------------------------------------------------------
    // Dados do estabelecimento e da operação (vindos da Pluggy)
    // ------------------------------------------------------------------

    @Column(name = "categoria_pluggy", length = 100)
    private String categoriaPluggy;

    @Column(name = "categoria_pluggy_id", length = 20)
    private String categoriaPluggyId;

    /** operationType da Pluggy: PAGAMENTO, ESTORNO, PIX, BOLETO, TARIFA... */
    @Column(name = "operacao_pluggy", length = 40)
    private String operacaoPluggy;

    /** CNPJ do estabelecimento, só números (14 dígitos). */
    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "merchant_nome", length = 150)
    private String merchantNome;

    /** Código do tipo de estabelecimento (ex.: 5411 = supermercado). */
    @Column(name = "mcc")
    private Integer mcc;

    /** Nome de quem enviou/recebeu um Pix. Nunca o documento ou a conta. */
    @Column(name = "contraparte_nome", length = 150)
    private String contraparteNome;

    // ------------------------------------------------------------------
    // Classificação
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lancamento", nullable = false, length = 30)
    private TipoLancamento tipoLancamento = TipoLancamento.NAO_CLASSIFICADO;

    /** Falso para o que não é gasto (pagamento de fatura, transferência própria). */
    @Column(name = "conta_nos_gastos", nullable = false)
    private boolean contaNosGastos = true;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Transacao() {
    }

    /** Construtor com tudo que é obrigatório vir da Pluggy. */
    public Transacao(String pluggyTransactionId,
                     Conta conta,
                     Instant dataTransacao,
                     BigDecimal valor,
                     String descricaoBanco,
                     TipoMovimento tipoMovimento,
                     StatusTransacao status) {
        this.pluggyTransactionId = pluggyTransactionId;
        this.conta = conta;
        this.dataTransacao = dataTransacao;
        this.valor = valor;
        this.descricaoBanco = descricaoBanco;
        this.tipoMovimento = tipoMovimento;
        this.status = status;
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
    // Regras de negócio simples (calculadas, não ficam no banco)
    // ------------------------------------------------------------------

    /** Compra parcelada: tem mais de uma parcela. */
    public boolean isParcelada() {
        return parcelaTotal != null && parcelaTotal > 1;
    }

    /**
     * Valor total da compra parcelada (parcela x quantidade).
     * A Pluggy não manda esse total, então ele é calculado.
     * Pode ter diferença de centavos por causa do arredondamento das parcelas.
     */
    public BigDecimal getValorTotalCompra() {
        if (!isParcelada()) {
            return valor;
        }
        return valor.multiply(BigDecimal.valueOf(parcelaTotal));
    }

    /** O que mostrar na tela: a descrição amigável ou, se não houver, a do banco. */
    public String getDescricaoExibicao() {
        return descricao != null ? descricao : descricaoBanco;
    }

    // ------------------------------------------------------------------
    // Getters e setters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getPluggyTransactionId() {
        return pluggyTransactionId;
    }

    public Conta getConta() {
        return conta;
    }

    public Cartao getCartao() {
        return cartao;
    }

    public void setCartao(Cartao cartao) {
        this.cartao = cartao;
    }

    public Fatura getFatura() {
        return fatura;
    }

    public void setFatura(Fatura fatura) {
        this.fatura = fatura;
    }

    public Instant getDataTransacao() {
        return dataTransacao;
    }

    public void setDataTransacao(Instant dataTransacao) {
        this.dataTransacao = dataTransacao;
    }

    public Instant getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(Instant dataCompra) {
        this.dataCompra = dataCompra;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public void setStatus(StatusTransacao status) {
        this.status = status;
    }

    public Short getParcelaNumero() {
        return parcelaNumero;
    }

    public Short getParcelaTotal() {
        return parcelaTotal;
    }

    /** Define as duas juntas, porque o banco exige que sejam coerentes. */
    public void setParcela(Short parcelaNumero, Short parcelaTotal) {
        this.parcelaNumero = parcelaNumero;
        this.parcelaTotal = parcelaTotal;
    }

    public String getDescricaoBanco() {
        return descricaoBanco;
    }

    public String getDescricao() {
        return descricao;
    }

    public OrigemDescricao getOrigemDescricao() {
        return origemDescricao;
    }

    /** Define a descrição amigável junto com a origem dela. */
    public void definirDescricao(String descricao, OrigemDescricao origem) {
        this.descricao = descricao;
        this.origemDescricao = origem;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public OrigemPessoa getOrigemPessoa() {
        return origemPessoa;
    }

    /** Define o destino junto com a origem dele. */
    public void definirPessoa(Pessoa pessoa, OrigemPessoa origem) {
        this.pessoa = pessoa;
        this.origemPessoa = origem;
    }

    public String getCategoriaPluggy() {
        return categoriaPluggy;
    }

    public void setCategoriaPluggy(String categoriaPluggy) {
        this.categoriaPluggy = categoriaPluggy;
    }

    public String getCategoriaPluggyId() {
        return categoriaPluggyId;
    }

    public void setCategoriaPluggyId(String categoriaPluggyId) {
        this.categoriaPluggyId = categoriaPluggyId;
    }

    public String getOperacaoPluggy() {
        return operacaoPluggy;
    }

    public void setOperacaoPluggy(String operacaoPluggy) {
        this.operacaoPluggy = operacaoPluggy;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getMerchantNome() {
        return merchantNome;
    }

    public void setMerchantNome(String merchantNome) {
        this.merchantNome = merchantNome;
    }

    public Integer getMcc() {
        return mcc;
    }

    public void setMcc(Integer mcc) {
        this.mcc = mcc;
    }

    public String getContraparteNome() {
        return contraparteNome;
    }

    public void setContraparteNome(String contraparteNome) {
        this.contraparteNome = contraparteNome;
    }

    public TipoLancamento getTipoLancamento() {
        return tipoLancamento;
    }

    public void setTipoLancamento(TipoLancamento tipoLancamento) {
        this.tipoLancamento = tipoLancamento;
    }

    public boolean isContaNosGastos() {
        return contaNosGastos;
    }

    public void setContaNosGastos(boolean contaNosGastos) {
        this.contaNosGastos = contaNosGastos;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
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
        Transacao outra = (Transacao) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
