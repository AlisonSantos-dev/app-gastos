package dev.alisonsantos.appgastos.sync;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dispara a sincronização com a Pluggy.
 *
 * POST /api/sync
 *
 * É POST (e não GET) porque altera dados: cria e atualiza registros no banco.
 * Na Fase 6, o sync passa a rodar sozinho todo dia, e este endpoint fica
 * para quando você quiser forçar uma atualização na hora.
 */
@RestController
@RequestMapping("/api/sync")
public class SincronizacaoController {

    private final SincronizacaoService sincronizacaoService;

    public SincronizacaoController(SincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    @PostMapping
    public ResponseEntity<?> sincronizar() {
        try {
            return ResponseEntity.ok(sincronizacaoService.sincronizar());
        } catch (IllegalStateException e) {
            // 409 Conflict: já tem uma sincronização rodando
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", e.getMessage()));
        }
    }
}
