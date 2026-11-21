package dev.alisonsantos.appgastos.edicao;

import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import jakarta.validation.constraints.Size;

/**
 * O que a API recebe para editar uma transação à mão.
 *
 * Todos os campos são opcionais: só o que vier preenchido é alterado.
 * Para cada atributo, dá para DEFINIR à mão ou RESTAURAR (devolver o
 * controle para as regras automáticas).
 *
 * Exemplos:
 *   { "descricao": "Sofá da sala", "categoria": "Casa" }
 *   { "pessoaId": 3 }
 *   { "restaurarPessoa": true }
 *   { "tipoLancamento": "OUTRO", "contaNosGastos": false, "observacao": "Reembolso do trabalho" }
 *
 * @param propagarParcelas se a edição vale para todas as parcelas da compra (padrão: sim)
 */
public record EdicaoTransacaoRequest(

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String descricao,
        Boolean restaurarDescricao,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String categoria,
        Boolean restaurarCategoria,

        Long pessoaId,
        Boolean restaurarPessoa,

        TipoLancamento tipoLancamento,
        Boolean contaNosGastos,
        Boolean restaurarClassificacao,

        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        String observacao,

        Boolean propagarParcelas
) {

    boolean querRestaurarDescricao() {
        return Boolean.TRUE.equals(restaurarDescricao);
    }

    boolean querRestaurarCategoria() {
        return Boolean.TRUE.equals(restaurarCategoria);
    }

    boolean querRestaurarPessoa() {
        return Boolean.TRUE.equals(restaurarPessoa);
    }

    boolean querRestaurarClassificacao() {
        return Boolean.TRUE.equals(restaurarClassificacao);
    }

    /** Padrão: propaga. Só não propaga se vier "propagarParcelas": false. */
    boolean devePropagar() {
        return !Boolean.FALSE.equals(propagarParcelas);
    }

    /** Veio pelo menos um pedido de mudança? */
    boolean temAlteracao() {
        return descricao != null || querRestaurarDescricao()
                || categoria != null || querRestaurarCategoria()
                || pessoaId != null || querRestaurarPessoa()
                || tipoLancamento != null || contaNosGastos != null || querRestaurarClassificacao()
                || observacao != null;
    }
}
