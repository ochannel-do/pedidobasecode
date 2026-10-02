package com.exemplo.loja.pedidos.infrastructure.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

class CriarPedidoTransacionalTest {
  @Test
  void executaDentroDaTransacaoRequiredEConfirmaAposSucesso() {
    var manager = mock(PlatformTransactionManager.class);
    var status = new SimpleTransactionStatus();
    when(manager.getTransaction(any())).thenReturn(status);
    var delegate = mock(CriarPedidoUseCase.class);
    var comando =
        new CriarPedidoUseCase.Comando(List.of(new CriarPedidoUseCase.Item(UUID.randomUUID(), 1)));
    var esperado = new CriarPedidoUseCase.Resultado(UUID.randomUUID(), "CRIADO");
    when(delegate.executar(comando)).thenReturn(esperado);
    assertThat(
            new CriarPedidoTransacional(delegate, new TransactionTemplate(manager))
                .executar(comando))
        .isSameAs(esperado);
    var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
    var ordem = inOrder(manager, delegate);
    ordem.verify(manager).getTransaction(definition.capture());
    ordem.verify(delegate).executar(comando);
    ordem.verify(manager).commit(status);
    assertThat(definition.getValue().getPropagationBehavior())
        .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    verify(manager, never()).rollback(any());
  }

  @Test
  void falhaDoCasoDeUsoFazRollbackSemCommit() {
    var manager = mock(PlatformTransactionManager.class);
    var status = new SimpleTransactionStatus();
    when(manager.getTransaction(any())).thenReturn(status);
    var delegate = mock(CriarPedidoUseCase.class);
    var comando = new CriarPedidoUseCase.Comando(List.of());
    var falha = new IllegalArgumentException("pedido inválido");
    when(delegate.executar(comando)).thenThrow(falha);
    assertThatThrownBy(
            () ->
                new CriarPedidoTransacional(delegate, new TransactionTemplate(manager))
                    .executar(comando))
        .isSameAs(falha);
    var ordem = inOrder(manager, delegate);
    ordem.verify(manager).getTransaction(any());
    ordem.verify(delegate).executar(comando);
    ordem.verify(manager).rollback(status);
    verify(manager, never()).commit(any());
  }
}
