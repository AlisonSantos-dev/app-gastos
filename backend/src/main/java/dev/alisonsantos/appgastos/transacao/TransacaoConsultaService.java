package dev.alisonsantos.appgastos.transacao;

import dev.alisonsantos.appgastos.comum.PaginaResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consulta de transações com filtros e paginação.
 *
 * @Transactional(readOnly = true): a consulta e a conversão para DTO
 * acontecem dentro da mesma transação, só de leitura. Isso é necessário
 * porque o "open-in-view" está desligado: fora da transação, os dados
 * relacionados (conta, cartão...) não poderiam mais ser carregados.
 */
@Service
public class TransacaoConsultaService {

    /** As datas do filtro são do calendário brasileiro, não de UTC. */
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final int TAMANHO_MAXIMO = 200;

    private final TransacaoRepository transacaoRepository;

    public TransacaoConsultaService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<TransacaoResponse> buscar(TransacaoFiltro filtro, int pagina, int tamanho) {
        // Limita o tamanho da página: ninguém pede 1 milhão de linhas sem querer
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), tamanhoSeguro,
                Sort.by(Sort.Direction.DESC, "dataTransacao").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Transacao> resultado = transacaoRepository.findAll(especificacao(filtro), paginacao);
        return PaginaResponse.de(resultado.map(TransacaoResponse::de));
    }

    /**
     * Monta o WHERE da consulta só com os filtros preenchidos.
     * Cada "if" acrescenta uma condição; no final, todas são unidas com AND.
     */
    private Specification<Transacao> especificacao(TransacaoFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (filtro.contaId() != null) {
                condicoes.add(cb.equal(root.get("conta").get("id"), filtro.contaId()));
            }
            if (filtro.cartao() != null && !filtro.cartao().isBlank()) {
                condicoes.add(cb.equal(root.get("cartao").get("ultimosDigitos"), filtro.cartao().trim()));
            }
            if (filtro.faturaId() != null) {
                condicoes.add(cb.equal(root.get("fatura").get("id"), filtro.faturaId()));
            }
            if (filtro.tipo() != null) {
                condicoes.add(cb.equal(root.get("tipoLancamento"), filtro.tipo()));
            }
            if (filtro.somenteGastos()) {
                condicoes.add(cb.isTrue(root.<Boolean>get("contaNosGastos")));
            }
            if (filtro.de() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.<Instant>get("dataTransacao"),
                        inicioDoDia(filtro.de())));
            }
            if (filtro.ate() != null) {
                // "Até" inclusive: tudo antes do começo do dia seguinte
                condicoes.add(cb.lessThan(root.<Instant>get("dataTransacao"),
                        inicioDoDia(filtro.ate().plusDays(1))));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                condicoes.add(cb.like(cb.lower(root.<String>get("descricaoBanco")),
                        "%" + escaparLike(filtro.texto().trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }

            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }

    private Instant inicioDoDia(LocalDate data) {
        return data.atStartOfDay(FUSO_BRASIL).toInstant();
    }

    /** Evita que um "%" ou "_" digitado na busca vire curinga do LIKE. */
    private String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
