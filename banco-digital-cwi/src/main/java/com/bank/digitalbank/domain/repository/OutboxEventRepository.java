package com.bank.digitalbank.domain.repository;

import com.bank.digitalbank.domain.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;



public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    /**
     * Busca os registros PENDING aplicando Lock Pessimista e SKIP LOCKED.
     * Passamos o 'Pageable' como argumento para que o Spring Data limite o
     * resultado no banco de dados (gerando o LIMIT 100 no SQL final do Postgres).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // -2 ativa o SKIP LOCKED
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingForProcessing(Pageable pageable);
}