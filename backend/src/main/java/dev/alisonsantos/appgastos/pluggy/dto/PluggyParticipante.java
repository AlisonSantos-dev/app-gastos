package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Quem enviou ou recebeu um pagamento.
 *
 * PRIVACIDADE: a Pluggy também manda o CPF/CNPJ e os dados bancários
 * (agência, conta) dessa pessoa. Eles NÃO são mapeados aqui de propósito:
 * como o record só tem o nome, o Jackson descarta o resto na leitura do
 * JSON, e esses dados de terceiros nunca entram no app.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyParticipante(String name) {
}
