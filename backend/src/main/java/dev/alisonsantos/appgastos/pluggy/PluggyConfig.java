package dev.alisonsantos.appgastos.pluggy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Configuração da integração com a Pluggy.
 *
 * Cria um RestClient já apontado para a URL da Pluggy. Todas as chamadas
 * à API (autenticação, contas, transações, faturas) usam esse mesmo cliente.
 */
@Configuration
@EnableConfigurationProperties(PluggyProperties.class)
public class PluggyConfig {

    @Bean
    public RestClient pluggyRestClient(PluggyProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
