package com.exemplo.loja.estoque.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EstoqueRepositoryAdapterTest {
  @AfterEach
  void limpaEstadoTransacional() {
    TransactionSynchronizationManager.clear();
  }

  @Test
  void rejeitaReservaForaDeTransacaoSemAlterarBanco() {
    var repository = mock(EstoqueJpaRepository.class);
    assertThatThrownBy(
            () -> new EstoqueRepositoryAdapter(repository).reservar(UUID.randomUUID(), 2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Reserva exige transação ativa");
    verifyNoInteractions(repository);
  }

  @Test
  void retornaSucessoSomenteQuandoUmaLinhaFoiAtualizada() {
    var repository = mock(EstoqueJpaRepository.class);
    var id = UUID.randomUUID();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    when(repository.reservar(id, 3)).thenReturn(1, 0, 2);
    var adapter = new EstoqueRepositoryAdapter(repository);
    assertThat(adapter.reservar(id, 3)).isTrue();
    assertThat(adapter.reservar(id, 3)).isFalse();
    assertThat(adapter.reservar(id, 3)).isFalse();
    verify(repository, times(3)).reservar(id, 3);
  }

  @Test
  void propagaFalhaDePersistencia() {
    var repository = mock(EstoqueJpaRepository.class);
    var id = UUID.randomUUID();
    var falha = new IllegalStateException("falha SQL");
    TransactionSynchronizationManager.setActualTransactionActive(true);
    when(repository.reservar(id, 1)).thenThrow(falha);
    assertThatThrownBy(() -> new EstoqueRepositoryAdapter(repository).reservar(id, 1))
        .isSameAs(falha);
  }
}
