package dev.alisonsantos.appgastos.fatura;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/faturas?contaId=...
 *
 * As faturas de uma conta de cartão, da mais recente para a mais antiga,
 * cada uma com a conciliação contra o total oficial do banco.
 */
@RestController
@RequestMapping("/api/faturas")
public class FaturaController {

    private final FaturaConsultaService consultaService;

    public FaturaController(FaturaConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    public List<FaturaResponse> listar(@RequestParam Long contaId) {
        return consultaService.listar(contaId);
    }
}
