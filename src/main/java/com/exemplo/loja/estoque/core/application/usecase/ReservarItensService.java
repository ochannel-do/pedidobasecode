package com.exemplo.loja.estoque.core.application.usecase;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.estoque.core.application.port.out.EstoqueRepository;
import com.exemplo.loja.estoque.core.domain.model.ItemEstoque;
import java.util.Comparator;
import java.util.List;

public final class ReservarItensService implements ReservarItensUseCase {
  private final EstoqueRepository repository;

  public ReservarItensService(EstoqueRepository repository) {
    this.repository = repository;
  }

  public void executar(List<Item> itens) {
    if (itens == null || itens.isEmpty() || itens.stream().anyMatch(java.util.Objects::isNull))
      throw new IllegalArgumentException("Itens obrigatórios");
    var validos =
        itens.stream()
            .map(i -> new ItemEstoque(i.produtoId(), i.quantidade()))
            .sorted(Comparator.comparing(ItemEstoque::produtoId))
            .toList();
    if (validos.stream().map(ItemEstoque::produtoId).distinct().count() != validos.size())
      throw new IllegalArgumentException("Produtos duplicados");
    for (var item : validos)
      if (!repository.reservar(item.produtoId(), item.quantidade())) throw new Indisponivel();
  }
}
