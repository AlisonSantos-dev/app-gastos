package dev.alisonsantos.appgastos.comum;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento centralizado de erros da API.
 *
 * @RestControllerAdvice: vale para TODOS os controllers. Em vez de cada um
 * tratar os seus erros com try/catch, eles só lançam a exceção, e este
 * lugar transforma cada tipo de exceção na resposta HTTP certa, sempre
 * no mesmo formato (ErroResponse).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 400: o corpo da requisição não passou nas validações (@NotBlank, @Size...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ErroResponse.deCampos("Confira os campos destacados.", campos));
    }

    /** 400: JSON mal formado, ou um valor que não existe num enum (ex.: tipo "AMIGO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ErroResponse.de(400, "Corpo inválido",
                "O corpo da requisição está mal formado ou tem um valor inválido."));
    }

    /** 400: parâmetro da URL com o tipo errado (ex.: /api/pessoas/abc, ou tipo=XYZ). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ErroResponse.de(400, "Parâmetro inválido",
                "O valor \"" + e.getValue() + "\" não é válido para o parâmetro \"" + e.getName() + "\"."));
    }

    /** 404: o recurso pedido não existe. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(404).body(ErroResponse.de(404, "Não encontrado", e.getMessage()));
    }

    /** 422: o pedido quebra uma regra do app. */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e) {
        return ResponseEntity.status(422).body(ErroResponse.de(422, "Regra de negócio", e.getMessage()));
    }
}
