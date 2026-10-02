# Adicionar módulo

1. Crie `<modulo>.core.domain` com modelos e invariantes Java puros. Teste sem Spring.
2. Defina porta de entrada, comandos, resultados e falhas públicas em `core.application.port.in`. Crie somente portas de saída exigidas pelo caso de uso.
3. Implemente coordenação em `core.application.usecase`, recebendo portas por construtor. Teste com fakes ou mocks.
4. Implemente persistência com entidades separadas, mapeamento explícito e migrations com proprietário documentado. Use PostgreSQL/Testcontainers nos testes.
5. Se consumir outro módulo, crie adaptador em `infrastructure.integration`; consuma apenas a porta de entrada e seus contratos. Traduza falhas públicas.
6. Componha beans em `infrastructure.config`. Mantenha transação e observabilidade em decoradores quando necessários. Teste a composição real e os limites transacionais.
7. HTTP é opcional: Estoque é o exemplo de módulo sem controller. Quando houver HTTP, separe DTO e comando, valide transporte e documente ProblemDetail/OpenAPI.
8. Atualize ArchUnit para incluir os novos limites, sem ciclos. Execute `./mvnw spotless:apply verify` e atualize documentação.

Não criar pacotes vazios, interfaces sem necessidade ou infraestrutura preventiva. Controllers, integrações e observabilidade não precisam existir em todos os módulos.
