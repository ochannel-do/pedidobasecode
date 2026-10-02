# Especificação — Base de back-end Java

## 1. Objetivo

Criar um projeto executável de referência para novos back-ends e para orientar implementações feitas por IA. A base deve demonstrar Clean Architecture em um monólito modular, com um fluxo de negócio completo, infraestrutura local e testes que garantam as decisões arquiteturais.

Este documento consolida uma discussão de arquitetura. O código ainda deve ser implementado. Exemplos de nomes e contratos podem ser refinados sem alterar os limites definidos aqui.

## 2. Stack e escopo

- Java 25 LTS, sem recursos preview.
- Spring Boot 4.1.x, com versão estável de correção fixada no início da implementação. Validar disponibilidade e compatibilidade das dependências nesse momento.
- Maven, com Maven Wrapper.
- Spring MVC para HTTP e Spring Data JPA na infraestrutura.
- PostgreSQL e Flyway.
- JUnit, mocks quando necessários, ArchUnit e Testcontainers com PostgreSQL.
- Actuator, Micrometer Tracing, OpenTelemetry e exportação OTLP.
- Jaeger para receber e visualizar traces localmente.
- Docker Compose para serviços locais; Dockerfile para a aplicação.
- CI executando as verificações do projeto.

Usar o gerenciamento de dependências do Spring Boot sempre que disponível. Manter um único projeto Maven e um único artefato executável inicialmente. Módulos de negócio são divisões do código, não serviços separados nem necessariamente módulos Maven.

### Fora do escopo inicial

- Kafka e outras soluções de mensageria.
- Circuit breaker e integrações remotas criadas apenas para demonstrá-lo.
- Redis e cache distribuído.
- Microserviços, sagas, outbox e consistência eventual.
- Endpoints HTTP no módulo Estoque.
- Plataforma completa de logs e métricas, Kubernetes e infraestrutura de produção.
- Autenticação e autorização: não foram definidas nesta discussão e devem ser decididas no projeto consumidor antes de uma implantação que as exija.

Essas capacidades podem ser adicionadas quando houver necessidade concreta. Não adicionar dependências preventivamente.

## 3. Módulos e fluxo de negócio

### Pedidos

Recebe solicitações HTTP, valida e cria pedidos, solicita reserva de estoque e persiste seus próprios dados.

### Estoque

Controla disponibilidade e reserva de itens. É acessado por chamadas Java através de uma porta de entrada. Não possui controller HTTP neste exemplo.

### Fluxo principal

1. Cliente envia uma solicitação de criação de pedido.
2. A camada web converte a requisição em um comando do core de Pedidos.
3. O caso de uso valida o pedido e solicita reserva por sua porta de saída `ReservarEstoque`.
4. `EstoqueModuleAdapter`, pertencente à infraestrutura de Pedidos, traduz a chamada para a porta de entrada `ReservarItensUseCase` de Estoque.
5. `ReservarItensService` verifica e reserva os itens por meio de `EstoqueRepository`.
6. Pedidos persiste o pedido por meio de `PedidoRepository`.
7. A camada web retorna o identificador e o resultado da criação.

Se houver estoque insuficiente, a operação falha com resposta de negócio padronizada. Se qualquer etapa transacional falhar, nenhuma reserva ou pedido parcial pode permanecer persistido.

O pedido deve admitir uma coleção de itens. Os contratos definitivos devem representar esse cenário; assinaturas com apenas um produto apresentadas durante a discussão eram simplificações didáticas.

## 4. Organização de pacotes

`core` é composto por `domain` e `application`. Estrutura de referência, criando subpacotes apenas quando houver classes concretas:

```text
src/main/java/com/exemplo/loja/
├── LojaApplication.java
├── pedidos/
│   ├── core/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   └── exception/
│   │   └── application/
│   │       ├── port/
│   │       │   ├── in/
│   │       │   └── out/
│   │       └── usecase/
│   └── infrastructure/
│       ├── web/
│       │   └── dto/
│       ├── persistence/
│       ├── integration/
│       ├── transaction/
│       ├── observability/
│       └── config/
└── estoque/
    ├── core/
    │   ├── domain/
    │   │   ├── model/
    │   │   └── exception/
    │   └── application/
    │       ├── port/
    │       │   ├── in/
    │       │   └── out/
    │       └── usecase/
    └── infrastructure/
        ├── persistence/
        ├── observability/
        └── config/
```

Não criar o pacote `api` discutido inicialmente. Os contratos entre módulos ficam nas portas de entrada e em seus tipos de dados. Também não usar o invólucro `internal` da proposta inicial: a estrutura escolhida expõe `core` e `infrastructure` diretamente sob cada módulo.

