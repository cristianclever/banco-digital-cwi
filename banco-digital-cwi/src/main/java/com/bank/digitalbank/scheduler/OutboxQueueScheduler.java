package com.bank.digitalbank.scheduler;


import com.bank.digitalbank.config.RabbitMQConfig;
import com.bank.digitalbank.domain.model.OutboxEvent;
import com.bank.digitalbank.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxQueueScheduler {

    private final OutboxEventRepository outboxEventRepository;

    private final RabbitTemplate rabbitTemplate;

    /**
     * Executa a cada 60000 milissegundos (1 minuto) após o término da última execução.
     * Abre uma transação para manter o lock ativo durante a leitura e o envio.
     * Atualizado para ser parametrizavel via variavel de ambiente
     */
    @Scheduled( fixedDelayString = "${app.outbox.processing.delay:60000}")
    @Transactional
    public void processarEventosOutbox() {

        // 1. Busca o lote de forma segura ignorando registros travados por outras instâncias
        // No método processarEventosOutbox do seu OutboxQueueScheduler:
        List<OutboxEvent> eventosPendentes = outboxEventRepository.findPendingForProcessing(
                org.springframework.data.domain.PageRequest.of(0, 100)
        );


        if (eventosPendentes.isEmpty()) {
            return;
        }

        log.info("Scheduler localizou {} eventos pendentes para processamento no Outbox.", eventosPendentes.size());

        for (OutboxEvent evento : eventosPendentes) {
            try {
                // 2. Publica o payload (String JSON) diretamente na Exchange mapeada no RabbitMQConfig
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICACAO_EXCHANGE,
                        RabbitMQConfig.NOTIFICACAO_ROUTING_KEY,
                        evento.getPayload()
                );

                // 3. Modifica o estado do evento para PROCESSED dentro da transação atual
                evento.marcarComoProcessado();
                outboxEventRepository.save(evento);

                log.debug("Evento Outbox {} enviado com sucesso ao broker.", evento.getId());

            } catch (Exception e) {
                log.error("Falha ao enviar evento Outbox {} para o RabbitMQ. Será tentado novamente.", evento.getId(), e);
                // Marcar como falhado se necessário, ou apenas não salvar para deixar PENDING para o próximo loop
                evento.marcarComoFalhado();
                outboxEventRepository.save(evento);
            }
        }
    }
}