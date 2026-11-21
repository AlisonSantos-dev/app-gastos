package dev.alisonsantos.appgastos.conta;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.cartao.CartaoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lista as contas com os seus cartões.
 *
 * Busca as contas e os cartões em duas consultas e junta em memória,
 * em vez de uma consulta de cartões para cada conta.
 */
@Service
public class ContaConsultaService {

    private final ContaRepository contaRepository;
    private final CartaoRepository cartaoRepository;

    public ContaConsultaService(ContaRepository contaRepository, CartaoRepository cartaoRepository) {
        this.contaRepository = contaRepository;
        this.cartaoRepository = cartaoRepository;
    }

    @Transactional(readOnly = true)
    public List<ContaResponse> listar() {
        Map<Long, List<Cartao>> cartoesPorConta = cartaoRepository.findAll().stream()
                .collect(Collectors.groupingBy(cartao -> cartao.getConta().getId()));

        return contaRepository.findAll(Sort.by("banco", "nome")).stream()
                .map(conta -> new ContaResponse(
                        conta.getId(),
                        conta.getBanco(),
                        conta.getNome(),
                        conta.getTipo().name(),
                        conta.isAtiva(),
                        cartoesPorConta.getOrDefault(conta.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(Cartao::getUltimosDigitos))
                                .map(this::resumir)
                                .toList()))
                .toList();
    }

    private ContaResponse.CartaoResumo resumir(Cartao cartao) {
        return new ContaResponse.CartaoResumo(
                cartao.getId(),
                cartao.getUltimosDigitos(),
                cartao.getApelido(),
                cartao.getTipo() == null ? null : cartao.getTipo().name(),
                cartao.getModoDono().name(),
                cartao.getStatus().name());
    }
}
