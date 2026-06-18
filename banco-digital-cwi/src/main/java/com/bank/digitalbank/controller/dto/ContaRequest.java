package com.bank.digitalbank.controller.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;


/**
 * DTO para recebimento de dados de cadastro de contas com Bean Validation ativo.
 */
public record ContaRequest(
        // ID é opcional (se não informado, nosso service gerará um UUID automaticamente)
        String id,

        @NotBlank(message = "O nome do cliente é obrigatório.")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado deve ser válido.")
        String email,

        @NotNull(message = "O saldo inicial é obrigatório.")
        @PositiveOrZero(message = "O saldo inicial não pode ser um valor negativo.")
        BigDecimal saldoInicial
) {}
