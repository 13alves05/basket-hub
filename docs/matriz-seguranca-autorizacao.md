# Matriz de segurança e autorização

> Estado auditado em 2026-11-11. Documento de análise do código atual; a decisão de permitir múltiplos papéis por conta foi confirmada. A matriz funcional de permissões continua a ser uma proposta inicial, não uma especificação integralmente validada.

## Estado atual

- Não existe autenticação nem autorização nas rotas HTTP analisadas.
- A aplicação recusa o arranque quando `APP_ENV=production`, através de `ensureDeploymentEnvironmentIsSupported` em `src/main/kotlin/main.kt`.
- A proteção de arranque é uma barreira temporária, não um controlo de acesso por rota.
- Não remover essa barreira até existir autenticação, autorização, proteção CSRF quando aplicável e testes negativos de acesso.
- Foi confirmada a possibilidade de uma conta acumular vários papéis; cada papel mantém o seu próprio âmbito de recursos.
- O projeto contém um `ExposedUserService` de exemplo (nome e idade), mas não contém um sistema de contas/autenticação utilizável.

## Modelo de contas e papéis

A decisão e a proposta técnica estão documentadas em [Modelo de contas e papéis](modelo-contas-papeis.md).

| Papel | Âmbito proposto | Permissões resumidas |
|---|---|---|
| Administrador | Plataforma | Gerir contas, papéis e todas as entidades; ações auditadas |
| Organizador | Eventos atribuídos | Gerir eventos e operações associadas |
| Gestor de Equipa | Equipas atribuídas | Gerir a própria equipa, jogadores e inscrições |
| Jogador | Perfil associado | Consultar dados próprios, jogos, inscrições e estatísticas |
| Patrocinador | Perfil/contratos próprios | Gerir perfil e propostas/contratos próprios |
| Visitante | Informação pública | Consultar conteúdo publicado como público |

Uma conta pode acumular qualquer combinação operacional necessária. As permissões são combinadas, mas cada operação verifica sempre o âmbito específico do recurso. Apenas Administrador pode atribuir ou remover papéis. Visitante não necessita de conta.

**Ainda por decidir antes da implementação de autenticação:** processo seguro de criação do primeiro Administrador, política de registo de contas, recuperação de conta/verificação de email e campos de perfil editáveis. Não criar registo público nem uma conta administrativa por defeito.

## Inventário de rotas

Todas as rotas abaixo estão atualmente sem autenticação/autorização no código auditado. A classificação "leitura" ou "escrita" descreve o efeito técnico, não define quem deve ter acesso.

### API — leitura

| Método | Rota | Dados/efeito |
|---|---|---|
| GET | `/equipas`, `/equipas/{id}` | Consulta equipas |
| GET | `/jogadores`, `/jogadores/{id}` | Consulta jogadores |
| GET | `/eventos`, `/eventos/{id}` | Consulta eventos |
| GET | `/jogos`, `/jogos/{id}` | Consulta jogos |
| GET | `/inscricoes`, `/inscricoes/{id}` | Consulta inscrições |
| GET | `/organizadores`, `/organizador/{id}` | Consulta organizadores |
| GET | `/pavilhoes`, `/pavilhoes/{id}` | Consulta pavilhões |
| GET | `/patrocinadores`, `/patrocinadores/{id}` | Consulta patrocinadores |
| GET | `/estatisticas/{jogadorId}` | Consulta histórico estatístico |
| GET | `/eventos/{id}/chaveamento` | Consulta chaveamento |
| GET | `/classificacoes/geral`, `/classificacoes/torneio/{eventoId}` | Consulta classificações |

### API — escrita

| Método | Rota | Operação |
|---|---|---|
| POST | `/equipas` | Criar equipa |
| PUT / DELETE | `/equipas/{id}` | Alterar/remover equipa |
| POST | `/jogadores` | Criar jogador |
| PUT / DELETE | `/jogadores/{id}` | Alterar/remover jogador |
| POST | `/eventos` | Criar evento |
| PUT / DELETE | `/eventos/{id}` | Alterar/remover evento |
| POST | `/jogos` | Criar jogo |
| PUT / DELETE | `/jogos/{id}` | Alterar/remover jogo |
| POST | `/inscricoes` | Criar inscrição |
| PUT / DELETE | `/inscricoes/{id}` | Alterar/remover inscrição |
| POST | `/organizador` | Criar organizador |
| PUT / DELETE | `/organizador/{id}` | Alterar/remover organizador |
| POST | `/pavilhoes` | Criar pavilhão |
| PUT / DELETE | `/pavilhoes/{id}` | Alterar/remover pavilhão |
| POST | `/patrocinadores` | Criar patrocinador |
| PUT / DELETE | `/patrocinadores/{id}` | Alterar/remover patrocinador |
| POST | `/estatisticas` | Registar movimento estatístico |
| POST | `/eventos/{id}/agendamento` | Gerar/avançar chaveamento |

