package com.bank.digitalbank.domain.service;

import com.bank.digitalbank.domain.model.Conta;
import com.bank.digitalbank.domain.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;



@Slf4j
@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;

    /**
     * Realiza o cadastro básico de uma nova conta no banco digital.
     * Caso não seja fornecido um ID, gera um UUID automaticamente.
     */
    @Transactional
    public Conta cadastrar(String id, String nome, String email, BigDecimal saldoInicial) {
        log.info("Cadastrando nova conta para o cliente: {}, Email: {}", nome, email);

        String contaId = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;

        if (contaRepository.existsById(contaId)) {
            throw new IllegalArgumentException("Já existe uma conta cadastrada com o ID informado: " + contaId);
        }

        if (saldoInicial == null || saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O saldo inicial não pode ser negativo.");
        }

        Conta novaConta = new Conta(contaId, nome, email, saldoInicial);
        Conta contaSalva = contaRepository.save(novaConta);

        log.info("Conta cadastrada com sucesso! ID: {}", contaSalva.getId());
        return contaSalva;
    }

    /**
     * Consulta o saldo/dados de uma conta sem aplicar locks (para leitura limpa).
     */
    public Conta buscarPorId(String id) {
        return contaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para o ID: " + id));
    }

    /**
     * Lista todas as contas cadastradas na base de dados.
     * Útil para auditoria e acompanhamento dos saldos no painel/Swagger.
     */
    public List<Conta> listarTodas() {
        log.debug("Listando todas as contas registradas.");
        return contaRepository.findAll();
    }
}