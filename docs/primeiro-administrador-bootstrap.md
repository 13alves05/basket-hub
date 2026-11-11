# Provisionamento do primeiro Administrador

> Estado: desenho técnico aprovado como próximo passo; ainda não implementado. Não configurar produção nem expor a aplicação enquanto a autenticação e a autorização não estiverem concluídas.

## Objetivo

Criar a primeira conta com o papel global `ADMINISTRADOR` sem registo público, sem credenciais versionadas e sem uma conta administrativa predefinida.

A implementação deve integrar-se no stack atual: Kotlin/JVM 21, Ktor 3.5.0, Exposed 1.3.0 com R2DBC e H2. Não copiar autenticação inexistente do repositório de grupo.

## Decisão de desenho

Usar um segredo de bootstrap temporário fornecido apenas por variável de ambiente:

- `ADMIN_BOOTSTRAP_TOKEN`: segredo aleatório de alta entropia, fornecido pelo gestor de segredos do ambiente.
- O pedido de bootstrap envia o segredo no cabeçalho `Authorization: Bearer`, nunca na query string nem no URL.
- O pedido inclui o email e a palavra-passe inicial da conta administrativa. A palavra-passe nunca é enviada por variável de ambiente, argumento de linha de comandos ou log.
- O endpoint só funciona enquanto não existir nenhuma conta com o papel `ADMINISTRADOR` e enquanto o bootstrap não estiver marcado como concluído na base de dados.
- Após a criação, a conclusão é persistida na mesma transação que cria a conta e atribui o papel. Repetir o pedido deve falhar, mesmo que a variável de ambiente ainda esteja configurada.
- O segredo deve ser removido do gestor de segredos após o provisionamento. A desativação persistida na base de dados é a proteção principal; remover a variável é defesa adicional.
- O endpoint deve ser desativado por configuração após o primeiro provisionamento e não pode ser usado para promover contas adicionais.
- Não implementar registo público nesta fase.

## Requisitos obrigatórios

### Verificação do segredo

- Exigir um segredo forte, gerado criptograficamente (pelo menos 32 bytes aleatórios).
- Comparar o valor recebido em tempo constante.
- Recusar se a variável estiver ausente, vazia ou abaixo do comprimento mínimo.
- Nunca registar o segredo, o cabeçalho Authorization, a palavra-passe ou o corpo sensível do pedido.
- Aplicar limitação de tentativas e respostas genéricas para falhas de autenticação.
- Exigir HTTPS em qualquer ambiente não local.

### Criação atómica e concorrência

A operação deve ser atómica:

1. Verificar se o bootstrap já foi concluído.
2. Confirmar que ainda não existe qualquer conta Administrador.
3. Validar e normalizar o email.
4. Derivar o hash da palavra-passe com um algoritmo de password hashing adequado (preferir Argon2id; se for usada alternativa, documentar parâmetros e custo).
5. Criar a conta.
6. Atribuir o papel `ADMINISTRADOR`.
7. Marcar o bootstrap como concluído.

Os passos 1–7 têm de ocorrer numa única transação, com proteção contra duas chamadas simultâneas. A base de dados deve impor unicidade no email normalizado e na atribuição de papel. Se qualquer passo falhar, a transação reverte integralmente.

Não confiar apenas numa verificação feita em memória ou num booleano local: o estado de conclusão tem de ser persistido na base de dados.

### Dados e auditoria

Modelo mínimo esperado:

- `accounts`: identificador, email normalizado único, hash de palavra-passe, estado ativo, timestamps.
- `roles`: catálogo de papéis, incluindo `ADMINISTRADOR`.
- `account_roles`: associação muitos-para-muitos com restrição única.
- `system_security_state` (ou equivalente): estado persistente de bootstrap concluído.
- `security_audit_log`: registar evento, conta criada, data e resultado; nunca guardar segredos nem palavras-passe.

A criação da primeira conta deve produzir um evento de auditoria. Não guardar o token de bootstrap em texto simples na base de dados.

### Resposta e comportamento

- Sucesso: resposta sem devolver o token, hash ou palavra-passe.
- Segredo inválido: `401 Unauthorized`.
- Bootstrap já concluído/desativado: `409 Conflict` ou resposta genérica equivalente, sem revelar detalhes internos.
- Dados inválidos: `400 Bad Request`, sem ecoar a palavra-passe.
- Falha inesperada: `500 Internal Server Error` com mensagem genérica; detalhes apenas no log seguro.

