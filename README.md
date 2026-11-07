# Basket Hub

Projeto académico desenvolvido no âmbito do curso de Desenvolvimento de Software. A aplicação disponibiliza uma plataforma web para gestão de atividades e entidades relacionadas com basquetebol.

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

## Estrutura do projeto

O código está organizado em pacotes por domínio funcional, facilitando a separação das responsabilidades:

- `Agendamento`
- `Classificacao`
- `Equipa`
- `Estatisticas`
- `Evento`
- `Inscricao`
- `Jogador`
- `Jogo`
- `Organizador`
- `Patrocinador`
- `Pavilhao`
- `DBUtils`

Os ficheiros de configuração do servidor e a integração com a base de dados encontram-se na estrutura principal de `src/main/kotlin`.

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

A configuração do servidor (incluindo a porta `8080`) está em `src/main/resources/application.yaml`. A ligação à base de dados H2 está definida em `src/main/kotlin/DBUtils/DBUtils.kt` e utiliza o ficheiro local `./h2`. Antes de executar noutro ambiente, revê estas definições.

## Limitações e segurança

Este repositório corresponde a um projeto académico e não deve ser exposto como serviço público nem utilizado para gerir dados reais.

- As rotas de criação, alteração e remoção não exigem autenticação nem verificam permissões de utilizador.
- A configuração CORS utiliza atualmente `anyHost()`, permitindo pedidos de origens externas.
- A ligação H2 está configurada diretamente no código, com o utilizador `root` e palavra-passe vazia. A base de dados é local e não está preparada para alojamento público.
- Algumas rotas devolvem mensagens de exceção ao cliente. Essas respostas devem ser substituídas por mensagens controladas antes de uma utilização pública.
- Existe um teste de servidor básico, mas não uma suite completa de testes de integração para os módulos e operações CRUD.

Antes de qualquer publicação, seria necessário implementar autenticação e autorização nas operações de escrita, restringir CORS às origens necessárias, retirar a configuração da base de dados do código e validar os dados recebidos em todos os endpoints.

## Estado

Repositório de um projeto académico. A documentação descreve a organização e as tecnologias identificadas no código; não representa uma certificação de que todas as funcionalidades foram testadas ou estão prontas para produção.

## Repositório

[github.com/13alves05/basket-hub](https://github.com/13alves05/basket-hub)
