package com.exemplo.loja.pedidos.infrastructure.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.pedidos.core.application.port.out.ReservarEstoque;
import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstoqueModuleAdapterTest {
  @Test
  void encaminhaTodosOsProdutosEQuantidades() {
    var estoque = mock(ReservarItensUseCase.class);
    var primeiro = UUID.randomUUID();
    var segundo = UUID.randomUUID();
    new EstoqueModuleAdapter(estoque)
        .reservar(
            List.of(new ReservarEstoque.Item(primeiro, 2), new ReservarEstoque.Item(segundo, 7)));
    verify(estoque)
        .executar(
            List.of(
                new ReservarItensUseCase.Item(primeiro, 2),
                new ReservarItensUseCase.Item(segundo, 7)));
    verifyNoMoreInteractions(estoque);
  }

  @Test
  void traduzSomenteIndisponibilidadeDeEstoque() {
    var estoque = mock(ReservarItensUseCase.class);
    doThrow(new ReservarItensUseCase.Indisponivel()).when(estoque).executar(anyList());
    assertThatThrownBy(
            () ->
                new EstoqueModuleAdapter(estoque)
                    .reservar(List.of(new ReservarEstoque.Item(UUID.randomUUID(), 1))))
        .isInstanceOf(EstoqueInsuficienteException.class)
        .hasMessage("Estoque insuficiente para os itens solicitados");
  }

  @Test
  void preservaFalhasTecnicasSemTraduzirParaFaltaDeEstoque() {
    var estoque = mock(ReservarItensUseCase.class);
    var falha = new IllegalStateException("banco indisponível");
    doThrow(falha).when(estoque).executar(anyList());
    assertThatThrownBy(
            () ->
                new EstoqueModuleAdapter(estoque)
                    .reservar(List.of(new ReservarEstoque.Item(UUID.randomUUID(), 1))))
        .isSameAs(falha);
  }
}
