package com.bank.digitalbank.controller.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * Captura as falhas de validação dos beans (@Valid) e monta uma resposta detalhada.
     * Retorna o status 422 Unprocessable Entity mantendo a coerência com as regras de negócio.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Captura todas as mensagens de erro geradas pelas anotações de validação e as une em uma única string
        String detalheErros = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "error", "Dados de Entrada Inválidos",
                "message", detalheErros
        ));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.NO_CONTENT.value(),
                "error", "Recurso não existe",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler({NegocioException.class, IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleNegocioException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "error", "Regra de Negócio Violada",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleFallbackGeral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Erro Interno do Servidor",
                "message", "Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde."
        ));
    }
}