package com.bank.digitalbank.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                // Espera inicial de 3 segundos com dobra a cada tentativa (Exponential Backoff)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(4), 4.0))
                // Lista de exceções que disparam o Retry (ajuste conforme a exceção do SDK)
                .retryExceptions(Exception.class)
                .build();
        return RetryRegistry.of(config);
    }


    /**
     * Configuração do Circuit Breaker:
     * - Se 50% das chamadas falharem (em uma janela de 10 chamadas), o circuito abre.
     * - O circuito fica aberto por 30 segundos (tempo para a API 'respirar').
     * - No estado 'Meio Aberto', permite 3 chamadas para testar a saúde do serviço.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Limite de falha de 50%
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Tempo em repouso
                .slidingWindowSize(10) // Janela de análise
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(Exception.class)
                .build();
        return CircuitBreakerRegistry.of(config);
    }


    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                // Define o número de execuções permitidas
                .limitForPeriod(1)
                // Define o intervalo de tempo para renovar o limite
                .limitRefreshPeriod(Duration.ofMinutes(1))
                // Tempo que uma thread espera para tentar entrar no limite antes de dar erro
                .timeoutDuration(Duration.ofSeconds(3))
                .build();
        return RateLimiterRegistry.of(config);
    }
}
