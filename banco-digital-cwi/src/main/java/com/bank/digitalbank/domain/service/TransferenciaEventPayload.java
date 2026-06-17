package com.bank.digitalbank.domain.service;

import java.math.BigDecimal;

/**
 * Record que representa os dados estruturados que o RabbitMQ precisa enviar.
 * Será convertido em string JSON para preencher a coluna 'payload' da tabela Outbox.
 */
public record TransferenciaEventPayload(
        String movimentacaoId,
        String contaOrigemId,
        String contaDestinoId,
        String emailDestinatario,
        BigDecimal valor
) {}