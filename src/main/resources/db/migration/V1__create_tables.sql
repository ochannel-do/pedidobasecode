-- Owner: Estoque
CREATE TABLE estoque (produto_id UUID PRIMARY KEY, disponivel INTEGER NOT NULL CHECK (disponivel >= 0));
-- Owner: Pedidos. produto_id is a value, not a cross-module foreign key.
CREATE TABLE pedidos (id UUID PRIMARY KEY);
CREATE TABLE pedido_itens (pedido_id UUID NOT NULL REFERENCES pedidos(id), produto_id UUID NOT NULL, quantidade INTEGER NOT NULL CHECK (quantidade > 0), PRIMARY KEY (pedido_id, produto_id));
