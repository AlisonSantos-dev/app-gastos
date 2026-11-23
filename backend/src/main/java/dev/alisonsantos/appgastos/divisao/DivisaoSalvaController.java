package dev.alisonsantos.appgastos.divisao;

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
 * GET    /api/divisoes-salvas
 * GET    /api/divisoes-salvas/{id}
 * POST   /api/divisoes-salvas          (201)
 * PUT    /api/divisoes-salvas/{id}
 * DELETE /api/divisoes-salvas/{id}     desativa (204)
 */
@RestController
@RequestMapping("/api/divisoes-salvas")
public class DivisaoSalvaController {

    private final DivisaoSalvaService service;

    public DivisaoSalvaController(DivisaoSalvaService service) {
        this.service = service;
    }

    @GetMapping
    public List<DivisaoSalvaResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return service.listar(incluirInativas);
    }

    @GetMapping("/{id}")
    public DivisaoSalvaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<DivisaoSalvaResponse> criar(@Valid @RequestBody DivisaoSalvaRequest pedido) {
        DivisaoSalvaResponse criada = service.criar(pedido);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @PutMapping("/{id}")
    public DivisaoSalvaResponse atualizar(@PathVariable Long id, @Valid @RequestBody DivisaoSalvaRequest pedido) {
        return service.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
