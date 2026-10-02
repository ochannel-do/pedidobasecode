package com.exemplo.loja.estoque.infrastructure.persistence;

import com.exemplo.loja.estoque.core.application.port.out.EstoqueRepository;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class EstoqueRepositoryAdapter implements EstoqueRepository {
  private final EstoqueJpaRepository repository;

  public EstoqueRepositoryAdapter(EstoqueJpaRepository repository) {
    this.repository = repository;
  }

  public boolean reservar(UUID id, int quantidade) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Reserva exige transação ativa");
    return repository.reservar(id, quantidade) == 1;
  }
}
