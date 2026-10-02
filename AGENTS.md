# Orientação para alterações

- Fluxo de trabalho: seguir `docs/gitflow.md`. Features partem de `develop`; releases promovem para `master`; hotfixes partem de `master` e voltam também a `develop`.
- Para comportamento novo/correção, primeiro commit `test:` com cenário que falha pelo motivo esperado, depois `feat:`/`fix:` com implementação e testes passando. Registrar comandos/resultados RED e GREEN no PR. Preservar os commits usando merge commit, sem squash.
- Vincular branch e PR à issue do Kanban e usar o tipo correspondente: Feature, Bug, Refactor, Test, CI/CD ou Docs.

- Use Java 25 sem preview. Build: `./mvnw verify`; formatar: `./mvnw spotless:apply`.
- Preserve `core.domain` e `core.application` livres de frameworks e infraestrutura. Domínio não depende de application.
- Portas de entrada e seus records são contratos públicos. HTTP, JPA e tracing ficam em infraestrutura.
- Integração entre módulos ocorre apenas em `pedidos.infrastructure.integration`, consumindo `estoque.core.application.port.in`; nunca importar modelos, repositórios ou implementações de outro módulo.
- Use `CriarPedidoService` como referência de coordenação e `PedidosConfiguration` para composição. Apenas a porta decorada é bean. Não expor o serviço puro como bean alternativo.
- Criação de pedido usa `CriarPedidoTransacional` e o mesmo gerenciador transacional da reserva. Não adicionar REQUIRES_NEW.
- Use atualização condicional e ordem por produto demonstradas em Estoque; não fazer read-modify-write desprotegido.
- Cada módulo possui suas tabelas e entidades. Não adicionar relacionamentos ou consultas entre módulos.
- Atualize testes e documentação quando comportamento ou arquitetura mudarem. Integração usa PostgreSQL/Testcontainers e Flyway, nunca H2.
- Crie apenas abstrações necessárias. Sem Kafka, Redis, circuit breaker ou plataformas preventivas. Estoque não tem controller. Não criar pacote api ou internal.
