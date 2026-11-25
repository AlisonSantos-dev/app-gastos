package dev.alisonsantos.appgastos.fechamento;

/**
 * Um aviso sobre o mês, antes de publicar.
 *
 * @param bloqueante se impede a publicação (a não ser que você force)
 */
public record Alerta(Tipo tipo, String mensagem, boolean bloqueante) {

    public enum Tipo {
        /** Gastos sem dono (resolver na caixa de entrada). */
        SEM_DONO,
        /** Conta fixa sem valor nenhum (nem previsto, nem pago). */
        CONTA_SEM_VALOR,
        /** Conta fixa ainda não paga (entra com o valor previsto). */
        CONTA_PENDENTE,
        /** A soma das compras de uma fatura não bate com o total do banco. */
        FATURA_NAO_BATE,
        /** O cartão ainda não tem a fatura que vence no mês (ela não fechou). */
        FATURA_AUSENTE
    }
}
