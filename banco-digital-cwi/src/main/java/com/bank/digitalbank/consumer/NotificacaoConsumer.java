package com.bank.digitalbank.consumer;

import com.bank.digitalbank.config.RabbitMQConfig;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;



@Slf4j
@Component
public class NotificacaoConsumer {


    /**
     * Consome as mensagens da fila de notificações de forma assíncrona.
     * Simula o disparo de um mecanismo de Push/Email para o cliente final.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICACAO_QUEUE)
    @Retry(name = "notificationRetry")
    public void consumirNotificacao(String payloadJson) {
        log.info("Consumidor RabbitMQ recebeu nova mensagem para envio de notificação.");

        try {
            // Aqui ocorreria o parse do JSON e a chamada para o serviço externo de Push/SMS
            log.info("Enviando Push Notification com o payload: {}", payloadJson);

            // Simulação de sucesso no envio do Push
            log.info("Notificação enviada com sucesso para o dispositivo do cliente!");

        } catch (Exception e) {
            log.error("Falha ao processar e enviar a notificação para o cliente.", e);
            // Ao lançar a exceção, o Spring AMQP intercepta e, com base nas configurações,
            // pode aplicar retries ou direcionar a mensagem para a DLQ (Dead Letter Queue)
            throw e;
        }
    }
}