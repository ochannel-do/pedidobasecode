# GitFlow, TDD e Kanban

## Branches e deploy

| Origem | Base de criação | Destino do PR | Efeito após merge |
| --- | --- | --- | --- |
| `feature/<issue>-<descricao>` | `develop` | `develop` | Deploy dev após checks |
| `release/<versao>` | `develop` | `master` e depois `develop` | Deploy prod ao entrar em master; dev na incorporação |
| `hotfix/<issue>-<descricao>` | `master` | `master` e depois `develop` | Correção prod e incorporação em dev |
| `master` | — | `develop` | Sincronização da versão publicada |

`develop` é a branch padrão de trabalho; `master` representa produção. `deploy` contém os manifests/digests consumidos pelo Argo CD e não recebe código por PR. A branch antiga `main` pode permanecer como histórico; não é alvo do pipeline.

Branches `feature/*` também servem para melhorias de CI, documentação e refactor: o tipo do trabalho está na issue/commits. Features não entram diretamente em master; develop não entra diretamente em master: abrir release. Usar merge commit, sem squash, para preservar a evidência RED/GREEN. Uma release pode levar alterações já testadas de várias features.

## Commits e TDD

Tipos de commits: `feat`, `fix`, `refactor`, `test`, `ci`, `docs`, `build`, `chore`, `style`, `perf`, com escopo opcional, como `test(pedidos): ...`.

Para cada ciclo de comportamento novo/correção:

1. Criar/alterar teste e executá-lo: deve falhar pelo comportamento ainda ausente, não por falha de infraestrutura. Registrar comando e resultado.
2. Commit `test: ...` com o teste RED, separado da implementação de produção.
3. Implementar e executar o teste: GREEN.
4. Commit `feat: ...` ou `fix: ...`. Refactor posterior mantém os testes passando.
5. Executar `./mvnw clean verify` e enviar o PR, registrando SHAs e evidências.

Um commit intermediário RED pode reprovar na CI. O estado final do PR precisa passar em todas as verificações. A CI roda sobre a versão final do PR, não exige que cada commit histórico passe.

O check `gitflow` valida a rota e exige alteração de testes em um commit `test:` anterior a cada `feat:`/`fix:` no intervalo do PR. Não considera testes apagados nem permite mudança de `src/main` no commit RED. O check valida sequência e arquivos, mas não comprova que o teste falhou pelo motivo correto nem que corresponde à implementação: isso é revisado usando as evidências do PR. A cobertura complementa essa revisão, sem garantir qualidade das asserções.

Documentação e CI não exigem teste de negócio artificial. Mudanças no validador têm seus próprios testes Python. Dependabot cria PRs de manutenção; branches `dependabot/*` devem ser incorporadas via uma feature de manutenção, ou adaptar a política explicitamente antes de automatizar sua aprovação.

## Checks e limitações atuais

Checks para tornar obrigatórios: `gitflow`, `policy-tests`, `verify` e `security`. O workflow de publicação aguarda testes/segurança e atualiza apenas dev/prod conforme a branch. Sem proteção de branches, GitHub ainda permite bypass manual: a API informou que este repositório privado precisa de GitHub Pro para configurar a proteção.

Cobertura verificada com Java 25 após a ampliação dos testes: 92,26% (143/155 linhas), acima do mínimo de 90%; ramos de decisão: 90,48% (38/42). Os 34 testes unitários/arquitetura/HTTP e 6 de integração passaram. Não reduzir a meta nem excluir código apenas para liberar o deploy.

## Kanban

Colunas: **Backlog → Pronto → Em desenvolvimento → Em revisão → Concluído**.

Board: https://github.com/users/ochannel-do/projects/1, vinculado ao repositório, com campo `Tipo` e labels correspondentes:

| Tipo | Label | Commit habitual |
| --- | --- | --- |
| Feature | `type:feature` | `feat` |
| Bug | `type:bug` | `fix` |
| Refactor | `type:refactor` | `refactor` |
| Test | `type:test` | `test` |
| CI/CD | `type:ci` | `ci` |
| Docs | `type:docs` | `docs` |

Cada issue tem problema, critérios de aceite e validação. O número aparece na branch; o PR usa `Closes #N`. Marcar Concluído depois da validação do resultado/deploy aplicável, não apenas quando o PR abre. Um card para a feature inteira acompanha ambos os commits test e feat.

O Project, suas cinco colunas e o campo `Tipo` foram criados. Templates de issue acompanham este PR; ficam disponíveis na interface depois de incorporados à branch padrão `develop`. Para administrar o Project pelo GitHub CLI, a autorização com escopo `project` foi concluída pelo usuário. Não compartilhar tokens no chat.
