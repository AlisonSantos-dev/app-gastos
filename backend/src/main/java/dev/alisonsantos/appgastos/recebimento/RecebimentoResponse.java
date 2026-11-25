package dev.alisonsantos.appgastos.recebimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record RecebimentoResponse(
        Long id,
        Long pessoaId,
        String pessoa,
        YearMonth competencia,
        BigDecimal valor,
        LocalDate data,
        Long transacaoId,
        String observacao
) {

    static RecebimentoResponse de(Recebimento r) {
        return new RecebimentoResponse(r.getId(), r.getPessoa().getId(), r.getPessoa().getNome(),
                r.getCompetencia(), r.getValor(), r.getData(),
                r.getTransacao() == null ? null : r.getTransacao().getId(), r.getObservacao());
    }
}
