# Basket Hub

Projeto académico desenvolvido no âmbito do curso de Desenvolvimento de Software. A aplicação disponibiliza uma plataforma web para gestão de atividades e entidades relacionadas com basquetebol.

## Nota sobre o repositório

Esta é uma versão distinta do Basket Hub que está nesta conta académica. **Não é o repositório do trabalho de grupo apresentado no ano letivo anterior.** O projeto de grupo está no repositório [bmgoliveira-UA/basket-hub](https://github.com/bmgoliveira-UA/basket-hub), que é a referência correta para esse trabalho. Mantém-se esta versão separada para não misturar histórico, código ou autoria entre os dois repositórios.

## Funcionalidades

A estrutura do código está organizada por áreas funcionais, incluindo:

- Gestão de equipas e jogadores.
- Gestão de jogos e inscrições.
- Organização de eventos e respetivos intervenientes.
- Gestão de pavilhões e patrocinadores.
- Consulta de classificações e estatísticas.

A aplicação utiliza páginas HTML renderizadas no servidor e disponibiliza funcionalidades de backend através de Kotlin e Ktor.

## Tecnologias

- **Linguagem:** Kotlin
- **Servidor:** Ktor com Netty
- **Apresentação:** Thymeleaf
- **Persistência:** Exposed e R2DBC
- **Base de dados:** H2
- **Serialização:** kotlinx.serialization
- **Comunicação HTTP:** Content Negotiation e CORS
- **Build:** Gradle
- **JVM:** Java 21

## Executar localmente

É necessário ter um JDK compatível com Java 21.

No Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

Em Linux ou macOS:

```bash
./gradlew test
./gradlew run
```

Para gerar o artefacto de build:

```bash
./gradlew build
```

A aplicação usa H2 local por omissão em desenvolvimento. A configuração da ligação está em `src/main/kotlin/DBUtils/DBUtils.kt`.

## Configuração da base de dados

Em desenvolvimento, se não definires variáveis, a aplicação mantém a configuração H2 local existente (`./h2`, utilizador `root`, palavra-passe vazia).

Em produção, a configuração da base de dados exige:

- `BASKET_HUB_DB_URL`: URL R2DBC da base de dados H2.
- `BASKET_HUB_DB_USER`: utilizador da base de dados.
- `BASKET_HUB_DB_PASSWORD`: palavra-passe não vazia.

Não coloques credenciais no repositório nem em ficheiros versionados. Guarda-as no gestor de segredos do ambiente de alojamento. O projeto inclui atualmente o driver H2; não assumes compatibilidade com PostgreSQL ou outro motor sem adicionar e testar o respetivo driver.

## Limitações e segurança

**Esta versão não está pronta para produção e não deve ser exposta como serviço público nem utilizada para gerir dados reais.**

- As rotas de criação, alteração e remoção não exigem autenticação nem verificam permissões de utilizador.
- A versão atual recusa intencionalmente arrancar quando `APP_ENV=production`. Esta proteção temporária evita publicar acidentalmente uma aplicação sem autenticação e autorização. Será removida apenas depois de as rotas privadas estarem protegidas e testadas.
- O CORS não aceita origens externas por omissão. Se a interface for alojada noutro domínio, configura explicitamente `CORS_ALLOWED_HOSTS` com os hosts autorizados, separados por vírgulas.
- Algumas rotas ainda devolvem mensagens de exceção ao cliente. Essas respostas devem ser substituídas por mensagens controladas antes de uma utilização pública.
- Existe um teste de servidor básico e testes de proteção do arranque, mas não uma suite completa de testes de integração para os módulos e operações CRUD.

A exigência de credenciais externas e a proteção do arranque em produção reduzem riscos de configuração, mas **não substituem autenticação e autorização**. Antes de qualquer publicação, continua a ser necessário implementar contas e sessões, proteger todas as rotas privadas, validar os dados recebidos, controlar erros e executar testes de segurança.

## Estado

Repositório de um projeto académico em desenvolvimento. A documentação descreve a organização e as tecnologias identificadas no código; não representa uma certificação de que todas as funcionalidades foram testadas ou estão prontas para produção.

## Repositório

[github.com/13alves05/basket-hub](https://github.com/13alves05/basket-hub)
