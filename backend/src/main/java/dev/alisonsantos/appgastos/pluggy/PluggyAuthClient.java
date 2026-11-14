package dev.alisonsantos.appgastos.pluggy;

import dev.alisonsantos.appgastos.pluggy.dto.AuthRequest;
import dev.alisonsantos.appgastos.pluggy.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

/**
 * Autenticação na Pluggy.
 *
 * Faz o mesmo POST /auth que você faz no Postman e guarda a API Key em
 * memória até perto de expirar. Assim, as outras chamadas reaproveitam a
 * mesma chave, em vez de pedir uma nova a cada requisição.
 *
 * Os métodos são "synchronized": se duas partes do app pedirem a chave ao
 * mesmo tempo, só uma faz a chamada à Pluggy, e a outra espera e reaproveita.
 */
@Component
public class PluggyAuthClient {

    private static final Logger log = LoggerFactory.getLogger(PluggyAuthClient.class);

    private final RestClient restClient;
    private final PluggyProperties properties;

    /** A chave em cache e até quando ela vale. Nunca vão para o log. */
    private String apiKey;
    private Instant validaAte;

    public PluggyAuthClient(RestClient pluggyRestClient, PluggyProperties properties) {
        this.restClient = pluggyRestClient;
        this.properties = properties;
    }

    /**
     * Devolve uma API Key válida: a do cache, se ainda valer,
     * ou uma nova, gerada na hora.
     */
    public synchronized String obterApiKey() {
        if (apiKey != null && Instant.now().isBefore(validaAte)) {
            return apiKey;
        }
        gerarNovaApiKey();
        return apiKey;
    }

    /**
     * Descarta a chave em cache. Usado quando a Pluggy recusar a chave
     * (por exemplo, se ela expirou antes do previsto): a próxima chamada
     * a obterApiKey() gera uma nova.
     */
    public synchronized void invalidar() {
        this.apiKey = null;
        this.validaAte = null;
    }

    /** Até quando a chave atual vale. Nulo se ainda não houver chave. */
    public synchronized Instant getValidaAte() {
        return validaAte;
    }

    private void gerarNovaApiKey() {
        try {
            AuthResponse resposta = restClient.post()
                    .uri("/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AuthRequest(properties.clientId(), properties.clientSecret()))
                    .retrieve()
                    .body(AuthResponse.class);

            if (resposta == null || resposta.apiKey() == null || resposta.apiKey().isBlank()) {
                throw new PluggyException("A Pluggy respondeu, mas não devolveu a API Key.");
            }

            this.apiKey = resposta.apiKey();
            this.validaAte = Instant.now().plus(properties.apiKeyDuracao());
            log.info("Nova API Key da Pluggy gerada. Válida até {}", validaAte);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new PluggyException(
                        "A Pluggy recusou as credenciais (HTTP " + status + "). "
                                + "Confira PLUGGY_CLIENT_ID e PLUGGY_CLIENT_SECRET no .env.", e);
            }
            throw new PluggyException("Erro ao autenticar na Pluggy (HTTP " + status + ").", e);

        } catch (ResourceAccessException e) {
            throw new PluggyException(
                    "Não foi possível conectar na Pluggy. Verifique a conexão com a internet.", e);
        }
    }
}
