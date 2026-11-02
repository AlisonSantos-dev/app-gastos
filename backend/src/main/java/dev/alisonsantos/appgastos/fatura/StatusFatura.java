package dev.alisonsantos.appgastos.fatura;

/**
 * ABERTA: ainda recebendo compras (não existe na Pluggy).
 * FECHADA: fechou e tem o total oficial do banco.
 * PAGA: o pagamento foi confirmado.
 */
public enum StatusFatura {
    ABERTA,
    FECHADA,
    PAGA
}
