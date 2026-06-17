package com.bank.digitalbank.domain.repository;

import com.bank.digitalbank.domain.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, String> {
}