### Interface web renderizada no servidor

| Método | Rota | Observação |
|---|---|---|
| GET | `/`, `/hub`, `/html-thymeleaf` | Página inicial/hub |
| GET | `/organizador` | Página e, conforme Accept, listagem JSON |
| GET | `/gestor-equipa` | Página de gestão de equipa |
| GET | `/jogador` | Página e, conforme Accept, listagem JSON |
| GET | `/patrocinadores` | Página e, conforme Accept, listagem JSON |
| GET | `/publico` | Página pública |
| POST | `/patrocinadores/financiar` | Cria contrato de patrocínio via formulário |
| POST | `/eventos/{id}/agendamento` | Gera/avança o chaveamento; implementada em `FrontendRotas.kt` e protegida por âmbito de evento |

Outras rotas técnicas encontradas: `/test`, `/json/kotlinx-serialization` e recursos estáticos em `/static`. Devem ser avaliadas separadamente antes de uma publicação.

## Requisitos técnicos para a implementação

- Aplicar autenticação e autorização no servidor; esconder botões na interface não é proteção.
- Usar negação por omissão e verificar a autorização em cada operação e recurso.
- Impedir IDOR: validar que o utilizador autenticado tem acesso ao evento/equipa/jogador indicado no URL ou corpo.
- Suportar vários papéis por conta com associações de âmbito (evento, equipa, jogador ou patrocinador).
- Se a sessão usar cookies, proteger operações de escrita contra CSRF e configurar cookies `HttpOnly`, `Secure` e `SameSite` adequadamente.
- Validar entradas e limitar campos/valores; não confiar em IDs enviados pelo cliente.
- Não devolver exceções internas nem dados pessoais desnecessários.
- Criar testes para não autenticado, autenticado sem permissão, acesso ao próprio recurso, acesso a recurso alheio, papéis combinados e operações permitidas.
- Manter `APP_ENV=production` bloqueado até todos os critérios anteriores estarem implementados e validados.

## Critério para desbloquear produção

Só considerar remover a barreira de arranque depois de:
- processo de provisionamento inicial de Administrador definido e seguro;
- matriz de papéis e permissões validada;
- todas as rotas classificadas e protegidas;
- testes automatizados positivos e negativos por grupo de rotas e por âmbito;
- proteção CSRF validada se forem usadas sessões por cookie;
- revisão de erros, CORS, segredos e configuração de produção concluída.

## Ficheiros auditados

- `src/main/kotlin/main.kt`
- `src/main/kotlin/FrontendRotas.kt`
- `src/main/kotlin/Routing.kt`
- `src/main/kotlin/Agendamento/Agendamento_Rotas.kt`
- `src/main/kotlin/Classificacao/Classificacao_Rotas.kt`
- `src/main/kotlin/Equipa/Equipa_Rotas.kt`
- `src/main/kotlin/Estatisticas/Estatisticas_Rotas.kt`
- `src/main/kotlin/Evento/Eventos_Rotas.kt`
- `src/main/kotlin/Inscricao/Inscricao_Rotas.kt`
- `src/main/kotlin/Jogador/Jogadores_Rotas.kt`
- `src/main/kotlin/Jogo/Jogo_Rotas.kt`
- `src/main/kotlin/Organizador/Organizadores_Rotas.kt`
- `src/main/kotlin/Patrocinador/Patrocinador_Rotas.kt`
- `src/main/kotlin/Pavilhao/Pavilhao_Rotas.kt`


## Auditoria da opção A — reutilização do projeto de grupo (2026-11-11)

Foi feita comparação do repositório individual com `bmgoliveira-UA/basket-hub`, incluindo as referências `main` e `master`, o histórico de commits disponível e o código-fonte Kotlin encontrado na `master`.

