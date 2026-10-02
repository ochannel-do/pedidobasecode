package com.exemplo.loja.pedidos.core.domain.exception;

public class EstoqueInsuficienteException extends RuntimeException {
  public EstoqueInsuficienteException() {
    super("Estoque insuficiente para os itens solicitados");
  }
}
