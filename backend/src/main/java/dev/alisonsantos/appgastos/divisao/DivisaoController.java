package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A divisão de uma compra é um "sub-recurso" da transação:
 *
 * PUT    /api/transacoes/{id}/divisao    divide (ou troca a divisão existente)
 * DELETE /api/transacoes/{id}/divisao    desfaz a divisão
 *
 * PUT, e não POST, porque é idempotente: mandar a mesma divisão duas
 * vezes dá o mesmo resultado (a segunda substitui a primeira).
 */
@RestController
@RequestMapping("/api/transacoes/{id}/divisao")
public class DivisaoController {

    private final DivisaoService divisaoService;

    public DivisaoController(DivisaoService divisaoService) {
        this.divisaoService = divisaoService;
    }

    @PutMapping
    public DivisaoResponse dividir(@PathVariable Long id, @Valid @RequestBody DivisaoRequest pedido) {
        return divisaoService.dividir(id, pedido);
    }

    @DeleteMapping
    public DivisaoResponse desfazer(@PathVariable Long id,
                                    @RequestParam(defaultValue = "true") boolean propagarParcelas) {
        return divisaoService.desfazer(id, propagarParcelas);
    }
}
