package dev.alisonsantos.appgastos.legenda;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Cadastro das regras da legenda.
 *
 * GET    /api/regras                 as ativas, com quantas compras cada uma identificou
 * GET    /api/regras?incluirInativas=true
 * GET    /api/regras/{id}
 * POST   /api/regras                 cria e reaplica a legenda (201)
 * PUT    /api/regras/{id}            edita e reaplica a legenda
 * DELETE /api/regras/{id}            desativa e reaplica a legenda (204)
 */
@RestController
@RequestMapping("/api/regras")
public class RegraController {

    private final RegraService regraService;

    public RegraController(RegraService regraService) {
        this.regraService = regraService;
    }

    @GetMapping
    public List<RegraResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return regraService.listar(incluirInativas);
    }

    @GetMapping("/{id}")
    public RegraResponse buscar(@PathVariable Long id) {
        return regraService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<RegraResponse> criar(@Valid @RequestBody RegraRequest pedido) {
        RegraResponse criada = regraService.criar(pedido);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @PutMapping("/{id}")
    public RegraResponse atualizar(@PathVariable Long id, @Valid @RequestBody RegraRequest pedido) {
        return regraService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        regraService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
