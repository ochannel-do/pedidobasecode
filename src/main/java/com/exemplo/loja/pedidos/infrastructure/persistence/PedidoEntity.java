package com.exemplo.loja.pedidos.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {
  @Id UUID id;

  @ElementCollection
  @CollectionTable(name = "pedido_itens", joinColumns = @JoinColumn(name = "pedido_id"))
  List<ItemEntity> itens = new ArrayList<>();

  protected PedidoEntity() {}

  PedidoEntity(UUID id, List<ItemEntity> itens) {
    this.id = id;
    this.itens = itens;
  }

  @Embeddable
  public static class ItemEntity {
    @Column(name = "produto_id", nullable = false)
    UUID produtoId;

    @Column(nullable = false)
    int quantidade;

    protected ItemEntity() {}

    ItemEntity(UUID produtoId, int quantidade) {
      this.produtoId = produtoId;
      this.quantidade = quantidade;
    }
  }
}
