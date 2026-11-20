package dev.alisonsantos.appgastos.pluggy;

import dev.alisonsantos.appgastos.pluggy.dto.PluggyAccount;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyBill;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyCursorResponse;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyPaginaResponse;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consultas à API da Pluggy: contas, transações e faturas.
 *
 * Faz pelo código as mesmas chamadas que você fazia no Postman,
 * já cuidando de três coisas chatas:
 *
 * 1. A API Key: pega do PluggyAuthClient (com cache) e manda no header.
 * 2. As páginas: busca todas, até não sobrar nenhuma.
 * 3. A chave recusada: se a Pluggy responder 401/403, gera uma chave nova
 *    e tenta de novo, uma única vez.
 */
@Component
public class PluggyApiClient {

    private static final Logger log = LoggerFactory.getLogger(PluggyApiClient.class);

    private static final String HEADER_API_KEY = "X-API-KEY";
    private static final int TAMANHO_PAGINA = 500;

    /*
     * ParameterizedTypeReference: como as respostas são genéricas
     * (PluggyPaginaResponse<T>), o Java "esquece" o T em tempo de execução.
     * Isso aqui diz ao Jackson qual é o T de cada chamada.
     */
    private static final ParameterizedTypeReference<PluggyPaginaResponse<PluggyAccount>> TIPO_CONTAS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<PluggyPaginaResponse<PluggyBill>> TIPO_FATURAS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<PluggyCursorResponse<PluggyTransaction>> TIPO_TRANSACOES =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final PluggyAuthClient authClient;

    public PluggyApiClient(RestClient pluggyRestClient, PluggyAuthClient authClient) {
        this.restClient = pluggyRestClient;
        this.authClient = authClient;
    }

    // ------------------------------------------------------------------
    // Contas
    // ------------------------------------------------------------------

    /** GET /accounts?itemId=... : as contas (corrente e cartão) de um banco. */
    public List<PluggyAccount> listarContas(String itemId) {
        PluggyPaginaResponse<PluggyAccount> resposta = executar(apiKey -> restClient.get()
                .uri(builder -> builder.path("/accounts")
                        .queryParam("itemId", "{itemId}")
                        .build(Map.of("itemId", itemId)))
                .header(HEADER_API_KEY, apiKey)
                .retrieve()
                .body(TIPO_CONTAS));

        List<PluggyAccount> contas = resultados(resposta);
        log.info("Pluggy: {} conta(s) encontradas no item {}", contas.size(), itemId);
        return contas;
    }

    // ------------------------------------------------------------------
    // Transações (paginação por cursor)
    // ------------------------------------------------------------------

    /**
     * GET /v2/transactions : todas as transações de uma conta no período.
     *
     * As datas são opcionais. Sem elas, a Pluggy devolve tudo o que tiver.
     * A busca continua página a página enquanto a Pluggy mandar um "next".
     */
    public List<PluggyTransaction> listarTransacoes(String accountId, LocalDate de, LocalDate ate) {
        List<PluggyTransaction> todas = new ArrayList<>();

        // Parâmetros da primeira página
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("accountId", accountId);
        if (de != null) {
            parametros.put("dateFrom", de.toString());
        }
        if (ate != null) {
            parametros.put("dateTo", ate.toString());
        }

        int paginas = 0;
        while (parametros != null) {
            final Map<String, String> parametrosDaPagina = parametros;

            PluggyCursorResponse<PluggyTransaction> resposta = executar(apiKey -> restClient.get()
                    .uri(builder -> montarUri(builder, "/v2/transactions", parametrosDaPagina))
                    .header(HEADER_API_KEY, apiKey)
                    .retrieve()
                    .body(TIPO_TRANSACOES));

            paginas++;
            if (resposta != null && resposta.results() != null) {
                todas.addAll(resposta.results());
            }
            // Próxima página: os parâmetros vêm prontos no "next" (ou null, se acabou)
            parametros = resposta == null ? null : parametrosDoCursor(resposta.next());
        }

        log.info("Pluggy: {} transação(ões) em {} página(s) da conta {}", todas.size(), paginas, accountId);
        return todas;
    }

    // ------------------------------------------------------------------
    // Faturas (paginação por número de página)
    // ------------------------------------------------------------------

