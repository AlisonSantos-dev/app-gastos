package dev.alisonsantos.appgastos.transacao;

/**
 * De onde veio a classificação da transação.
 *
 * AUTOMATICA: definida pelas regras. É refeita a cada sincronização,
 *             então melhora sozinha quando as regras melhoram.
 * MANUAL: corrigida à mão. Nunca é sobrescrita automaticamente.
 */
public enum OrigemClassificacao {
    AUTOMATICA,
    MANUAL
}
