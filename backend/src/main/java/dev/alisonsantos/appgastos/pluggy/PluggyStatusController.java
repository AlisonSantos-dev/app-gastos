package dev.alisonsantos.appgastos.pluggy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint para conferir se a conexão com a Pluggy está funcionando.
 *
 * GET /api/pluggy/status
 *
 * Útil agora, para testar a autenticação, e depois, para diagnosticar
 * problemas de sincronização. Na Fase 4 ele passa a exigir login de admin.
 */
@RestController
@RequestMapping("/api/pluggy")
public class PluggyStatusController {

    private final PluggyAuthClient authClient;

    public PluggyStatusController(PluggyAuthClient authClient) {
        this.authClient = authClient;
    }

    @GetMapping("/status")
    public ResponseEntity<PluggyStatusResponse> status() {
        try {
            authClient.obterApiKey();
            return ResponseEntity.ok(new PluggyStatusResponse(
                    true,
                    authClient.getValidaAte(),
                    "Autenticado na Pluggy"));

        } catch (PluggyException e) {
            // 502 Bad Gateway: o nosso app está ok, quem falhou foi o serviço externo
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new PluggyStatusResponse(
                    false,
                    null,
                    e.getMessage()));
        }
    }
}
