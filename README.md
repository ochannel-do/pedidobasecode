# Base de backend Java

Monólito modular de referência: Java 25 (sem preview), Spring Boot 4.1.1 e Maven 3.9.16. Pedidos e Estoque compartilham PostgreSQL e uma transação. O core é Java puro.

## Executar

Requisitos: JDK 25, Docker com Compose e acesso ao Maven Central. Configure `JAVA_HOME` para o JDK 25. O wrapper baixa Maven; não exige instalação global. `.mvn/settings.xml` usa Maven Central isoladamente para evitar mirrors pessoais indisponíveis; ajuste esse arquivo se o projeto consumidor exigir um mirror corporativo.

```sh
docker compose up -d postgres jaeger
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

A IDE deve usar Java 25, executar `LojaApplication` e ativar o perfil `local`. Flyway cria o schema; Hibernate apenas valida. Após a primeira inicialização, em outro terminal:

```sh
docker compose exec -T postgres psql -U loja -d loja < scripts/seed-local.sql
curl -i http://localhost:8080/pedidos -H 'Content-Type: application/json' -d '{"itens":[{"produtoId":"00000000-0000-0000-0000-000000000001","quantidade":2},{"produtoId":"00000000-0000-0000-0000-000000000002","quantidade":1}]}'
```

O seed é manual, somente para desenvolvimento, e não faz parte das migrations. Não executá-lo em produção. Sucesso: HTTP 201 com `{"id":"<uuid>","status":"CRIADO"}`. Sem estoque: HTTP 409, ProblemDetail com `code: ESTOQUE_INSUFICIENTE`; itens vazios, produtos repetidos ou quantidades inválidas: 400 com `ENTRADA_INVALIDA`; falhas inesperadas: 500 com `ERRO_INTERNO` sem detalhes internos. Contrato completo: [OpenAPI](docs/openapi.yaml).

```sh
./mvnw verify
docker compose --profile app up --build -d
```

`verify` executa domínio, casos de uso, HTTP, arquitetura (`*Test`) e integração (`*IT`, Failsafe). Integração exige Docker ativo e PostgreSQL real via Testcontainers; nenhum teste é ignorado quando Docker está indisponível. Spotless verifica formatação; `./mvnw spotless:apply` aplica o formato.

Configuração externa: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OTLP_ENDPOINT` (OTLP HTTP), `TRACE_SAMPLING`. Credenciais do Compose são exclusivamente locais. Exportação de tracing tem timeout de 2 segundos e ocorre em background. O perfil local amostra 100%; demais ambientes usam 10% por padrão.

Jaeger: http://localhost:16686, serviço `loja`. Após enviar pedido, procure `POST /pedidos → pedidos.criar → estoque.reservar / pedidos.persistir`. Logs incluem traceId/spanId com contexto ativo. Actuator expõe somente `/actuator/health` e `/actuator/info`; detalhes de health seguem os padrões do Spring.

CI: exemplo em GitHub Actions, escolhido como provedor inicial para esta base ainda sem repositório remoto. Antes de implantação, definir autenticação, autorização e infraestrutura de produção no projeto consumidor.

Arquitetura: [limites](docs/architecture.md), [novo módulo](docs/adding-a-module.md), [decisões](docs/decisions/001-base.md). Especificação de origem preservada em `docs/SPEC-BACKEND-BASE.md`.
