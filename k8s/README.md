# Deploy no homelab

## Fluxo

PRs para `develop` e `master`: testes Java 25, cobertura unitária mínima de 90%, Gitleaks para segredos e Trivy para manifests (as dependências são analisadas na imagem construída). `Verify / verify` e `Verify / security` devem ser checks obrigatórios das duas branches, sem bypass.

Push em `develop` ou `master`: repete as verificações, constrói a imagem, analisa a imagem, publica no GHCR e atualiza somente o overlay correspondente na branch `deploy`, usando digest imutável. Argo CD acompanha essa branch. A branch `deploy` deve permitir escrita pelo `GITHUB_TOKEN`; as branches de código continuam protegidas. Não editar os manifests diretamente em `deploy` durante uma publicação; alterações estruturais devem ser incorporadas a essa branch antes da próxima atualização.

Os testes de integração continuam obrigatórios, mas não aumentam a cobertura unitária: o agente JaCoCo é configurado apenas no Surefire. Medimos linhas de toda a aplicação, sem excluir arquivos para atingir a meta. Em 02/10/2026, a validação local passou 15 testes unitários/arquitetura/web e 6 de integração, mas a cobertura foi 78/155 linhas (50,32%). O build reprovou corretamente no limite de 90%; é necessário ampliar os testes antes do primeiro deploy.

## Preparação do servidor

1. Instalar Argo CD seguindo o script do projeto homelab2, versão estável revisada `v3.5.3` encontrada em 02/10/2026. Conferir os pods após a instalação.
2. Criar os namespaces `pedidobasecode-dev` e `pedidobasecode-prod`.
3. Instalar os PostgreSQL de `databases-dev` e `databases-prod` a partir do homelab2. Seus Secrets administrativos devem ser criados fora do Git. Não substituir o PostgreSQL atual em `homelab`.
4. Em cada instância, criar `pedidobasecode_dev` ou `pedidobasecode_prod`, com usuário exclusivo, proprietário do respectivo banco. Revogar CONNECT de PUBLIC e conceder ao usuário da aplicação. Flyway executa as migrations no startup; não rodar duas versões concorrentes em migrações incompatíveis. Fazer backup antes de mudanças destrutivas.
5. Em cada namespace da aplicação, criar Secret `pedidobasecode-db` com `DB_USER` e `DB_PASSWORD`. O DB_URL já está no ConfigMap do overlay. Criar usando arquivos de credenciais protegidos, sem senhas no shell ou no Git.
6. O repositório é privado. Configurar no Argo CD uma credencial somente de leitura desse repositório. Criar também `ghcr-pull` (tipo `kubernetes.io/dockerconfigjson`) nos dois namespaces, com credencial de leitura do pacote GHCR. Usar token com `read:packages` e acesso ao pacote, ou mecanismo equivalente. Não reutilizar token de administração.
7. Configurar limites por namespace e NetworkPolicies que permitam acesso ao banco do próprio ambiente e DNS, além do tráfego web necessário. Conferir se o mecanismo de NetworkPolicy está ativo no K3s antes de considerar o isolamento validado.
8. Incluir ambos os bancos no backup e testar restauração. O backup antigo de `homelab` não os inclui.
9. Criar a branch `deploy` com os manifests `k8s` e permitir a escrita do Actions. Inicialmente a imagem usa `bootstrap`, que não existe: aguardar a primeira publicação aprovada, que troca a referência por digest, antes de cadastrar as Applications.
10. Aplicar `k8s/argocd/dev.yaml` e `k8s/argocd/prod.yaml` no servidor depois que os pré-requisitos existirem. Os AppProjects limitam os recursos e namespaces permitidos. Não gerenciam Secrets, namespaces ou bancos.

## Acesso inicial sem domínio

Manter os serviços ClusterIP. Em uma sessão no servidor:

```bash
sudo k3s kubectl -n argocd port-forward svc/argocd-server 8081:443 --address 127.0.0.1
```

No Mac, abrir um túnel em outra sessão:

```bash
ssh -L 8081:127.0.0.1:8081 ochannel@10.0.0.60
```

Acessar `https://localhost:8081`. Recuperar a senha inicial somente no terminal privado do servidor e alterá-la no primeiro acesso. Não enviar a senha ao chat. Para testar a API, usar port-forward de `svc/pedidobasecode` no namespace desejado e túnel SSH equivalente. Domínio e Cloudflare Tunnel podem ser adicionados depois.

## Rollback e validação

Reverter na branch `deploy` o commit que alterou o digest e aguardar a sincronização. Rollback da imagem não desfaz migrations de banco; manter migrations compatíveis e testar restauração.

Validar: PR com cobertura insuficiente bloqueado; imagem vulnerável não publicada; merge em develop atualiza apenas dev; merge em master atualiza apenas prod; readiness verifica banco; credenciais de dev não acessam prod; dados persistem; backups restauram.

SonarQube não está instalado nesta primeira etapa. A cobertura é bloqueada pelo JaCoCo sem depender dele. Podemos adicionar o painel e Quality Gate depois, escolhendo a edição adequada à análise de PRs e branches.