### Responsabilidades

| Parte | Responsabilidade |
|---|---|
| `core.domain` | Modelos, invariantes e regras de negócio |
| `core.application.port.in` | Interfaces de entrada, comandos e resultados consumíveis pelos chamadores |
| `core.application.port.out` | Interfaces e tipos que expressam necessidades externas do módulo |
| `core.application.usecase` | Implementação e coordenação dos casos de uso |
| `infrastructure.web` | HTTP, DTOs, validação de transporte e tradução de erros |
| `infrastructure.persistence` | Entidades JPA, Spring Data, adaptadores e mapeamento de persistência |
| `infrastructure.integration` | Adaptadores consumidores de contratos de outros módulos |
| `infrastructure.transaction` | Limites transacionais implementados com Spring |
| `infrastructure.observability` | Instrumentação dos fluxos por decoradores/adaptadores |
| `infrastructure.config` | Criação de beans e composição das implementações |

## 5. Regras de dependência

1. O core não depende de Spring, JPA, HTTP, Micrometer, OpenTelemetry ou infraestrutura.
2. O domínio não depende da camada application.
3. Application depende do domínio e das próprias portas.
4. Infraestrutura depende das abstrações do core e implementa suas portas de saída.
5. Um módulo só acessa outro por um adaptador de integração, consumindo portas de entrada e seus tipos públicos de contrato.
6. O core de Pedidos não importa classes de Estoque.
7. Não acessar modelos internos, casos de uso concretos, entidades JPA ou repositórios de outro módulo.
8. Não compartilhar entidades de negócio ou entidades JPA entre módulos.
9. Cada módulo acessa apenas suas tabelas. Evitar consultas e relacionamentos JPA que atravessem essa fronteira.
10. Não permitir dependências cíclicas entre módulos.

Implementar essas regras em testes ArchUnit. Visibilidade `public` em Java não equivale a autorização arquitetural de consumo.

## 6. Portas e integração

| Contrato | Local | Implementação |
|---|---|---|
| `CriarPedidoUseCase` | Pedidos, `core.application.port.in` | `CriarPedidoService` |
| `ReservarEstoque` | Pedidos, `core.application.port.out` | `EstoqueModuleAdapter`, na infraestrutura de Pedidos |
| `PedidoRepository` | Pedidos, `core.application.port.out` | `PedidoRepositoryAdapter` |
| `ReservarItensUseCase` | Estoque, `core.application.port.in` | `ReservarItensService` |
| `EstoqueRepository` | Estoque, `core.application.port.out` | `EstoqueRepositoryAdapter` |

```text
CriarPedidoService
    → ReservarEstoque
    → EstoqueModuleAdapter
    → ReservarItensUseCase
    → ReservarItensService
```

O adaptador de integração traduz comandos, resultados e falhas esperadas entre os contratos dos dois módulos. Os tipos de falha que atravessam a fronteira devem fazer parte do contrato público da porta, sem exigir que o consumidor importe exceções internas do domínio de Estoque.

Comandos e resultados podem ser records Java. Não expor `ResponseEntity`, entidades JPA ou tipos Spring Data nos contratos do core. DTO HTTP, contrato de aplicação, modelo de domínio e entidade de persistência possuem responsabilidades distintas.

## 7. Transações, persistência e concorrência

- Usar o mesmo PostgreSQL e o mesmo gerenciador de transações para os dois módulos.
- Envolver a criação completa do pedido em uma única transação: reserva e persistência são atômicas.
- Implementar a fronteira transacional em um decorador de infraestrutura que implemente `CriarPedidoUseCase`, usando `TransactionTemplate`.
- Compor os beans explicitamente para evitar ambiguidades entre serviço e decoradores e impedir chamadas acidentais que contornem a transação.
- A reserva participa da transação existente; não usar uma transação independente para confirmá-la antecipadamente.
- O acoplamento transacional entre os módulos é uma decisão intencional desta versão do monólito e deve ser documentado.
- Usar entidades JPA separadas dos modelos de domínio e mapeamento explícito.
- Versionar o banco com Flyway. Não depender da geração automática de tabelas pelo Hibernate; usar validação do schema.
- Cada tabela deve ter proprietário de módulo documentado.

Implementar uma estratégia concreta contra reserva concorrente, como atualização condicional atômica ou controle de locking adequadamente tratado. Documentar a escolha. Não basta ler o saldo e depois gravar um novo valor sem proteção.

