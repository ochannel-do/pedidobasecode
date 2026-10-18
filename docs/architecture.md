# Arquitetura

Um Maven e um executável. Módulos de negócio são pacotes. `core` contém domínio, portas e casos de uso; infraestrutura implementa persistência, HTTP, transação, composição e observabilidade.

Pedidos recebe HTTP, transforma DTO em comando, valida o agregado, reserva via porta própria e persiste. `EstoqueModuleAdapter` converte contratos e traduz a falha pública `ReservarItensUseCase.Indisponivel` em falha de Pedidos. Estoque só publica sua porta Java, sem HTTP.

Composição: observação `pedidos.criar` envolve decorador TransactionTemplate que envolve serviço puro; reserva observada e persistência observada são filhas. Nenhum core conhece Spring ou tracing. A porta decorada é o único bean de criação.

Propriedade de tabelas: Estoque possui `estoque`; Pedidos possui `pedidos` e `pedido_itens`. O identificador de produto no pedido é um valor sem FK para Estoque. Entidades JPA são separadas e mapeadas explicitamente.

Reserva usa `UPDATE estoque SET disponivel = disponivel - quantidade WHERE produto_id = id AND disponivel >= quantidade`. PostgreSQL serializa updates da mesma linha e reavalia a condição após espera. Linhas afetadas zero significam indisponibilidade. Itens são validados antes de atualização, produtos repetidos são rejeitados e linhas são processadas em ordem de UUID para evitar inversão de locks. Qualquer falha propaga para o decorador, revertendo todo o pedido e suas reservas. O adaptador recusa reserva sem transação ativa.

Reserva e criação estão intencionalmente acopladas à mesma transação PostgreSQL. Uma chamada Java direta de reserva também precisa de transação fornecida pelo chamador. Não há promessa de atomicidade independente do serviço puro, cujo papel é coordenação. A fronteira de infraestrutura garante atomicidade.

ArchUnit verifica core puro, domínio independente, acesso entre módulos, ausência de ciclos e Estoque sem HTTP. Testes reais verificam rollback após escrita parcial e disputa da última unidade, sem transação automática no teste.

Tracing usa Micrometer Observation, OpenTelemetry e OTLP HTTP para Jaeger. Não há payload em atributos. Spans explícitos: `pedidos.criar`, `estoque.reservar`, `pedidos.persistir`. HTTP é instrumentado pelo Spring. Logs correlacionados seguem auto-configuração do Boot. Exportador assíncrono possui timeout limitado; falha do backend não muda o resultado de negócio.

Na infraestrutura web, `HttpRequestLoggingFilter` registra a conclusão HTTP dentro do escopo da observação do Spring, inclusive após exceções resolvidas pelo advice. O advice anota o código de insuficiência no request, para um único evento com status e motivo. MDC de correlação é restaurado em `finally`; JSON de stdout contém os campos de contexto, sem payload ou URL variável. Detalhes e limites: [observabilidade HTTP](http-observability.md).
