package com.bank.digitalbank.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private String id;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    private String payload; // JSON contendo os dados estruturados da notificação

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }

    // Construtor customizado para garantir que todo evento nasça como 'PENDING' e com timestamp correto
    public OutboxEvent(String id, String aggregateType, String aggregateId, String payload) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Métodos de alteração de estado explícitos para manter a semântica rica do domínio
    public void marcarComoProcessado() {
        this.status = OutboxStatus.PROCESSED;
    }

    public void marcarComoFalhado() {
        this.status = OutboxStatus.FAILED;
    }
}