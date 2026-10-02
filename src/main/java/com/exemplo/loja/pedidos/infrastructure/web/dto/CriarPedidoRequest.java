package com.exemplo.loja.pedidos.infrastructure.web.dto;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record CriarPedidoRequest(@NotEmpty List<@NotNull @Valid Item> itens) {
  public record Item(@NotNull UUID produtoId, @Positive int quantidade) {}

  public CriarPedidoUseCase.Comando comando() {
    return new CriarPedidoUseCase.Comando(
        itens.stream()
            .map(i -> new CriarPedidoUseCase.Item(i.produtoId(), i.quantidade()))
            .toList());
  }
}
