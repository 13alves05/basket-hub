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
| POST | `/eventos/{id}/agendamento` | Também registada na API de agendamento; confirmar/remover o registo duplicado |

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
