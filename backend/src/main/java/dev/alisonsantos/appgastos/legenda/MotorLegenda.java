package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.comum.Texto;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * O motor da legenda: dada uma transação e as regras, decide qual regra
 * vale para cada atributo (descrição, categoria e pessoa).
 *
 * Não grava nada, não acessa banco: só decide. Por isso é fácil de testar.
 *
 * A CASCATA (da mais forte para a mais fraca):
 *   1. Regra temporária vigente vence regra permanente
 *   2. O tipo da chave: CNPJ > estabelecimento > descrição igual > descrição contém > MCC
 *   3. Com filtro de cartão vence sem filtro (é mais específica)
 *   4. Maior prioridade
 *   5. Valor da chave mais longo ("WELLHUB GABRIELLA" vence "WELLHUB")
 *   6. A regra mais antiga (menor id), para o resultado ser sempre o mesmo
 */
@Component
public class MotorLegenda {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    static final Comparator<Regra> ORDEM_DA_CASCATA = Comparator
            .comparingInt((Regra r) -> r.isTemporaria() ? 0 : 1)
            .thenComparingInt(r -> r.getTipoChave().getNivel())
            .thenComparingInt(r -> r.getCartaoFinal() == null ? 1 : 0)
            .thenComparing(Regra::getPrioridade, Comparator.reverseOrder())
            .thenComparing(r -> r.getValorChave().length(), Comparator.reverseOrder())
            .thenComparing(Regra::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public ResultadoLegenda avaliar(Transacao transacao, List<Regra> regras) {
        Caracteristicas compra = Caracteristicas.de(transacao);

        // As regras que batem com esta compra, já na ordem da cascata
        List<Regra> queBatem = regras.stream()
                .filter(Regra::isAtiva)
                .filter(regra -> regra.isVigenteEm(compra.data()))
                .filter(regra -> regra.getCartaoFinal() == null
                        || regra.getCartaoFinal().equals(compra.finalCartao()))
                .filter(regra -> bate(regra, compra))
                .sorted(ORDEM_DA_CASCATA)
                .toList();

        if (queBatem.isEmpty()) {
            return ResultadoLegenda.vazio();
        }

        // Para cada atributo, a primeira regra (a mais forte) que o define.
        // Regras de MCC só definem categoria.
        Regra paraDescricao = queBatem.stream()
                .filter(r -> r.getDescricao() != null && r.getTipoChave() != TipoChave.MCC)
                .findFirst().orElse(null);
        Regra paraCategoria = queBatem.stream()
                .filter(r -> r.getCategoria() != null)
                .findFirst().orElse(null);
        Regra paraPessoa = queBatem.stream()
                .filter(r -> r.getPessoa() != null && r.getTipoChave() != TipoChave.MCC)
                .findFirst().orElse(null);

        return new ResultadoLegenda(paraDescricao, paraCategoria, paraPessoa);
    }

    private boolean bate(Regra regra, Caracteristicas compra) {
        String valor = regra.getValorChave();
        if (valor == null || valor.isEmpty()) {
            return false;
        }
        return switch (regra.getTipoChave()) {
            case CNPJ -> valor.equals(compra.cnpj());
            case MERCHANT -> valor.equals(compra.estabelecimento());
            case DESCRICAO_IGUAL -> valor.equals(compra.descricao());
            case DESCRICAO_CONTEM -> compra.descricao().contains(valor);
            case MCC -> valor.equals(compra.mcc());
        };
    }

    /**
     * As características da compra, já normalizadas uma vez só,
     * para não normalizar de novo a cada regra testada.
     */
    private record Caracteristicas(String descricao, String estabelecimento, String cnpj,
                                   String mcc, String finalCartao, LocalDate data) {

        static Caracteristicas de(Transacao t) {
            // A vigência considera a data da COMPRA (nas parcelas, a da 1ª)
            Instant quando = t.getDataCompra() != null ? t.getDataCompra() : t.getDataTransacao();
            return new Caracteristicas(
                    Texto.normalizar(t.getDescricaoBanco()),
                    Texto.normalizar(t.getMerchantNome()),
                    t.getCnpj(),
                    t.getMcc() == null ? null : String.valueOf(t.getMcc()),
                    t.getCartao() == null ? null : t.getCartao().getUltimosDigitos(),
                    quando == null ? null : LocalDate.ofInstant(quando, FUSO_BRASIL));
        }
    }
}
