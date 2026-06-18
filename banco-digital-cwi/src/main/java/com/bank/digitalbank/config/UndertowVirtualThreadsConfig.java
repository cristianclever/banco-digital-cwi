package com.bank.digitalbank.config;

import io.undertow.servlet.api.DeploymentInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuração customizada para forçar o servidor web Undertow a utilizar
 * Virtual Threads (Project Loom) no processamento das requisições HTTP.
 * * Esta classe só será ativada se a propriedade 'spring.threads.virtual.enabled' estiver como 'true'.
 */
@Configuration
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
public class UndertowVirtualThreadsConfig {

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowVirtualThreadsCustomizer() {
        return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            // Cria o executor de Threads Virtuais nativo do Java 21
            ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

            // Define que o Undertow deve despachar as requisições servlet (blocking tasks) usando Threads Virtuais
            deploymentInfo.setExecutor(virtualThreadExecutor);
            deploymentInfo.setAsyncExecutor(virtualThreadExecutor);
        });
    }
}