Resultado:
- Não foi encontrada implementação funcional de autenticação no código analisado do projeto de grupo.
- `main.kt` limita-se a configurar plugins, inicializar a base de dados e registar rotas; não instala autenticação nem middleware de autorização.
- `FrontendRotas.kt` regista páginas e operações diretamente, sem verificar uma sessão ou identidade autenticada.
- `Routing.kt` permite resolver páginas Thymeleaf por parâmetro de URL; o parâmetro `perfil` é lido da query string e não representa uma identidade autenticada nem uma autorização segura.
- `Http.kt` configura CORS e permite `anyHost()`; não implementa autenticação. A configuração não deve ser copiada para produção.
- `UsersService.kt` contém apenas um serviço de demonstração com `name` e `age`; não guarda credenciais, papéis ou sessões.
- O histórico de commits disponível não revelou commits dedicados a login/autenticação.
- A árvore da `master` contém um gitlink chamado `basket-hub`, mas não foi encontrado `.gitmodules` que identifique o repositório de origem; por isso, esse ponteiro não foi considerado uma implementação verificável nem foi importado.

Conclusão:
- A opção A foi concluída como auditoria: não existe, nas versões e ficheiros verificáveis, um sistema de autenticação seguro que possa ser reaproveitado.
- Não copiar o serviço `ExposedUserService`, o parâmetro `perfil` ou a configuração CORS como substitutos de autenticação.
- Próximo passo necessário: escolher um processo seguro para provisionar o primeiro Administrador. A alternativa recomendada é um segredo de bootstrap de uso único, fornecido apenas por variável de ambiente, que cria a primeira conta administrativa e fica inutilizável após sucesso. O registo público continua desativado.
- Manter o bloqueio de arranque em produção até à implementação e validação integral da autenticação e autorização.


## Decisão sobre o primeiro Administrador

Foi selecionado o provisionamento por segredo temporário de ambiente (bootstrap de uso único), sem registo público. A especificação está em [Primeiro Administrador — bootstrap seguro](primeiro-administrador-bootstrap.md).

Estado: especificação documentada; implementação de contas, armazenamento de credenciais, endpoint de bootstrap, sessões e autorização ainda pendente. A seleção desta abordagem não significa que o sistema de autenticação esteja concluído. Manter o bloqueio de `APP_ENV=production`.

## Revisão técnica do código individual (2026-11-11)

A inspeção detalhada da branch `feature/admin-bootstrap-security` confirmou:
- O projeto usa Kotlin/JVM 21, Ktor e Exposed R2DBC 1.3.0.
- O acesso à base de dados passa por `DBUtils.database`; o ambiente local usa H2 por omissão e produção exige `BASKET_HUB_DB_URL`, `BASKET_HUB_DB_USER` e `BASKET_HUB_DB_PASSWORD`.
- `DatabaseCreation.init()` cria os schemas de domínio existentes, mas ainda não cria contas, papéis, credenciais ou sessões.
- `ExposedUserService` é um CRUD de demonstração para nome/idade e não deve ser reaproveitado como autenticação.
- As rotas estão distribuídas por módulos, pelo que a proteção terá de abranger cada grupo de rotas e não apenas um endpoint novo.
- Foi removido o registo duplicado de `POST /eventos/{id}/agendamento` de `Agendamento_Rotas.kt`; a operação continua registada em `FrontendRotas.kt`. A rota `GET /eventos/{id}/chaveamento` mantém-se no módulo de agendamento.

### Sequência de implementação

1. Definir e testar o armazenamento de contas, papéis múltiplos, associação de âmbitos e estado de bootstrap na mesma base de dados R2DBC.
2. Implementar hashing de palavras-passe Argon2id e bootstrap atómico, de uso único, com segredo de ambiente.
3. Implementar login, logout, sessões revogáveis e proteção CSRF adequada ao mecanismo de sessão escolhido.
4. Aplicar autorização por omissão a todas as rotas, com verificação do papel e do âmbito do recurso.
5. Criar testes de bootstrap concorrente/repetido, credenciais inválidas, sessão expirada, CSRF, acesso sem papel, acesso a recurso alheio e combinação de papéis.
6. Só depois rever a barreira de produção; não a remover enquanto existir qualquer rota sem classificação e proteção.

