package com.bank.digitalbank.controller.exception;

// Exceção específica para quando uma conta informada não existir
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}