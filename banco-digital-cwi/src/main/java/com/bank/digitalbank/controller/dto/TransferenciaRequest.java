package com.bank.digitalbank.controller.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO para requisição de transferência bancária com Bean Validation ativo.
 */
public record TransferenciaRequest(
        @NotBlank(message = "A conta de origem é obrigatória.")
        String contaOrigemId,

        @NotBlank(message = "A conta de destino é obrigatória.")
        String contaDestinoId,

        @NotNull(message = "O valor da transferência é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor mínimo para transferência deve ser de R$ 0.01.")
        BigDecimal valor
) {}