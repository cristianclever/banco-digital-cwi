package com.bank.digitalbank.domain.service;


import com.bank.digitalbank.domain.model.Conta;
import com.bank.digitalbank.domain.model.Movimentacao;
import com.bank.digitalbank.domain.model.OutboxEvent;
import com.bank.digitalbank.domain.repository.ContaRepository;
import com.bank.digitalbank.domain.repository.MovimentacaoRepository;
import com.bank.digitalbank.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper; // Jackson para serializar o payload

    @Transactional
    public void transferir(String origemId, String destinoId, BigDecimal valor) {
        log.info("Iniciando transferência de {} para {} no valor de {}", origemId, destinoId, valor);

        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException("A conta de origem não pode ser igual à conta de destino.");
        }

        // 1. Evitando Deadlock: Garante que os Locks Pessimistas ocorram sempre na mesma sequência de IDs
        Conta contaOrigem;
        Conta contaDestino;


        if (origemId.compareTo(destinoId) < 0) {
            contaOrigem = adquirirContaComLock(origemId);
            contaDestino = adquirirContaComLock(destinoId);
        } else {
            contaDestino = adquirirContaComLock(destinoId);
            contaOrigem = adquirirContaComLock(origemId);
        }

        // 2.  Executa as operações de negócio encapsuladas no Modelo (Validação de saldo ocorre aqui)
        contaOrigem.debitar(valor);
        contaDestino.creditar(valor);

        // Salva os novos saldos atualizados
        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        // 3. Registra a Movimentação no Histórico
        String movimentacaoId = UUID.randomUUID().toString();
        Movimentacao movimentacao = new Movimentacao(movimentacaoId, origemId, destinoId, valor);
        movimentacaoRepository.save(movimentacao);

        // 4. Constrói e insere o Evento no Outbox na MESMA transação do banco de dados
        try {
            TransferenciaEventPayload payloadDto = new TransferenciaEventPayload(
                    movimentacaoId,
                    origemId,
                    destinoId,
                    contaDestino.getEmail(), // Enviaremos a notificação para quem recebe o dinheiro
                    valor
            );
            String jsonPayload = objectMapper.writeValueAsString(payloadDto);

            OutboxEvent outboxEvent = new OutboxEvent(
                    UUID.randomUUID().toString(),
                    "TRANSFERENCIA",
                    movimentacaoId,
                    jsonPayload
            );
            outboxEventRepository.save(outboxEvent);
            log.debug("Evento Outbox registrado com sucesso para a movimentação {}", movimentacaoId);

        } catch (Exception e) {
            log.error("Falha ao serializar o payload do Outbox", e);
            // Fazemos o rollback de toda a operação caso ocorra erro inesperado na montagem do evento
            throw new IllegalStateException("Erro interno ao processar evento de transferência.", e);
        }

        log.info("Transferência concluída com sucesso e integrada ao Outbox: {}", movimentacaoId);
    }

    private Conta adquirirContaComLock(String id) {
        return contaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para o ID: " + id));
    }
}
