package dev.alisonsantos.appgastos.conta;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/contas        as contas (corrente e cartão) com os seus cartões
 * PUT /api/contas/{id}   edita banco, nome, dono padrão e se está ativa
 */
@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final ContaConsultaService consultaService;
    private final ContaService contaService;

    public ContaController(ContaConsultaService consultaService, ContaService contaService) {
        this.consultaService = consultaService;
        this.contaService = contaService;
    }

    @GetMapping
    public List<ContaResponse> listar() {
        return consultaService.listar();
    }

    @PutMapping("/{id}")
    public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequest pedido) {
        contaService.atualizar(id, pedido);
        return consultaService.buscar(id);
    }
}
