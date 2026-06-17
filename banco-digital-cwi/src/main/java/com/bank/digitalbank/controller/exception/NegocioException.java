package com.bank.digitalbank.controller.exception;

// Exceção genérica para erros de validação de negócio (ex: saldo insuficiente)
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}