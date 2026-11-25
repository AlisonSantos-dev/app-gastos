package dev.alisonsantos.appgastos.contafixa;

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
 * Cadastro das contas fixas.
 *
 * GET    /api/contas-fixas
 * GET    /api/contas-fixas/{id}
 * POST   /api/contas-fixas          (201)
 * PUT    /api/contas-fixas/{id}
 * DELETE /api/contas-fixas/{id}     desativa (204)
 */
@RestController
@RequestMapping("/api/contas-fixas")
public class ContaFixaController {

    private final ContaFixaService service;

    public ContaFixaController(ContaFixaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContaFixaResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return service.listar(incluirInativas);
    }

    @GetMapping("/{id}")
    public ContaFixaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<ContaFixaResponse> criar(@Valid @RequestBody ContaFixaRequest pedido) {
        ContaFixaResponse criada = service.criar(pedido);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @PutMapping("/{id}")
    public ContaFixaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaFixaRequest pedido) {
        return service.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
