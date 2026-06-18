package com.bank.digitalbank.domain.service;

import com.bank.digitalbank.controller.exception.RecursoNaoEncontradoException;
import com.bank.digitalbank.domain.model.Conta;
import com.bank.digitalbank.domain.repository.ContaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Inicializa o Mockito de forma extremamente rápida sem carregar o Spring
public class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository; // Mocka o acesso ao banco de dados

    @InjectMocks
    private ContaService contaService; // Injeta o mock do repository automaticamente no service

    @Test
    @DisplayName("Deve cadastrar uma nova conta com sucesso utilizando ID informado")
    void deveCadastrarContaComSucesso() {
        // Arrange (Configuração do cenário)
        String idCustomizado = "TEST-CONTA-001";
        String nome = "Scarlett Johansson";
        String email = "scarlett@cinema.com";
        BigDecimal saldoInicial = new BigDecimal("15000.00");
        Conta contaEsperada = new Conta(idCustomizado, nome, email, saldoInicial);

        // Simulamos o comportamento do repository para retornar a conta quando ela for salva
        when(contaRepository.save(any(Conta.class))).thenReturn(contaEsperada);

        // Act (Execução da ação)
        Conta contaCadastrada = contaService.cadastrar(idCustomizado, nome, email, saldoInicial);

        // Assert (Validações)
        assertNotNull(contaCadastrada);
        assertEquals(idCustomizado, contaCadastrada.getId());
        assertEquals(nome, contaCadastrada.getNome());
        assertEquals(email, contaCadastrada.getEmail());
        assertEquals(saldoInicial, contaCadastrada.getSaldo());

        // Garante que o método save do repositório foi chamado exatamente 1 vez
        verify(contaRepository, times(1)).save(any(Conta.class));
    }

    @Test
    @DisplayName("Deve gerar ID automático tipo UUID se nenhum ID for informado no cadastro")
    void deveGerarIdAutomaticoNoCadastro() {
        // Arrange
        String nome = "Robert Downey Jr";
        String email = "robert@cinema.com";
        BigDecimal saldoInicial = new BigDecimal("80000.00");

        // Simulamos o save retornando o próprio objeto passado, que terá o ID gerado pelo service
        when(contaRepository.save(any(Conta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Conta contaCadastrada = contaService.cadastrar(null, nome, email, saldoInicial);

        // Assert
        assertNotNull(contaCadastrada.getId());
        assertFalse(contaCadastrada.getId().isBlank());
        // Garante que o ID automático gerado é um UUID válido
        assertDoesNotThrow(() -> UUID.fromString(contaCadastrada.getId()));

        verify(contaRepository, times(1)).save(any(Conta.class));
    }

    @Test
    @DisplayName("Deve buscar uma conta existente por ID com sucesso")
    void deveBuscarContaPorIdComSucesso() {
        // Arrange
        String id = "CONTA-VALIDA";
        Conta contaExistente = new Conta(id, "Cillian Murphy", "cillian@cinema.com", BigDecimal.TEN);

        when(contaRepository.findById(id)).thenReturn(Optional.of(contaExistente));

        // Act
        Conta contaBuscada = contaService.buscarPorId(id);

        // Assert
        assertNotNull(contaBuscada);
        assertEquals(id, contaBuscada.getId());
        assertEquals("Cillian Murphy", contaBuscada.getNome());

        verify(contaRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException ao buscar conta inexistente")
    void deveLancarErroAoBuscarContaInexistente() {
        // Arrange
        String idFalso = "ID-INEXISTENTE-999";
        when(contaRepository.findById(idFalso)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            contaService.buscarPorId(idFalso);
        });

        verify(contaRepository, times(1)).findById(idFalso);
    }

    @Test
    @DisplayName("Deve listar todas as contas cadastradas")
    void deveListarTodasAsContas() {
        // Arrange
        List<Conta> listaSimulada = List.of(
                new Conta("C-1", "Zendaya", "zendaya@cinema.com", BigDecimal.ZERO),
                new Conta("C-2", "Pedro Pascal", "pedro@cinema.com", BigDecimal.TEN)
        );
        when(contaRepository.findAll()).thenReturn(listaSimulada);

        // Act
        List<Conta> contas = contaService.listarTodas();

        // Assert
        assertNotNull(contas);
        assertEquals(2, contas.size());
        assertEquals("Zendaya", contas.get(0).getNome());

        verify(contaRepository, times(1)).findAll();
    }
}