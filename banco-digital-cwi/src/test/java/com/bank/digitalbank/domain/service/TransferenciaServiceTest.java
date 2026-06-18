package com.bank.digitalbank.domain.service;

import com.bank.digitalbank.domain.model.Conta;
import com.bank.digitalbank.domain.model.Movimentacao;
import com.bank.digitalbank.domain.model.OutboxEvent;
import com.bank.digitalbank.domain.repository.ContaRepository;
import com.bank.digitalbank.domain.repository.MovimentacaoRepository;
import com.bank.digitalbank.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários da Classe TransferenciaService")
public class TransferenciaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransferenciaService transferenciaService;

    // ========================== TESTES DE SUCESSO ==========================

    @Test
    @DisplayName("Deve realizar transferência com sucesso quando contas e saldo são válidos")
    void deveRealizarTransferenciaComSucesso() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal saldoOrigemAntes = new BigDecimal("500.00");
        BigDecimal saldoDestinoAntes = new BigDecimal("200.00");

        Conta contaOrigem = new Conta(idOrigem, "João Silva", "joao@email.com", saldoOrigemAntes);
        Conta contaDestino = new Conta(idDestino, "Maria Santos", "maria@email.com", saldoDestinoAntes);

        // Mock das buscas com lock na ordem correta (CONTA-001 < CONTA-002)
        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));

        // Mock do ObjectMapper para serializar o payload
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{\"movimentacaoId\": \"test-id\"}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert
        // Valida que os saldos foram alterados corretamente
        assertEquals(new BigDecimal("400.00"), contaOrigem.getSaldo()); // 500 - 100
        assertEquals(new BigDecimal("300.00"), contaDestino.getSaldo()); // 200 + 100

        // Verifica que o repository salvou as contas
        verify(contaRepository, times(2)).save(any(Conta.class));
        // Verifica que a movimentação foi registrada
        verify(movimentacaoRepository, times(1)).save(any(Movimentacao.class));
        // Verifica que o evento foi inserido no outbox
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Deve realizar transferência respeitando a ordem de locks ao comparar IDs (origem maior que destino)")
    void deveRespeitarOrdemDeLockQuandoOrigemMaiorQueDestino() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-002"; // Maior que destino
        String idDestino = "CONTA-001"; // Menor que origem
        BigDecimal valor = new BigDecimal("50.00");

        Conta contaOrigem = new Conta(idOrigem, "Alice", "alice@email.com", new BigDecimal("300.00"));
        Conta contaDestino = new Conta(idDestino, "Bob", "bob@email.com", new BigDecimal("100.00"));

        // Mock - deve buscar destino primeiro (menor ID) para evitar deadlock
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));
        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert
        // Verifica que findByIdForUpdate foi chamado com os IDs (ordem é implícita na execução)
        verify(contaRepository, times(2)).findByIdForUpdate(anyString());
        assertEquals(new BigDecimal("250.00"), contaOrigem.getSaldo()); // 300 - 50
        assertEquals(new BigDecimal("150.00"), contaDestino.getSaldo()); // 100 + 50
    }

    @Test
    @DisplayName("Deve criar evento no Outbox com payload correto contendo email do destinatário")
    void deveInserirEventoOutboxComEmailDoDestinatario() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-A";
        String idDestino = "CONTA-B";
        BigDecimal valor = new BigDecimal("200.00");
        String emailDestinatario = "destinatario@email.com";

        Conta contaOrigem = new Conta(idOrigem, "Origem", "origem@email.com", new BigDecimal("1000.00"));
        Conta contaDestino = new Conta(idDestino, "Destino", emailDestinatario, new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{\"email\": \"" + emailDestinatario + "\"}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert - Captura o OutboxEvent salvo para validar
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent eventoSalvo = outboxCaptor.getValue();
        assertNotNull(eventoSalvo);
        assertEquals("TRANSFERENCIA", eventoSalvo.getAggregateType());
        assertEquals(OutboxEvent.OutboxStatus.PENDING, eventoSalvo.getStatus());
        assertNotNull(eventoSalvo.getId());
        assertNotNull(eventoSalvo.getCreatedAt());
    }

    @Test
    @DisplayName("Deve registrar movimentação com ID único gerado para cada transferência")
    void deveRegistrarMovimentacaoComIdUnico() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("150.00");

        Conta contaOrigem = new Conta(idOrigem, "Pessoa A", "a@email.com", new BigDecimal("1000.00"));
        Conta contaDestino = new Conta(idDestino, "Pessoa B", "b@email.com", new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert
        ArgumentCaptor<Movimentacao> movCaptor = ArgumentCaptor.forClass(Movimentacao.class);
        verify(movimentacaoRepository).save(movCaptor.capture());

        Movimentacao movimentacao = movCaptor.getValue();
        assertNotNull(movimentacao.getId());
        assertEquals(idOrigem, movimentacao.getContaOrigemId());
        assertEquals(idDestino, movimentacao.getContaDestinoId());
        assertEquals(valor, movimentacao.getValor());
        assertNotNull(movimentacao.getDataCriacao());
    }

    // ========================== TESTES DE ERRO: VALIDAÇÃO BÁSICA ==========================

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar transferir para a mesma conta")
    void deveLancarErroAoTransferirParaMesmaConta() {
        // Arrange
        String mesmaContaId = "CONTA-001";
        BigDecimal valor = new BigDecimal("100.00");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(mesmaContaId, mesmaContaId, valor));

        // Verifica que nenhum repositório foi acessado
        verify(contaRepository, never()).findByIdForUpdate(anyString());
        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // ========================== TESTES DE ERRO: CONTAS NÃO ENCONTRADAS ==========================

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando conta de origem não é encontrada")
    void deveLancarErroQuandoContaOrigemNaoEncontrada() {
        // Arrange
        String idOrigem = "CONTA-AAAA"; // Menor que destino
        String idDestino = "CONTA-BBBB";
        BigDecimal valor = new BigDecimal("100.00");

        // Mock para retornar sucesso em destino, falha em origem
        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        verify(contaRepository, times(1)).findByIdForUpdate(idOrigem);
        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando conta de destino não é encontrada")
    void deveLancarErroQuandoContaDestinoNaoEncontrada() {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-INEXISTENTE";
        BigDecimal valor = new BigDecimal("100.00");

        Conta contaOrigem = new Conta(idOrigem, "Origem", "origem@email.com", new BigDecimal("1000.00"));

        // Primeira chamada (Origem) retorna sucesso, segunda (Destino) retorna vazio
        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        verify(contaRepository, times(2)).findByIdForUpdate(anyString());
        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // ========================== TESTES DE ERRO: VALIDAÇÃO DE SALDO ==========================

    @Test
    @DisplayName("Deve lançar IllegalStateException quando conta de origem tem saldo insuficiente")
    void deveLancarErroQuandoSaldoInsuficiente() {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("1000.00");
        BigDecimal saldoInsuficiente = new BigDecimal("100.00");

        Conta contaOrigem = new Conta(idOrigem, "João", "joao@email.com", saldoInsuficiente);
        Conta contaDestino = new Conta(idDestino, "Maria", "maria@email.com", new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        // Verifica que o saldo original não foi alterado
        assertEquals(saldoInsuficiente, contaOrigem.getSaldo());
        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando transferência com valor igual a zero")
    void deveLancarErroComValorZero() {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = BigDecimal.ZERO;

        Conta contaOrigem = new Conta(idOrigem, "João", "joao@email.com", new BigDecimal("1000.00"));
        Conta contaDestino = new Conta(idDestino, "Maria", "maria@email.com", new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando transferência com valor negativo")
    void deveLancarErroComValorNegativo() {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("-50.00");

        Conta contaOrigem = new Conta(idOrigem, "João", "joao@email.com", new BigDecimal("1000.00"));
        Conta contaDestino = new Conta(idDestino, "Maria", "maria@email.com", new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        verify(movimentacaoRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // ========================== TESTES DE ERRO: SERIALIZAÇÃO ==========================

    @Test
    @DisplayName("Deve lançar IllegalStateException quando falha ao serializar o payload do Outbox")
    void deveLancarErroAoFalharSerializacao() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("100.00");

        Conta contaOrigem = new Conta(idOrigem, "João", "joao@email.com", new BigDecimal("1000.00"));
        Conta contaDestino = new Conta(idDestino, "Maria", "maria@email.com", new BigDecimal("500.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));

        // Mock da falha na serialização
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenThrow(new RuntimeException("Erro de serialização"));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transferenciaService.transferir(idOrigem, idDestino, valor));

        verify(outboxEventRepository, never()).save(any());
    }

    // ========================== TESTES DE INTEGRAÇÃO E FLUXO COMPLETO ==========================

    @Test
    @DisplayName("Deve completar fluxo de transferência com múltiplas validações e integrações")
    void deveCompletarFluxoCompleto() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-A";
        String idDestino = "CONTA-B";
        BigDecimal valor = new BigDecimal("250.50");

        Conta contaOrigem = new Conta(idOrigem, "Remetente", "remetente@email.com", new BigDecimal("5000.00"));
        Conta contaDestino = new Conta(idDestino, "Beneficiário", "beneficiario@email.com", new BigDecimal("1000.00"));

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{\"movimentacaoId\": \"mov-id\", \"valor\": 250.50}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert - Validações Completas
        assertEquals(new BigDecimal("4749.50"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("1250.50"), contaDestino.getSaldo());

        // Verifica que save foi chamado para ambas as contas
        verify(contaRepository, times(2)).save(any(Conta.class));

        // Verifica que a movimentação foi registrada
        ArgumentCaptor<Movimentacao> movCaptor = ArgumentCaptor.forClass(Movimentacao.class);
        verify(movimentacaoRepository).save(movCaptor.capture());
        Movimentacao mov = movCaptor.getValue();
        assertEquals(idOrigem, mov.getContaOrigemId());
        assertEquals(idDestino, mov.getContaDestinoId());
        assertEquals(valor, mov.getValor());

        // Verifica que o evento foi criado com status PENDING
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent evento = outboxCaptor.getValue();
        assertEquals(OutboxEvent.OutboxStatus.PENDING, evento.getStatus());
        assertEquals("TRANSFERENCIA", evento.getAggregateType());
    }

    @Test
    @DisplayName("Deve fazer transferência com valores em precisão decimal alta")
    void deveTransferirComPrecisaoDecimal() throws JsonProcessingException {
        // Arrange
        String idOrigem = "CONTA-001";
        String idDestino = "CONTA-002";
        BigDecimal valor = new BigDecimal("99.99");

        Conta contaOrigem = new Conta(idOrigem, "João", "joao@email.com", new BigDecimal("999.99"));
        Conta contaDestino = new Conta(idDestino, "Maria", "maria@email.com", BigDecimal.ZERO);

        when(contaRepository.findByIdForUpdate(idOrigem)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdForUpdate(idDestino)).thenReturn(Optional.of(contaDestino));
        when(objectMapper.writeValueAsString(any(TransferenciaEventPayload.class)))
                .thenReturn("{}");

        // Act
        transferenciaService.transferir(idOrigem, idDestino, valor);

        // Assert
        assertEquals(new BigDecimal("900.00"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("99.99"), contaDestino.getSaldo());
    }
}


