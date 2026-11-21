package dev.alisonsantos.appgastos.comum;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Resposta paginada, com um formato estável e em português.
 *
 * Em vez de devolver o Page do Spring direto (que tem dezenas de campos
 * internos e pode mudar entre versões), a API devolve só o que o front
 * precisa: o conteúdo da página e as informações de paginação.
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
