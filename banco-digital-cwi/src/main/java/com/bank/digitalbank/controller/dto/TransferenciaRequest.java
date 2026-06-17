package com.bank.digitalbank.controller.dto;

import java.math.BigDecimal;

public record TransferenciaRequest(
        String contaOrigemId,
        String contaDestinoId,
        BigDecimal valor
) {}