package com.bank.digitalbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao")
@Getter
@Setter
@NoArgsConstructor
public class Movimentacao {

    @Id
    private String id;

    @Column(name = "conta_origem_id")
    private String contaOrigemId;

    @Column(name = "conta_destino_id")
    private String contaDestinoId;

    private BigDecimal valor;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    public Movimentacao(String id, String contaOrigemId, String contaDestinoId, BigDecimal valor) {
        this.id = id;
        this.contaOrigemId = contaOrigemId;
        this.contaDestinoId = contaDestinoId;
        this.valor = valor;
        this.dataCriacao = LocalDateTime.now();
    }
}