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
.gradlew.bat test
.gradlew.bat run
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

A configuração da aplicação é carregada através dos recursos do projeto. Antes de executar num ambiente diferente do desenvolvimento, confirma as definições de base de dados e servidor.

## Estado

Repositório de um projeto académico. A documentação descreve a organização e as tecnologias identificadas no código; não representa uma certificação de que todas as funcionalidades foram testadas ou estão prontas para produção.

## Repositório

[github.com/13alves05/basket-hub](https://github.com/13alves05/basket-hub)
