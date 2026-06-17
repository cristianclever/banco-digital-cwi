package com.bank.digitalbank.controller.dto;

import java.math.BigDecimal;

public record ContaRequest(
        String id,
        String nome,
        String email,
        BigDecimal saldoInicial
) {}