A implementação não deve avançar com armazenamento de sessão apenas em memória nem com um endpoint de bootstrap sem persistência atómica. A configuração de produção atual continua bloqueada.


## Implementação inicial de identidade e bootstrap (2026-11-11)

Implementação inicial adicionada à branch de trabalho:
- Schema persistente para contas, papéis, associação conta-papel, âmbitos por recurso, sessões revogáveis, estado de segurança e auditoria.
- Catálogo dos seis papéis documentados, criado de forma idempotente na inicialização.
- Restrições de unicidade para email normalizado, token de sessão e associação de âmbito.
- Hashing de palavras-passe Argon2id (3 iterações, 64 MiB, paralelismo 1), com validação mínima de 12 caracteres e testes unitários de hash/verificação.
- Endpoint `POST /auth/bootstrap`, protegido por token Bearer de pelo menos 32 bytes e limitado intencionalmente a `APP_ENV=development`.
- Criação do primeiro Administrador numa transação; a chave primária do estado de bootstrap funciona como barreira de concorrência; conta, papel e evento de auditoria são gravados na mesma transação.

Validação CI no commit `2ed1e42`: testes Gradle e build passaram.

### Limitações ainda abertas

- O endpoint de bootstrap ainda não tem testes de integração para concorrência, rollback, repetição, token inválido, email duplicado e ausência de fugas em logs.
- Ainda não existe login, logout, validação de sessão, revogação operacional ou recuperação de conta.
- As rotas de negócio continuam sem autenticação/autorização.
- A limitação de tentativas e a configuração HTTPS/proxy para futura utilização operacional ainda precisam de implementação e validação.
- A barreira `APP_ENV=production` continua obrigatória. O endpoint de bootstrap não torna a aplicação pronta para produção.


## Estado atualizado da implementação (2026-11-11)

### Implementado na branch `feature/admin-bootstrap-security`

- Schema persistente de contas, catálogo de papéis, atribuições de papéis, âmbitos por recurso, sessões, estado de segurança e auditoria.
- Catálogo inicial de seis papéis criado de forma idempotente.
- Inicialização do schema de segurança integrada em `DatabaseCreation.init()`.
- Bootstrap do primeiro Administrador com token Bearer de pelo menos 32 bytes, apenas em `APP_ENV=development`, com criação transacional e proteção contra repetição.
- Hashing Argon2id das palavras-passe.
- Login por email/palavra-passe.
- Sessões Bearer opacas de 256 bits, com hash SHA-256 persistido, validade de 8 horas e revogação.
- Consulta da identidade e papéis em `GET /auth/me`.
- Logout em `POST /auth/logout`.
- Eventos de auditoria para bootstrap, login bem-sucedido e revogação de sessão.

### Testes e CI

- Testes para bootstrap, token inválido/fraco, persistência do Administrador, hash da palavra-passe, atribuição do papel e auditoria.
- Testes para login válido/inválido, sessão autenticada e revogação.
- CI passou em testes Gradle e build no commit `4fd9969` (run `43058203577`).

### Ainda não implementado — bloqueios de segurança

- Middleware/guardas de autenticação aplicados às rotas de negócio.
- Autorização por papel e por âmbito de evento/equipa/jogador/patrocinador.
- Testes negativos de acesso cruzado entre recursos e testes de integração HTTP dos endpoints de autenticação.
- Rate limiting/controlo de tentativas de login.
- CSRF, caso se opte por sessões em cookies; a implementação atual usa Bearer tokens.
- Revisão completa de HTTPS, proxy, CORS e gestão de segredos.
- Recuperação de conta e verificação de email.

A barreira `APP_ENV=production` mantém-se ativa. A existência de login e sessões não significa que as rotas da aplicação estejam protegidas.


## Primitiva de autorização por âmbito (2026-11-11)

Foi adicionada a classe `AuthorizationService`:
- Administrador ativo tem acesso global.
- Os restantes utilizadores têm de possuir o papel pedido e um âmbito explícito correspondente ao mesmo papel, tipo e identificador de recurso.
- Um âmbito atribuído a um papel não concede permissões a outro papel da mesma conta.
- Contas inativas são sempre recusadas.

