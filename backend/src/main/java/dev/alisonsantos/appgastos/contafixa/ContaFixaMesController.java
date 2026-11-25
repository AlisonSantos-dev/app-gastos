package dev.alisonsantos.appgastos.contafixa;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * As contas fixas de cada mês.
 *
 * GET   /api/contas-fixas/competencias/2026-11          o mês, com os totais
 * POST  /api/contas-fixas/competencias/2026-11/gerar    cria as contas que faltam no mês
 * PATCH /api/contas-fixas/lancamentos/{id}              pagar, dispensar, reabrir, ajustar previsão
 */
@RestController
@RequestMapping("/api/contas-fixas")
public class ContaFixaMesController {

    private final ContaFixaMesService service;

    public ContaFixaMesController(ContaFixaMesService service) {
        this.service = service;
    }

    @GetMapping("/competencias/{competencia}")
    public ResumoMesResponse listar(@PathVariable String competencia) {
        return service.listar(Competencias.ler(competencia));
    }

    @PostMapping("/competencias/{competencia}/gerar")
    public ResumoMesResponse gerar(@PathVariable String competencia) {
        return service.gerar(Competencias.ler(competencia));
    }

    @PatchMapping("/lancamentos/{id}")
    public ContaFixaMesResponse atualizar(@PathVariable Long id, @Valid @RequestBody LancamentoContaRequest pedido) {
        return service.atualizar(id, pedido);
    }
}
