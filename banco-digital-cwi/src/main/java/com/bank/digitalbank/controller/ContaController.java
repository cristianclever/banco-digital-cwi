package com.bank.digitalbank.controller;

import com.bank.digitalbank.controller.dto.ContaRequest;
import com.bank.digitalbank.domain.model.Conta;
import com.bank.digitalbank.domain.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/contas")
@RequiredArgsConstructor
@Tag(name = "Contas", description = "Endpoints para gerenciamento de contas bancárias")
public class ContaController {

    private final ContaService contaService;

    @PostMapping
    @Operation(summary = "Cadastra uma nova conta", description = "Permite a criação de uma conta com saldo inicial")
    public ResponseEntity<Conta> cadastrar(@RequestBody ContaRequest request) {
        Conta novaConta = contaService.cadastrar(
                request.id(),
                request.nome(),
                request.email(),
                request.saldoInicial()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(novaConta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma conta por ID", description = "Retorna os dados da conta. Lança 422 se não existir")
    public ResponseEntity<Conta> buscarPorId(@PathVariable String id) {
        Conta conta = contaService.buscarPorId(id);
        return ResponseEntity.ok(conta);
    }

    @GetMapping
    @Operation(summary = "Lista todas as contas", description = "Retorna o estado atual de todas as contas para auditoria")
    public ResponseEntity<List<Conta>> listarTodas() {
        return ResponseEntity.ok(contaService.listarTodas());
    }
}