package dev.alisonsantos.appgastos.recebimento;

import dev.alisonsantos.appgastos.comum.Competencia;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Os recebimentos.
 *
 * GET    /api/recebimentos/competencias/2026-11   quem já pagou novembro
 * GET    /api/recebimentos/sugestoes?dias=60      os Pix recebidos que parecem pagamentos
 * POST   /api/recebimentos                        registra (o "tique" na sugestão, ou um pagamento em dinheiro)
 * DELETE /api/recebimentos/{id}                   desfaz
 */
@RestController
@RequestMapping("/api/recebimentos")
public class RecebimentoController {

    private final RecebimentoService service;

    public RecebimentoController(RecebimentoService service) {
        this.service = service;
    }

    @GetMapping("/competencias/{competencia}")
    public PainelRecebimentosResponse painel(@PathVariable String competencia) {
        return service.painel(Competencia.ler(competencia));
    }

    @GetMapping("/sugestoes")
    public List<SugestaoRecebimentoResponse> sugestoes(@RequestParam(defaultValue = "60") int dias) {
        return service.sugestoes(dias);
    }

    @PostMapping
    public ResponseEntity<RecebimentoResponse> registrar(@Valid @RequestBody RecebimentoRequest pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(pedido));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
