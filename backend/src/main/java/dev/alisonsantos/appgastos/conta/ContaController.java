package dev.alisonsantos.appgastos.conta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/contas
 *
 * As contas (corrente e cartão) com os seus cartões.
 */
@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final ContaConsultaService consultaService;

    public ContaController(ContaConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    public List<ContaResponse> listar() {
        return consultaService.listar();
    }
}
