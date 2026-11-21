package dev.alisonsantos.appgastos.pessoa;

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
 * Cadastro de pessoas.
 *
 * GET    /api/pessoas                   lista as ativas
 * GET    /api/pessoas?incluirInativas=true
 * GET    /api/pessoas/{id}
 * POST   /api/pessoas                   cria (201 Created)
 * PUT    /api/pessoas/{id}              edita
 * DELETE /api/pessoas/{id}              desativa (204 No Content)
 *
 * Repara que o controller não tem try/catch: os erros são tratados
 * pelo ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @GetMapping
    public List<PessoaResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return pessoaService.listar(incluirInativas);
    }

    @GetMapping("/{id}")
    public PessoaResponse buscar(@PathVariable Long id) {
        return pessoaService.buscar(id);
    }

    /**
     * 201 Created, com o header "Location" apontando para a pessoa criada
     * (ex.: /api/pessoas/5). É o padrão REST para criação.
     */
    @PostMapping
    public ResponseEntity<PessoaResponse> criar(@Valid @RequestBody PessoaRequest pedido) {
        PessoaResponse criada = pessoaService.criar(pedido);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @PutMapping("/{id}")
    public PessoaResponse atualizar(@PathVariable Long id, @Valid @RequestBody PessoaRequest pedido) {
        return pessoaService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        pessoaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