Testes automatizados cobrem equipa atribuída, equipa diferente, isolamento entre papéis e Administrador inativo. CI passou em testes e build no commit `a68372a` (run `43059768612`).

**Limite importante:** esta classe ainda não está ligada às rotas de negócio. Não considerar a aplicação protegida por RBAC até os guards estarem aplicados a todas as rotas e os testes HTTP/IDOR passarem. A matriz por rota continua a precisar de validação funcional.


## Gestão administrativa de contas, papéis e âmbitos (2026-11-11)

Foram adicionados serviços e rotas administrativas protegidas por sessão Bearer e papel `ADMINISTRADOR`:
- `POST /admin/accounts`: criar conta ativa, com email normalizado e palavra-passe inicial; não atribui papéis por omissão.
- `POST /admin/accounts/{accountId}/roles`: atribuir um dos papéis existentes.
- `DELETE /admin/accounts/{accountId}/roles/{roleCode}`: remover um papel; remove também todos os âmbitos desse papel para impedir reativação futura.
- `POST /admin/accounts/{accountId}/scopes`: atribuir âmbito para EVENTO, EQUIPA, JOGADOR ou PATROCINADOR.
- `DELETE /admin/accounts/{accountId}/scopes`: revogar o âmbito correspondente.
- A última conta Administrador ativa não pode perder o papel Administrador.
- As alterações são auditadas com a identidade do Administrador que as executou.

Testes cobrem criação de conta sem papéis implícitos, email duplicado, atribuição de papel/âmbito, remoção de papel e limpeza dos âmbitos antigos, ator sem privilégios e proteção do último Administrador. CI passou em testes Gradle e build no commit `2cb2257` (run `43061777311`).

### Limitações

- As rotas administrativas de gestão de contas estão protegidas; as restantes rotas de negócio ainda não estão ligadas ao `AuthorizationService`.
- O processo de entrega segura da palavra-passe inicial, alteração de palavra-passe e recuperação de conta ainda não está implementado.
- A proteção concorrente do último Administrador deve ser revista com o mecanismo de isolamento/lock da base de dados antes de uso operacional.
- A matriz por rota e a correspondência entre recursos de domínio e âmbitos ainda precisam de validação.


## Política provisória aplicada às rotas de negócio (2026-11-11)

Foi ligada a guarda `enforceTemporaryAdministratorPolicy` aos grupos de rotas existentes:
- Frontend e formulários;
- eventos;
- equipas;
- jogadores;
- inscrições;
- jogos;
- patrocinadores;
- organizadores;
- pavilhões;
- estatísticas;
- agendamento;
- classificações.

Comportamento atual:
- Leituras GET explicitamente allowlisted em `BusinessRouteSecurity.kt` permanecem públicas.
- As restantes operações exigem sessão Bearer válida e papel `ADMINISTRADOR`.
- Pedido sem sessão: `401 Unauthorized`.
- Sessão válida sem papel Administrador: `403 Forbidden`.
- As rotas de gestão de contas continuam a usar a sua própria verificação de Administrador.

Esta política é deliberadamente conservadora e temporária. **Não implementa ainda permissões de Organizador, Gestor de Equipa, Jogador ou Patrocinador por âmbito.** O `AuthorizationService` ainda não está integrado nas operações de domínio, nem há filtragem das listagens por recurso atribuído.

Foi acrescentado um teste HTTP de integração para validar os resultados 401/403/200 e a disponibilidade da leitura pública de evento. A validação CI deste novo teste está pendente.

A barreira `APP_ENV=production` permanece ativa. Não remover até existir autorização específica por operação, testes HTTP/IDOR e revisão operacional.


## Progresso de autorização por âmbito — revisão seguinte (2026-11-11)

### Validação de equipa e evento

- A primitiva `AuthorizationService` passou a ser usada diretamente nas rotas de equipa.
- O Gestor de Equipa autenticado só consulta a lista das equipas atribuídas e só pode consultar, editar ou remover uma equipa dentro do seu âmbito.
- A criação de equipas continua reservada ao Administrador.
- As rotas de evento deixaram de usar a guarda provisória de Administrador em bloco e passaram a verificar sessão e autorização específica.
- A criação de eventos continua reservada ao Administrador.
- Um Organizador só pode editar ou remover eventos com âmbito explícito `ORGANIZADOR / EVENTO / id`.
- A atualização por Organizador não pode alterar o `organizadorId` do evento.
- As leituras públicas GET de eventos mantêm-se públicas, conforme a allowlist de conteúdo público.
- Foram acrescentados testes unitários de isolamento de âmbito para Organizador, além dos testes de Gestor de Equipa.

