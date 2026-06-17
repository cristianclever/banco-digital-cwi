package com.bank.digitalbank.controller;

import com.bank.digitalbank.controller.dto.TransferenciaRequest;
import com.bank.digitalbank.domain.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferências", description = "Endpoints para execução de transações financeiras")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @PostMapping
    @Operation(summary = "Executa uma transferência entre contas", description = "Processa o débito/crédito atomicamente e integra com o Transactional Outbox")
    public ResponseEntity<Map<String, String>> transferir(@RequestBody TransferenciaRequest request) {
        transferenciaService.transferir(
                request.contaOrigemId(),
                request.contaDestinoId(),
                request.valor()
        );

        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "message", "Transferência processada e enviada para a fila de notificações."
        ));
    }
}