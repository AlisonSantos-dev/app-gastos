package dev.alisonsantos.appgastos.destino;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/destinos/reaplicar
 *
 * Reaplica o destino padrão (pelo cartão ou pela conta) em todas as
 * transações, preservando as escolhas manuais e as definidas por regra.
 */
@RestController
@RequestMapping("/api/destinos")
public class DestinoController {

    private final DestinoService destinoService;

    public DestinoController(DestinoService destinoService) {
        this.destinoService = destinoService;
    }

    @PostMapping("/reaplicar")
    public ResumoDestino reaplicar() {
        return destinoService.reaplicarTodas();
    }
}