CI:
- Run 43077492381 (commit `803d129`): testes e build passaram, incluindo a correção de recolha dos âmbitos.
- Run 43079044414 (commit `1f180d7`): testes e build passaram após a integração das permissões de evento.
- Run 43079080189 (commit `c5be2eb`): validação do teste adicional de âmbito de Organizador ainda em execução no momento desta atualização.

### Jogadores e patrocinadores — bloqueio de modelação

A inspeção dos modelos atuais não permite implementar com segurança o acesso próprio de Jogador ou Patrocinador:

- `JogadorService.Jogadores` contém apenas nome, posição, número de camisola e ID. Não existe associação persistente entre jogador e equipa, nem entre jogador e conta autenticada.
- `PatrocinadorService.Patrocinadores` contém nome, empresa, valor de contrato e um `eventoId` opcional. Não existe associação entre patrocinador e conta autenticada.
- O âmbito `JOGADOR` ou `PATROCINADOR` sozinho não resolve a ligação entre a identidade autenticada e o registo de domínio. Aceitar um ID enviado pelo cliente criaria risco de IDOR/impersonação.
- Não foi inventada uma relação entre contas, jogadores, equipas ou patrocinadores. Até essa relação ser decidida e persistida, as rotas de jogadores e patrocinadores mantêm a guarda provisória de Administrador, com as leituras públicas apenas quando explicitamente permitidas pela allowlist global.

Decisões de domínio necessárias antes de implementar:
1. Um jogador é ligado a uma conta por uma associação única conta-jogador? Pode uma conta gerir mais do que um jogador (por exemplo, encarregado de educação)?
2. Um jogador pertence a uma equipa de cada vez ou pode estar associado a várias equipas/épocas?
3. Um patrocinador tem uma conta própria ligada a um único registo de patrocinador? Quem pode criar essa ligação e aprová-la?
4. Um contrato de patrocínio pertence ao patrocinador, ao evento, ou a ambos? Quem pode ver o valor contratual?

Não desbloquear estas rotas por inferência baseada em nome, email, query parameter ou ID recebido do cliente.

### Estado após esta revisão

- Equipa: autorização por âmbito implementada; falta reforçar testes HTTP de isolamento por equipa.
- Evento: autorização por âmbito implementada para Organizador; falta reforçar testes HTTP de isolamento por evento e rever todas as operações associadas (inscrições, agendamento, jogos).
- Jogador: ainda sob política provisória de Administrador; falta decidir e implementar a relação conta-jogador/equipa.
- Patrocinador: ainda sob política provisória de Administrador; falta decidir e implementar a relação conta-patrocinador/contratos.
- Produção continua bloqueada por `APP_ENV=production`. Não fazer merge nem deploy até terminar a autorização de todas as rotas e a revisão operacional.



## Auditoria de inscrições, jogos e recursos associados (2026-11-11)

A revisão das rotas e serviços de domínio identificou relações persistentes úteis para a próxima fase de autorização, mas não alterou permissões sem fechar a política de negócio.

### Inscrições

- A tabela `inscricoes` contém chaves estrangeiras reais para `equipa_id` e `evento_id`.
- A inscrição pode, portanto, ser relacionada com o âmbito de uma equipa (`GESTOR_EQUIPA / EQUIPA`) e com o âmbito de um evento (`ORGANIZADOR / EVENTO`).
- A listagem atual devolve todas as inscrições; a rota permanece sob a guarda provisória de Administrador.
- Antes de abrir leituras ou escritas a Gestores/Organizadores, definir se basta controlar um dos lados da inscrição ou se determinadas operações exigem simultaneamente acesso à equipa e ao evento. Para criação, a política mais segura é validar ambos os âmbitos, ou manter a operação administrativa até decisão explícita.
- Não aceitar `equipaId` ou `eventoId` do corpo como prova de autorização.

### Jogos e chaveamento

