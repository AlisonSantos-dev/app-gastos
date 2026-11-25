package dev.alisonsantos.appgastos.fechamento;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma linha de um mês publicado. Guarda uma CÓPIA dos dados (descrição,
 * valor...), e não só a referência: assim a foto não muda se a compra
 * original for renomeada, dividida de outro jeito ou removida.
 */
@Entity
@Table(name = "fechamento_item")
public class ItemFechamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fechamento_id", nullable = false)
    private Fechamento fechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemItem origem;

    @Column(name = "transacao_id")
    private Long transacaoId;

    @Column(name = "conta_fixa_mes_id")
    private Long contaFixaMesId;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @Column(name = "detalhe", length = 100)
    private String detalhe;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "dividida", nullable = false)
    private boolean dividida;

    protected ItemFechamento() {
    }

    ItemFechamento(Fechamento fechamento, LinhaFechamento linha) {
        this.fechamento = fechamento;
        this.pessoa = linha.pessoa();
        this.origem = linha.origem();
        this.transacaoId = linha.transacaoId();
        this.contaFixaMesId = linha.contaFixaMesId();
        this.data = linha.data();
        this.descricao = cortar(linha.descricao(), 255);
        this.detalhe = cortar(linha.detalhe(), 100);
        this.valor = linha.valor();
        this.valorTotal = linha.valorTotal();
        this.dividida = linha.dividida();
    }

    LinhaFechamento paraLinha() {
        return new LinhaFechamento(pessoa, origem, transacaoId, contaFixaMesId, data, descricao, detalhe,
                valor, valorTotal, dividida);
    }

    private static String cortar(String texto, int tamanho) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
