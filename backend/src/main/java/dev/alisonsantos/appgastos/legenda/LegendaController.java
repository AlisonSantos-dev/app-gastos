package dev.alisonsantos.appgastos.legenda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * POST /api/legenda/reaplicar       aplica as regras em todas as transações
 * GET  /api/legenda/pendentes       as descrições sem regra mais frequentes
 */
@RestController
@RequestMapping("/api/legenda")
public class LegendaController {

    private final LegendaService legendaService;

    public LegendaController(LegendaService legendaService) {
        this.legendaService = legendaService;
    }

    @PostMapping("/reaplicar")
    public ResumoLegenda reaplicar() {
        return legendaService.reaplicarTodas();
    }

    @GetMapping("/pendentes")
    public List<RegraRepository.DescricaoPendente> pendentes(@RequestParam(defaultValue = "30") int limite) {
        return legendaService.pendentes(limite);
    }
}