- A tabela `jogos` referencia `evento_id`, `equipa_casa_id`, `equipa_fora_id` e `pavilhao_id`.
- Existe base técnica para verificar o âmbito do Organizador pelo evento e, quando a relação equipa/conta estiver aprovada, para verificar o âmbito do Gestor de Equipa.
- As operações CRUD em `/jogos` continuam reservadas ao Administrador pela guarda provisória.
- A leitura de `GET /eventos/{id}/chaveamento` está explicitamente na allowlist pública. Esta exposição deve ser confirmada como conteúdo desportivo público; se os resultados ou participantes de certos eventos forem privados, a allowlist precisa de uma regra de publicação/visibilidade por evento.
- O serviço de agendamento gera jogos a partir das inscrições e do evento, pelo que a operação de gerar/avançar rondas deve validar o âmbito do mesmo evento antes de permitir acesso a Organizadores.

### Estatísticas

- Cada registo estatístico referencia um jogador e uma equipa, mas o modelo de jogador ainda não tem associação persistente a conta ou equipa.
- A rota de leitura por jogador e a escrita de movimentos permanecem reservadas ao Administrador.
- Não permitir que um utilizador escolha livremente `jogadorId` ou `equipaId` para ler ou escrever estatísticas sem uma associação validada.

### Pavilhões e organizadores

- Pavilhões são recursos globais e não têm atualmente associação a uma conta ou a um evento como âmbito próprio. CRUD permanece administrativo.
- Eventos guardam `organizadorId`, mas este ID referencia um registo de domínio de Organizador, não uma conta autenticada. A autorização de evento usa a associação explícita `RoleScopes` com tipo `EVENTO`; não inferir identidade de conta a partir do `organizadorId`.
- A gestão de registos de Organizador permanece administrativa até existir uma ligação formal entre conta e registo de Organizador.

### Revisão da superfície pública

A allowlist de `BusinessRouteSecurity.kt` permite apenas GETs exatos de páginas/conteúdo público e leituras de eventos, chaveamentos e classificações. Métodos de escrita não são allowlisted. As rotas GET de inscrições, jogos CRUD, estatísticas, pavilhões, jogadores e patrocinadores continuam protegidas pela política provisória, salvo os caminhos públicos expressamente listados.

**Ação pendente:** validar com o responsável funcional se chaveamentos e classificações são sempre públicos ou se dependem do estado de publicação do evento. Até essa decisão, a regra atual é uma decisão técnica provisória, não uma política de privacidade confirmada.

### CI e estado da branch

- Run #88 (`43079080189`, commit `c5be2eb`): testes Gradle e build passaram.
- Run #89 (`43079408319`, commit `9f020ca`): testes Gradle e build passaram.
- PR #2 mantém-se como draft; não foi feito merge nem deploy.
- O bloqueio de arranque em produção continua ativo.

### Próxima sequência recomendada

1. Adicionar testes HTTP reais às rotas de equipa e evento para comprovar isolamento entre dois âmbitos distintos, não apenas testar a primitiva de autorização.
2. Adicionar testes HTTP/IDOR a jogos e ao endpoint de geração/avanço do chaveamento; preservar as regras de publicação das leituras públicas.
3. Decidir e testar a regra de autorização de inscrições (equipa, evento ou ambos) antes de alterar as rotas.
4. Resolver as relações de conta-jogador, conta-patrocinador e conta-organizador antes de abrir os respetivos recursos.
5. Manter CRUD de pavilhões e gestão global de entidades sob Administrador.


## Jogos: autorização por evento implementada (2026-11-11)

A rota `/jogos` deixou de depender da guarda global provisória e passou a aplicar autorização explícita por âmbito:

- Administrador mantém acesso global.
- Organizador só pode listar jogos dos eventos atribuídos ao seu papel `ORGANIZADOR`.
- Consulta individual, criação, atualização e remoção validam o âmbito do evento associado ao jogo.
- A atualização não pode transferir um jogo para outro evento.
- A criação/atualização valida também que o pavilhão corresponde ao pavilhão do evento. Para Torneios, valida ainda que as equipas estão inscritas nesse evento, evitando injeção de equipas de outro torneio. Eventos Amigáveis não são bloqueados por falta de inscrições, preservando a possibilidade de jogos amigáveis com equipas existentes.
- Utilizadores sem sessão recebem 401; contas sem papel/âmbito de Organizador recebem 403.

