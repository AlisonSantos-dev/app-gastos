package dev.alisonsantos.appgastos.contafixa;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Os pagamentos das contas fixas.
 *
 * POST /api/contas-fixas/competencias/2026-11/conciliar   procura os pagamentos do mês no extrato
 * POST /api/contas-fixas/lancamentos/{id}/vincular        escolhe o pagamento à mão
 */
@RestController
@RequestMapping("/api/contas-fixas")
public class ConciliacaoController {

    private final ConciliacaoService service;

    public ConciliacaoController(ConciliacaoService service) {
        this.service = service;
    }

    @PostMapping("/competencias/{competencia}/conciliar")
    public ResultadoConciliacao conciliar(@PathVariable String competencia) {
        return service.conciliar(Competencias.ler(competencia));
    }

    @PostMapping("/lancamentos/{id}/vincular")
    public ContaFixaMesResponse vincular(@PathVariable Long id, @Valid @RequestBody VincularRequest pedido) {
        return service.vincularManualmente(id, pedido.transacaoId());
    }
}