Critério obrigatório: duas transações independentes disputando a última unidade não podem ambas obter sucesso. Reservas com múltiplos itens também devem ser revertidas integralmente quando um item falhar.

## 8. HTTP e erros

- Expor criação de pedido em `POST /pedidos` e documentar o contrato via OpenAPI.
- Validar formato e presença de campos na camada web.
- Garantir invariantes no core mesmo em chamadas Java diretas.
- Padronizar respostas de erro com `ProblemDetail` e códigos estáveis, por exemplo `ESTOQUE_INSUFICIENTE`.
- Centralizar o mapeamento HTTP das falhas relevantes e não expor detalhes internos ou stack traces na resposta.
- Diferenciar entrada inválida, falha de negócio e erro inesperado.
- Documentar exemplos de sucesso e erro e os respectivos status HTTP.

Não gerar endpoints CRUD genéricos em todos os módulos. Implementar os necessários ao fluxo de referência. O modo de fornecer estoque inicial para demonstração deve ser documentado e restrito a desenvolvimento/testes.

## 9. Testes

Manter cinco categorias de testes. Testcontainers é uma ferramenta dos testes de integração, não uma sexta categoria.

| Categoria | Cobertura | Ambiente |
|---|---|---|
| Unitários de domínio | Invariantes e regras de Pedido/Estoque | Sem Spring e sem banco |
| Unitários de caso de uso | Coordenação, resultados e falhas usando portas simuladas | Sem Spring e sem banco |
| Integração | Adaptadores de persistência, módulos, rollback, concorrência e tracing | Spring e PostgreSQL via Testcontainers quando necessário |
| HTTP | Contratos, validação, status e erros | Camada web; caso de uso pode ser simulado |
| Arquitetura | Limites de camadas e módulos | ArchUnit, sem subir aplicação ou banco |

```text
src/test/java/com/exemplo/loja/
├── support/
│   └── PostgresTestConfiguration.java
├── architecture/
│   └── ArchitectureTest.java
├── pedidos/
└── estoque/
```

### Cenários mínimos

1. Pedido válido é criado e o estoque é reservado.
2. Entrada ou quantidade inválida é rejeitada.
3. Estoque insuficiente não deixa pedido ou reserva parcial.
4. Falha na persistência do pedido desfaz a reserva.
5. Reservas concorrentes não consomem a mesma unidade.
6. Migrations funcionam em PostgreSQL novo.
7. Respostas HTTP seguem os contratos documentados.
8. Spans das operações de Pedidos e Estoque possuem o mesmo trace e a hierarquia esperada.
9. Regras arquiteturais passam no build.

Testes de rollback e concorrência devem observar commits/rollbacks reais em transações independentes quando aplicável. Evitar que uma transação automática do teste oculte a fronteira transacional da aplicação.

Usar PostgreSQL real via Testcontainers, sem substituí-lo por H2 nos testes de integração. Executar Flyway nesses testes. O ambiente de desenvolvimento e CI precisa de Docker compatível. Não exigir Jaeger externo nos testes automatizados de spans: usar captura/exportação em memória e habilitar a instrumentação necessária no contexto de teste.

## 10. Tracing e diagnóstico

Arquitetura escolhida:

```text
Aplicação Spring Boot
    │ Micrometer + OpenTelemetry
    │ exporta via OTLP
    ▼
Jaeger
    └── Interface web para consultar os traces
```

- Jaeger disponível no Docker Compose local.
- Endereço, protocolo e configurações de exportação externalizados por ambiente.
- Identificação estável do serviço.
- Logs com `traceId` e `spanId` durante a execução com contexto ativo.
- Amostragem de 100% para novos traces locais; configuração própria para os demais ambientes.
- Instrumentação explícita das operações importantes entre módulos, pois chamadas Java não produzem automaticamente spans de negócio.
- Observações/spans implementados em decoradores e adaptadores de infraestrutura, preservando o core.
- Não duplicar spans já produzidos pela instrumentação automática.
- Não registrar payloads, credenciais ou dados pessoais como atributos de tracing.
- Quando houver clientes HTTP futuramente, usar builders configurados pelo Spring para propagação do contexto.
- Caso sejam introduzidos fluxos assíncronos, implementar e testar propagação de contexto; eles não fazem parte do fluxo inicial.

Exemplo de visualização esperado:

```text
POST /pedidos
└── pedidos.criar
    ├── estoque.reservar
    └── pedidos.persistir
```

Instrumentar explicitamente `pedidos.persistir` caso esse span seja incluído; não presumir que a dependência de tracing instrumente automaticamente cada chamada JPA.