A leitura pública do chaveamento permanece na allowlist GET existente. A operação de geração/avanço existe em `POST /eventos/{id}/agendamento`, registada em `FrontendRotas.kt` (não em `Agendamento_Rotas.kt`). Esta operação deixou de herdar a restrição provisória exclusiva de Administrador e passou a validar sessão e âmbito: Administrador tem acesso global; Organizador necessita de âmbito `ORGANIZADOR / EVENTO / {id}`. A exceção à guarda provisória é limitada ao método POST e ao padrão exato desta rota; a autorização efetiva é executada no handler.

A rota devolve 401 sem sessão, 403 sem papel/âmbito, 404 se o evento não existir e 409 quando o torneio não permite gerar/avançar a ronda no estado atual.

**Limitação de validação:** a alteração tem testes unitários para garantir que apenas o POST exato é delegado ao handler, mas ainda precisa de testes HTTP/IDOR com dois Organizadores e dois eventos. Os testes devem comprovar geração permitida no evento atribuído, negação no evento alheio, negação sem sessão e acesso global do Administrador. Os testes HTTP/IDOR de jogos também continuam pendentes.


## Atualização da auditoria de rotas restantes (2026-11-11)

A matriz de inventário acima descreve a superfície funcional original. O estado de proteção foi entretanto alterado nas rotas indicadas nos aditamentos seguintes. Nesta revisão foram confirmados os pontos abaixo:

- **Agendamento:** existe um POST real `/eventos/{id}/agendamento` em `FrontendRotas.kt`; `Agendamento_Rotas.kt` contém apenas GET `/eventos/{id}/chaveamento`. Não são rotas duplicadas, porque têm métodos HTTP diferentes. O POST passou a exigir sessão e papel/âmbito `ORGANIZADOR / EVENTO / {id}`, ou Administrador global.
- **Inscrições:** cada inscrição tem referências explícitas a `equipaId` e `eventoId`. Isso permite validar ambos os âmbitos tecnicamente, mas não decide se criar/editar/remover uma inscrição pertence ao Gestor de Equipa, ao Organizador, ou exige aprovação de ambos. As rotas permanecem temporariamente reservadas ao Administrador. Não confiar nos IDs do corpo sem validar a relação entre equipa e evento.
- **Estatísticas:** os registos associam jogador e equipa, mas a identidade autenticada ainda não está ligada ao registo de jogador. As rotas permanecem temporariamente reservadas ao Administrador; não abrir leitura individual até existir associação conta-jogador e política de acesso ao histórico.
- **Pavilhões:** são entidades globais sem âmbito de conta/evento próprio. CRUD permanece sob Administrador. A rota GET por ID também permanece protegida pela política provisória, tal como a listagem.
- **Organizadores:** o `organizadorId` guardado nos eventos referencia uma entidade de domínio, não uma conta autenticada. Não associar automaticamente contas a organizadores por nome ou email. CRUD de organizadores permanece sob Administrador até existir ligação formal.
- **Classificações:** as rotas geral e por torneio estão na allowlist GET e expõem resultados calculados a partir dos jogos. Não foi encontrado um campo de publicação/visibilidade no modelo de evento que permita restringir automaticamente resultados ainda não publicados. A exposição pública continua a ser uma decisão provisória que precisa de validação funcional.
- **Interface web:** `/hub` e `/publico` são leituras públicas allowlisted. Páginas `/organizador`, `/gestor-equipa`, `/jogador` e `/patrocinadores`, bem como a operação de formulário `POST /patrocinadores/financiar`, continuam protegidas pela política provisória de Administrador.

### Decisões funcionais ainda necessárias

1. Definir se o Gestor de Equipa pode submeter e gerir inscrições da sua equipa, se o Organizador aprova, ou se a operação pertence apenas a um dos papéis.
2. Definir se classificações e chaveamentos são públicos antes, durante e depois de um evento, ou se dependem de estado de publicação.
3. Definir e implementar ligações explícitas conta-jogador, conta-patrocinador e conta-organizador antes de permitir acesso individual.
4. Manter pavilhões, gestão de organizadores e restantes entidades globais sob Administrador até haver uma necessidade funcional e um modelo de âmbito comprovados.

A aplicação continua impedida de arrancar com `APP_ENV=production`. A implementação incremental de permissões não equivale a validação integral de todas as rotas nem autoriza remover a barreira.
