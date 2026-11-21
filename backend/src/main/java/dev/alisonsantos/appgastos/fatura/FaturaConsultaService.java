package dev.alisonsantos.appgastos.fatura;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lista as faturas de uma conta de cartão já conciliadas:
 * total oficial x soma dos gastos do app.
 */
@Service
public class FaturaConsultaService {

    /** Diferença aceita na conciliação (centavos de arredondamento de parcelas). */
    private static final BigDecimal TOLERANCIA = new BigDecimal("1.00");

    private final FaturaRepository faturaRepository;

    public FaturaConsultaService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    @Transactional(readOnly = true)
    public List<FaturaResponse> listar(Long contaId) {
        Map<Long, FaturaRepository.SomaFatura> somas = faturaRepository.somarGastosPorFatura(contaId).stream()
                .collect(Collectors.toMap(FaturaRepository.SomaFatura::getFaturaId, Function.identity()));

        return faturaRepository.findByContaIdOrderByDataVencimentoDesc(contaId).stream()
                .map(fatura -> conciliar(fatura, somas.get(fatura.getId())))
                .toList();
    }

    private FaturaResponse conciliar(Fatura fatura, FaturaRepository.SomaFatura soma) {
        BigDecimal somaGastos = soma == null || soma.getSoma() == null ? BigDecimal.ZERO : soma.getSoma();
        long quantidade = soma == null ? 0 : soma.getQuantidade();

        BigDecimal diferenca = fatura.getTotalOficial() == null ? null
                : fatura.getTotalOficial().subtract(somaGastos);
        boolean bate = diferenca != null && diferenca.abs().compareTo(TOLERANCIA) <= 0;

        return new FaturaResponse(
                fatura.getId(),
                fatura.getDataFechamento(),
                fatura.getDataVencimento(),
                fatura.getStatus().name(),
                fatura.getTotalOficial(),
                somaGastos,
                diferenca,
                quantidade,
                bate);
    }
}
