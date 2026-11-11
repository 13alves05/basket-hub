# Modelo de contas e papéis

> Estado: decisão de contas com múltiplos papéis confirmada pelo responsável em 2026-11-11. Os nomes dos papéis e a matriz funcional abaixo são a proposta inicial de implementação; os âmbitos e regras de negócio continuam sujeitos a validação com o domínio real.

## Decisão confirmada

- Uma conta pode ter vários papéis em simultâneo.
- Os papéis são atribuídos independentemente; remover um papel não remove os restantes.
- A autorização é calculada pela união das permissões dos papéis ativos, mas cada permissão continua limitada ao âmbito dos recursos atribuídos à conta.
- Um papel não concede acesso global só por coexistir com outro papel.
- O papel Administrador é global e reservado à administração da plataforma. A atribuição e remoção de papéis administrativos deve ser auditada.

Exemplos válidos:
- Gestor de Equipa + Jogador.
- Organizador + Gestor de Equipa.
- Administrador + outro papel, apenas quando existir uma necessidade operacional explícita.

## Papéis propostos

| Papel | Âmbito | Permissões propostas |
|---|---|---|
| Administrador | Plataforma inteira | Gerir contas, atribuir/remover papéis e gerir todas as entidades. Ações administrativas auditadas. |
| Organizador | Eventos que lhe foram atribuídos | Criar/gerir os eventos atribuídos, respetivos jogos, inscrições, pavilhões associados e agendamentos, dentro das regras do evento. |
| Gestor de Equipa | Equipas que lhe foram atribuídas | Gerir a própria equipa, os seus jogadores e inscrições; não pode alterar equipas alheias nem administrar eventos globalmente. |
| Jogador | Perfil de jogador associado à conta | Consultar e atualizar apenas os campos permitidos do próprio perfil; consultar os próprios jogos, inscrições e estatísticas. |
| Patrocinador | Perfil/contratos associados à conta | Gerir o próprio perfil e submeter/consultar propostas e contratos próprios; sem acesso a dados privados de outros patrocinadores. |
| Visitante | Informação explicitamente pública | Consultar eventos, jogos, classificações e informação publicada como pública; sem operações de escrita. |

## Modelo de atribuição recomendado

Não basta guardar uma lista global de papéis no utilizador. Os papéis de gestão devem ter um âmbito explícito:

- Administrador: atribuição global.
- Organizador: associação entre conta e evento(s) geridos.
- Gestor de Equipa: associação entre conta e equipa(s) geridas.
- Jogador: associação entre conta e registo de jogador.
- Patrocinador: associação entre conta e registo de patrocinador.

A mesma conta pode ter várias associações e vários papéis. Exemplo: uma conta pode gerir a Equipa A, ser jogador da Equipa B e organizar o Evento C. Cada operação deve validar o papel e a associação relevantes para o recurso pedido.

## Estrutura de dados sugerida

- `accounts`: identidade de autenticação (identificador, email normalizado, hash de palavra-passe, estado, datas).
- `account_roles`: relação muitos-para-muitos entre conta e papel.
- `role_scopes` (ou tabelas de associação específicas): liga a atribuição de papel ao evento, equipa, jogador ou patrocinador correspondente.
- `auth_sessions`: sessões revogáveis, se for adotada autenticação por sessão.
- `security_audit_log`: registo de alterações de papéis e ações administrativas relevantes.

As tabelas finais e as migrações devem respeitar o motor e o padrão de persistência realmente usados pelo projeto. Não reutilizar a tabela de exemplo `Users`/ `ExposedUserService` como sistema de contas: atualmente contém apenas nome e idade e não constitui autenticação.

## Regras de autorização

1. Negar por omissão.
2. Validar autenticação e autorização no servidor em todas as rotas, APIs e operações de serviço.
3. Validar o âmbito do recurso em cada pedido; IDs enviados pelo cliente nunca provam propriedade.
4. Não permitir que um utilizador atribua papéis a si próprio. Apenas Administrador pode gerir papéis.
5. Nunca permitir que um Administrador seja criado por registo público.
6. Registar atribuições, remoções e alterações de âmbito de papéis.
7. Responder com 401 para ausência de autenticação e 403 (ou 404 quando necessário para não revelar a existência do recurso) para falta de autorização.
8. Se forem usadas sessões por cookie, ativar proteção CSRF e cookies HttpOnly, Secure e SameSite adequados.
9. Testar contas com papéis múltiplos, incluindo a garantia de que a combinação não ultrapassa o âmbito de cada papel.

## Questões ainda não decididas

- Como será criado o primeiro Administrador (provisionamento manual seguro, variável/segredo de bootstrap de uso único, ou outro processo).
- Como será feita a recuperação de conta e verificação de email.
- Se o registo de novas contas será aberto, por convite ou gerido por Administrador.
- Que campos de perfil podem ser alterados pelo próprio Jogador/Gestor/Patrocinador.
- Regras específicas para criação, aprovação e assinatura de contratos de patrocínio.

Não implementar registo público nem bootstrap administrativo até a primeira decisão estar definida. Manter `APP_ENV=production` bloqueado.

## Estado técnico observado

- Ktor 3.5.0, Kotlin/JVM 21 e Exposed R2DBC.
- O projeto tem um `ExposedUserService` de exemplo, com campos `name` e `age`; não está integrado como autenticação e não tem palavra-passe, email, papéis ou sessões.
- Não foi encontrada uma implementação de autenticação existente.
- A base de dados é inicializada em `DBUtils/DatabaseCreation.kt`; qualquer nova tabela de contas deverá ser inicializada de forma explícita e compatível com o padrão atual.