## Testes de aceitação obrigatórios

1. Sem variável de ambiente: bootstrap indisponível.
2. Segredo vazio ou fraco: bootstrap indisponível.
3. Segredo incorreto: não cria conta nem papel.
4. Pedido válido: cria exatamente uma conta e atribui Administrador.
5. Palavra-passe: só fica persistido um hash com salt; nunca texto simples.
6. Segundo pedido válido: recusado e não cria outra conta.
7. Duas chamadas simultâneas: apenas uma pode concluir.
8. Falha entre criação de conta e atribuição do papel: rollback integral.
9. Email duplicado ou inválido: operação recusada sem estado parcial.
10. Token e palavra-passe não aparecem em logs nem respostas.
11. Um utilizador comum não consegue invocar bootstrap nem atribuir papéis.
12. Remover a variável após sucesso não afeta a conta já criada.

## Limites de segurança

O bootstrap apenas cria a primeira identidade administrativa. **Não protege, por si só, as restantes rotas.** Antes de permitir produção, ainda é obrigatório implementar login, sessões revogáveis, logout, autorização server-side em todas as rotas e operações, validação de âmbito dos recursos, proteção CSRF se forem usadas cookies, e testes positivos/negativos de cada papel.

A conta Administrador deve poder acumular outros papéis, mas as permissões de cada papel não administrativo continuam limitadas aos respetivos recursos. O papel Administrador é global e as suas atribuições/remoções têm de ser auditadas.

## Configuração operacional futura

O segredo deve ser criado fora do repositório e inserido diretamente no gestor de segredos do ambiente. Nunca commitar um ficheiro `.env` real, valor de exemplo que pareça uma credencial válida, token de teste reutilizável ou palavra-passe.

A documentação operacional deve explicar como gerar, configurar e remover o segredo sem o expor no terminal, histórico de shell, CI ou logs.

## Estado atual do repositório

- A aplicação continua a recusar `APP_ENV=production`.
- O código atual não tem autenticação, contas, hash de palavra-passe, papéis persistidos nem sessões.
- Este documento especifica o provisionamento; não afirma que o endpoint ou o sistema de autenticação já estejam implementados.


## Estado de implementação (2026-11-11)

A implementação inicial já existe na branch `feature/admin-bootstrap-security`:
- `POST /auth/bootstrap` aceita o token Bearer e os dados iniciais da conta.
- O endpoint só responde em `APP_ENV=development`; permanece indisponível noutros ambientes.
- O token configurado tem de ter pelo menos 32 bytes.
- A criação da conta, atribuição do papel Administrador, estado de bootstrap e auditoria é feita numa transação.
- As palavras-passe são armazenadas com Argon2id; não são guardadas em texto simples.
- O estado de bootstrap é persistido e a chave primária impede duas inicializações concorrentes.

Testes unitários do hashing Argon2id e build Gradle passaram no CI do commit `2ed1e42`.

Ainda falta testar o endpoint e o comportamento transacional em integração, implementar login/sessões/logout, proteger todas as rotas por papel e âmbito, validar CSRF se forem usadas cookies e concluir a revisão HTTPS/limitação de tentativas. A barreira de produção mantém-se ativa. Esta implementação inicial não deve ser interpretada como autorização para publicar a aplicação.


## Estado atualizado (2026-11-11)

Além do bootstrap, a branch já contém:
- `POST /auth/login`, com resposta Bearer e expiração de 8 horas.
- `GET /auth/me`, para consultar a identidade e os papéis da sessão.
- `POST /auth/logout`, com revogação persistente.
- Tokens aleatórios de 256 bits; a base de dados guarda apenas SHA-256 do token.
- Auditoria de login bem-sucedido e revogação.

Os testes de bootstrap e sessão, bem como o build Gradle, passaram no CI do commit `4fd9969` (run `43058203577`).

Continuam pendentes a proteção de todas as rotas de negócio por autenticação, papel e âmbito, testes negativos de autorização, limitação de tentativas de login, recuperação de conta e revisão operacional de HTTPS/proxy/CORS. O arranque em produção continua bloqueado.
