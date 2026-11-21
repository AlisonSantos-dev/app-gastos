package dev.alisonsantos.appgastos.edicao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PATCH /api/transacoes/{id}
 *
 * PATCH (e não PUT) porque é uma alteração PARCIAL: só os campos
 * enviados mudam. O PUT, pela convenção REST, substituiria o recurso inteiro.
 */
@RestController
@RequestMapping("/api/transacoes")
public class EdicaoTransacaoController {

    private final EdicaoTransacaoService edicaoService;

    public EdicaoTransacaoController(EdicaoTransacaoService edicaoService) {
        this.edicaoService = edicaoService;
    }

    @PatchMapping("/{id}")
    public EdicaoTransacaoResponse editar(@PathVariable Long id, @Valid @RequestBody EdicaoTransacaoRequest pedido) {
        return edicaoService.editar(id, pedido);
    }
}