Pedidos e Estoque são operações do mesmo serviço, e não dois serviços distribuídos. A instrumentação fica preparada para continuidade do trace em futuras chamadas remotas.

Exportar diretamente ao Jaeger na versão inicial. OpenTelemetry Collector separado não é obrigatório. A indisponibilidade do backend de tracing não deve causar falha de negócio nem espera indefinida na requisição.

Adicionar Actuator com health check e documentação dos endpoints expostos. Não incluir Grafana, Tempo, Zipkin ou SigNoz na implementação inicial: foram alternativas avaliadas, e Jaeger foi escolhido.

## 11. Execução, build e CI

- README com requisitos, inicialização, testes e exemplos HTTP.
- Maven Wrapper para build reprodutível.
- `./mvnw verify` executa testes unitários, integração e verificações de arquitetura.
- Configurar convenção de nomes e plugins de testes para que nenhuma categoria seja omitida silenciosamente.
- Formatação automática e verificação no build.
- Docker Compose com PostgreSQL e Jaeger; documentar execução da aplicação pela IDE e pelo Dockerfile.
- Configuração por ambiente, sem credenciais de produção versionadas.
- CI com Java compatível, Docker para Testcontainers e execução de `verify`.
- Definir o provedor de CI conforme o repositório de destino.

## 12. Documentação e orientação para IA

Entregáveis esperados na raiz:

```text
backend-base/
├── src/main/
├── src/test/
├── docs/
│   ├── architecture.md
│   ├── adding-a-module.md
│   └── decisions/
├── AGENTS.md
├── README.md
├── compose.yaml
├── Dockerfile
├── pom.xml
├── mvnw
└── mvnw.cmd
```

### `AGENTS.md`

Registrar limites de dependência, convenções de pacotes, referências concretas de implementação, comandos de validação e recursos fora do escopo. Orientar a IA a:

- Reutilizar os padrões demonstrados pelo fluxo de referência.
- Criar apenas classes e abstrações necessárias ao comportamento solicitado.
- Preservar o core independente de frameworks.
- Não acessar internals de outro módulo nem criar dependências cíclicas.
- Não adicionar Kafka, Redis, circuit breaker ou outras plataformas sem requisito concreto.
- Atualizar testes e documentação quando houver alteração de comportamento ou arquitetura.

### Decisões arquiteturais

Documentar ao menos: monólito modular em um Maven único; core puro; portas entre módulos; transação compartilhada; estratégia de concorrência; PostgreSQL/Testcontainers; tracing OpenTelemetry/OTLP com Jaeger.

### Guia de novo módulo

Explicar como criar domínio, caso de uso, portas, adaptadores, configuração, migrations e testes, indicando quais componentes são opcionais. A ausência de HTTP em Estoque deve servir de exemplo de que todo módulo não precisa de controller.

## 13. Critérios de aceite

- [ ] Build executável com Java e Spring Boot nas versões documentadas.
- [ ] Pedidos e Estoque no mesmo processo, com core puro em ambos.
- [ ] Estoque sem endpoint HTTP e sem pacote `api` adicional.
- [ ] Integração exclusivamente pelo adaptador de Pedidos e pela porta de entrada de Estoque.
- [ ] Fluxo de criação de pedido com múltiplos itens funcionando.
- [ ] Reserva e pedido atômicos, com rollback demonstrado por teste.
- [ ] Proteção contra reserva concorrente validada.
- [ ] PostgreSQL e migrations executados pelos testes via Testcontainers.
- [ ] Cinco categorias de testes presentes e incluídas no build.
- [ ] Regras ArchUnit impedem acessos proibidos e ciclos.
- [ ] Erros HTTP padronizados e OpenAPI documentada.
- [ ] Jaeger local recebe trace com spans de Pedidos e Estoque.
- [ ] Logs correlacionados ao trace.
- [ ] Docker Compose, Dockerfile, Maven Wrapper e CI utilizáveis.
- [ ] README, AGENTS.md e documentação arquitetural suficientes para outro desenvolvedor ou IA repetir o padrão.
- [ ] Kafka, circuit breaker e cache distribuído ausentes.

## 14. Referências técnicas

- Spring Boot — tracing: https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- Spring — transações programáticas: https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html
- ArchUnit: https://www.archunit.org/userguide/html/000_Index.html
- Jaeger: https://www.jaegertracing.io/docs/latest/
- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

Antes de implementar, verificar a documentação correspondente às versões efetivamente escolhidas. Spring Modulith foi citado como referência durante a discussão, mas não é uma dependência obrigatória desta especificação; ArchUnit é a ferramenta escolhida para verificar os limites iniciais.
