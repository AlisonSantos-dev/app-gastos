package dev.alisonsantos.appgastos.pluggy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Configurações da Pluggy, lidas do application.yaml (bloco "pluggy").
 *
 * Os valores reais do client-id, do client-secret e dos item-ids vêm do
 * .env, nunca do código.
 *
 * @Validated: se alguma credencial estiver vazia, o app nem sobe e
 * mostra qual campo faltou. É melhor falhar na hora de iniciar do que
 * descobrir o problema só na primeira sincronização.
 */
@Validated
@ConfigurationProperties(prefix = "pluggy")
public record PluggyProperties(

        @NotBlank(message = "pluggy.base-url não foi configurado")
        String baseUrl,

        @NotBlank(message = "PLUGGY_CLIENT_ID não foi definido no .env")
        String clientId,

        @NotBlank(message = "PLUGGY_CLIENT_SECRET não foi definido no .env")
        String clientSecret,

        @NotNull(message = "pluggy.api-key-duracao não foi configurado")
        Duration apiKeyDuracao,

        /*
         * Ids das conexões (items) com os bancos. No .env ficam separados
         * por vírgula, e o Spring transforma em lista sozinho.
         * Não é obrigatório: sem itens, o app sobe, mas não sincroniza nada.
         */
        List<String> itemIds
) {

    /** Garante que nunca devolve null, mesmo se a variável estiver vazia. */
    public List<String> itemIds() {
        return itemIds == null ? List.of() : itemIds;
    }
}
