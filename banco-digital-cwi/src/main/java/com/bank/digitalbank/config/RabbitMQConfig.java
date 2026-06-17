package com.bank.digitalbank.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICACAO_EXCHANGE = "bank.notificacao.exchange";
    public static final String NOTIFICACAO_QUEUE = "bank.notificacao.queue";
    public static final String NOTIFICACAO_ROUTING_KEY = "bank.notificacao.routing-key";

    public static final String NOTIFICACAO_DLQ_EXCHANGE = "bank.notificacao.dlq.exchange";
    public static final String NOTIFICACAO_DLQ_QUEUE = "bank.notificacao.dlq.queue";
    public static final String NOTIFICACAO_DLQ_ROUTING_KEY = "bank.notificacao.dlq.routing-key";

    // Adiciona o RabbitAdmin e força a declaração dos elementos no Boot da aplicação
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);

        // Força a criação das exchanges, filas e bindings imediatamente
        admin.declareExchange(notificacaoExchange());
        admin.declareQueue(notificacaoQueue());
        admin.declareBinding(notificacaoBinding());

        admin.declareExchange(notificacaoDlqExchange());
        admin.declareQueue(notificacaoDlqQueue());
        admin.declareBinding(notificacaoDlqBinding());

        return admin;
    }

    @Bean
    public DirectExchange notificacaoExchange() {
        return new DirectExchange(NOTIFICACAO_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacaoQueue() {
        return QueueBuilder.durable(NOTIFICACAO_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICACAO_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICACAO_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificacaoBinding() {
        return BindingBuilder.bind(notificacaoQueue())
                .to(notificacaoExchange())
                .with(NOTIFICACAO_ROUTING_KEY);
    }

    @Bean
    public DirectExchange notificacaoDlqExchange() {
        return new DirectExchange(NOTIFICACAO_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacaoDlqQueue() {
        return QueueBuilder.durable(NOTIFICACAO_DLQ_QUEUE).build();
    }

    @Bean
    public Binding notificacaoDlqBinding() {
        return BindingBuilder.bind(notificacaoDlqQueue())
                .to(notificacaoDlqExchange())
                .with(NOTIFICACAO_DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}