    /** GET /bills?accountId=... : as faturas fechadas de uma conta de cartão. */
    public List<PluggyBill> listarFaturas(String accountId) {
        List<PluggyBill> todas = new ArrayList<>();
        int pagina = 1;
        int totalPaginas;

        do {
            final int paginaAtual = pagina;
            PluggyPaginaResponse<PluggyBill> resposta = executar(apiKey -> restClient.get()
                    .uri(builder -> builder.path("/bills")
                            .queryParam("accountId", "{accountId}")
                            .queryParam("pageSize", "{pageSize}")
                            .queryParam("page", "{page}")
                            .build(Map.of(
                                    "accountId", accountId,
                                    "pageSize", TAMANHO_PAGINA,
                                    "page", paginaAtual)))
                    .header(HEADER_API_KEY, apiKey)
                    .retrieve()
                    .body(TIPO_FATURAS));

            todas.addAll(resultados(resposta));
            totalPaginas = (resposta == null || resposta.totalPages() == null) ? 1 : resposta.totalPages();
            pagina++;
        } while (pagina <= totalPaginas);

        log.info("Pluggy: {} fatura(s) da conta {}", todas.size(), accountId);
        return todas;
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /**
     * Executa uma chamada com a API Key. Se a Pluggy recusar a chave
     * (401/403), descarta o cache, gera uma chave nova e tenta de novo,
     * uma única vez. Qualquer outro erro vira uma PluggyException.
     */
    private <T> T executar(Function<String, T> chamada) {
        try {
            return chamada.apply(authClient.obterApiKey());

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status != 401 && status != 403) {
                throw traduzir(e);
            }
            log.warn("Pluggy recusou a API Key (HTTP {}). Gerando uma nova e tentando de novo.", status);
            authClient.invalidar();
            try {
                return chamada.apply(authClient.obterApiKey());
            } catch (RestClientResponseException segundaFalha) {
                throw traduzir(segundaFalha);
            } catch (ResourceAccessException segundaFalha) {
                throw semConexao(segundaFalha);
            }

        } catch (ResourceAccessException e) {
            throw semConexao(e);
        }
    }

    /** Transforma o erro HTTP numa mensagem em português que diz o que fazer. */
    private PluggyException traduzir(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        return switch (status) {
            case 400 -> new PluggyException(
                    "A Pluggy recusou a requisição (HTTP 400): " + e.getResponseBodyAsString(), e);
            case 401, 403 -> new PluggyException(
                    "A Pluggy recusou o acesso (HTTP " + status + "), mesmo com uma API Key nova.", e);
            case 404 -> new PluggyException(
                    "Não encontrado na Pluggy (HTTP 404). Confira o id informado.", e);
            case 429 -> new PluggyException(
                    "Limite de requisições da Pluggy atingido (HTTP 429). Tente de novo em alguns minutos.", e);
            default -> new PluggyException("Erro na Pluggy (HTTP " + status + ").", e);
        };
    }

    private PluggyException semConexao(ResourceAccessException e) {
        return new PluggyException(
                "Não foi possível conectar na Pluggy. Verifique a conexão com a internet.", e);
    }

    /**
     * Monta a URL com os parâmetros como variáveis. Assim o Spring codifica
     * cada valor de forma segura (o cursor da Pluggy pode ter "+", "/" e "=").
     */
    private URI montarUri(UriBuilder builder, String caminho, Map<String, String> parametros) {
        builder.path(caminho);
        parametros.keySet().forEach(nome -> builder.queryParam(nome, "{" + nome + "}"));
        return builder.build(parametros);
    }

    /**
     * Lê o "next" da Pluggy (ex.: "?accountId=...&after=...") e devolve
     * os parâmetros já decodificados. Null quando não há próxima página.
     *
     * UriUtils.decode, e não URLDecoder: o URLDecoder transforma "+" em
     * espaço, o que corromperia o cursor.
     */
    private Map<String, String> parametrosDoCursor(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        String query = next.startsWith("?") ? next.substring(1) : next;
        MultiValueMap<String, String> brutos = UriComponentsBuilder.newInstance()
                .query(query)
                .build()
                .getQueryParams();

        Map<String, String> parametros = new LinkedHashMap<>();
        brutos.forEach((nome, valores) ->
                parametros.put(nome, UriUtils.decode(valores.getFirst(), StandardCharsets.UTF_8)));
        return parametros;
    }

    private <T> List<T> resultados(PluggyPaginaResponse<T> resposta) {
        if (resposta == null || resposta.results() == null) {
            return List.of();
        }
        return resposta.results();
    }
}
