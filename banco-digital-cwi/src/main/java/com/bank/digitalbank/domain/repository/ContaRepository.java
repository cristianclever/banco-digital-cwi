package com.bank.digitalbank.domain.repository;

import com.bank.digitalbank.domain.model.Conta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, String> {

    /**
     * Busca uma conta aplicando Lock Pessimista de Escrita (SELECT ... FOR UPDATE).
     * Garante que nenhuma outra transação altere ou leia este registro simultaneamente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id = :id")
    Optional<Conta> findByIdForUpdate(@Param("id") String id);
}