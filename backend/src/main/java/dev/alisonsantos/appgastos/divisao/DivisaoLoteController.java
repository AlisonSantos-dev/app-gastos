package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/divisoes/lote
 *
 * Divide várias compras de uma vez, com a mesma divisão: o "evento"
 * (o churrasco, a viagem), ou aplicar uma divisão salva em várias compras.
 */
@RestController
@RequestMapping("/api/divisoes")
public class DivisaoLoteController {

    private final DivisaoService divisaoService;

    public DivisaoLoteController(DivisaoService divisaoService) {
        this.divisaoService = divisaoService;
    }

    @PostMapping("/lote")
    public DivisaoLoteResponse dividirLote(@Valid @RequestBody DivisaoLoteRequest pedido) {
        return divisaoService.dividirLote(pedido);
    }
}
