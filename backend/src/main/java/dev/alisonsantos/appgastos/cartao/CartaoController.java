package dev.alisonsantos.appgastos.cartao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/cartoes        todos os cartões, com dono e candidatos
 * GET /api/cartoes/{id}
 * PUT /api/cartoes/{id}   configura apelido, tipo, cor e de quem é
 */
@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {

    private final CartaoService cartaoService;

    public CartaoController(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    @GetMapping
    public List<CartaoResponse> listar() {
        return cartaoService.listar();
    }

    @GetMapping("/{id}")
    public CartaoResponse buscar(@PathVariable Long id) {
        return cartaoService.buscar(id);
    }

    @PutMapping("/{id}")
    public CartaoResponse configurar(@PathVariable Long id, @Valid @RequestBody CartaoRequest pedido) {
        return cartaoService.configurar(id, pedido);
    }
}
