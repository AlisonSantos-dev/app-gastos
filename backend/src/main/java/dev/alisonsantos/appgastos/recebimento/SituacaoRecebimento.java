package dev.alisonsantos.appgastos.recebimento;

import java.math.BigDecimal;

/**
 * Como está o pagamento de uma pessoa num mês.
 */
public enum SituacaoRecebimento {

    /** O mês ainda não foi publicado: ainda não se sabe quanto ela deve. */
    MES_NAO_PUBLICADO,

    /** Não deve nada neste mês. */
    SEM_DEBITO,

    /** Ainda não pagou nada. */
    PENDENTE,

    /** Pagou uma parte. */
    PARCIAL,

    /** Pagou tudo (ou mais: o que sobrar fica como crédito no saldo). */
    QUITADO;

    /** Diferenças de até R$ 1,00 contam como quitado (arredondamentos). */
    static final BigDecimal TOLERANCIA = new BigDecimal("1.00");

    public static SituacaoRecebimento de(BigDecimal devido, BigDecimal recebido) {
        if (devido == null) {
            return MES_NAO_PUBLICADO;
        }
        if (devido.signum() <= 0) {
            return SEM_DEBITO;
        }
        if (recebido.signum() == 0) {
            return PENDENTE;
        }
        return devido.subtract(recebido).compareTo(TOLERANCIA) <= 0 ? QUITADO : PARCIAL;
    }
}
