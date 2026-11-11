# Matriz de segurança e autorização

> Estado auditado em 2026-11-11. Documento de análise do código atual; não representa uma política de permissões aprovada.

## Estado atual

- Não existe autenticação nem autorização nas rotas HTTP analisadas.
- A aplicação recusa o arranque quando `APP_ENV=production`, através de `ensureDeploymentEnvironmentIsSupported` em `src/main/kotlin/main.kt`.
- A proteção de arranque é uma barreira temporária, não um controlo de acesso por rota.
- Não remover essa barreira até existir autenticação, autorização, proteção CSRF quando aplicável e testes negativos de acesso.

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

## Decisões necessárias antes de implementar permissões

Não foram atribuídos papéis nem direitos neste documento. É necessário decidir, com base no funcionamento pretendido, pelo menos:

1. Quem administra a plataforma e pode gerir todas as entidades.
2. Quem cria/edita eventos e respetivos jogos, inscrições e pavilhões.
3. Se gestores de equipa podem gerir apenas a própria equipa e os seus jogadores/inscrições.
4. Se jogadores têm conta e podem consultar apenas os próprios dados.
5. Se patrocinadores podem submeter propostas/contratos ou apenas consultar informação.
6. Que dados e páginas são públicos sem sessão.

Para cada decisão, especificar também o âmbito (global ou por evento/equipa), operações permitidas e comportamento quando o recurso não pertence ao utilizador.

## Requisitos técnicos para a implementação seguinte

- Aplicar autenticação e autorização no servidor; esconder botões na interface não é proteção.
- Usar negação por omissão e verificar a autorização em cada operação e recurso.
- Impedir IDOR: validar que o utilizador autenticado tem acesso ao evento/equipa/jogador indicado no URL ou corpo.
- Se a sessão usar cookies, proteger operações de escrita contra CSRF e configurar cookies `HttpOnly`, `Secure` e `SameSite` adequadamente.
- Validar entradas e limitar campos/valores; não confiar em IDs enviados pelo cliente.
- Não devolver exceções internas nem dados pessoais desnecessários.
- Criar testes para não autenticado, autenticado sem permissão, acesso ao próprio recurso, acesso a recurso alheio e operações permitidas.
- Manter `APP_ENV=production` bloqueado até todos os critérios anteriores estarem implementados e validados.

## Critério para desbloquear produção

Só considerar remover a barreira de arranque depois de:
- matriz de papéis e permissões aprovada;
- todas as rotas classificadas e protegidas;
- testes automatizados positivos e negativos por grupo de rotas;
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
