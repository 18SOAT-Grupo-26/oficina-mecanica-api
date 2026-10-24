# Oficina Mecânica API

## Objetivo do projeto

API para gestão de oficina mecânica.

## Descrição resumida da solução

Funcionalidades disponíveis até o momento:

- Autenticação de usuários com JWT (cadastro, login e validação de token)
- CRUD de clientes, com validação de CPF e CNPJ (restrito ao perfil `ADMINISTRATOR`)
- Cadastro de peças e insumos

## Arquitetura utilizada

Arquitetura hexagonal (ports and adapters):

- `core` concentra o domínio, os casos de uso e as portas de entrada (`port/in`) e saída (`port/out`)
- `controller` é o adapter de entrada (REST)
- `infrastructure` é o adapter de saída (persistência com JPA) e as configurações de segurança e documentação

A decisão, as camadas, a regra de dependência e as alternativas avaliadas estão na [ADR 001](docs/adr/0001-monolito-em-camadas.md). Todas as decisões de arquitetura estão no [índice de ADRs](docs/adr/README.md).

## Tecnologias utilizadas

- Java 25
- Spring Boot 4 (Web MVC, Data JPA, Validation, Security, Actuator)
- PostgreSQL 18
- JWT (jjwt)
- MapStruct e Lombok
- springdoc-openapi (Swagger UI)
- Micrometer com Prometheus
- JUnit 5 e Mockito
- Docker e Docker Compose

## Pré-requisitos

- [Git](https://git-scm.com/downloads)
- [Docker](https://docs.docker.com/get-docker/)

## Instruções para execução

```bash
git clone git@github.com:18SOAT-Grupo-26/oficina-mecanica-api.git
cd oficina-mecanica-api
docker compose up -d --build
```

Isso sobe o Postgres e a API em containers. A API só inicia depois que o banco estiver saudável. As configurações de desenvolvimento já estão definidas no `docker-compose.yml`.

| Serviço | Container | Porta |
|---|---|---|
| `db` | `oficina-mecanica-db` | 5432 |
| `api` | `oficina-mecanica-api` | 8081 |

Depois de alterar o código, rode `docker compose up -d --build` novamente para reconstruir a imagem.

Endereços:

- API: http://localhost:8081/api/v1
- Health check: http://localhost:8081/api/v1/actuator/health

Comandos úteis:

```bash
docker compose logs -f api   # acompanhar logs da API
docker compose ps            # status dos containers
docker compose down          # parar os containers
docker compose down -v       # parar e apagar os dados do banco
```

## Instruções para execução dos testes

Os testes não dependem do banco. Para rodá-los sem instalar Java ou Maven localmente:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3-amazoncorretto-25-alpine mvn test
```

Com Java 25 e Maven instalados, basta rodar `mvn test`.

## Qualidade de código e cobertura (Sonar + JaCoCo)

`mvn verify` roda os testes e gera o relatório de cobertura do JaCoCo em `target/site/jacoco/index.html` (o XML em `target/site/jacoco/jacoco.xml` é o que o Sonar lê).

### SonarCloud (automático)

O workflow `.github/workflows/ci.yml` roda build e testes em todo push e PR para `main` e `develop`. Quando o secret `SONAR_TOKEN` está configurado no repositório, o workflow também envia a análise ao SonarCloud (organização `18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`). O resultado e o quality gate aparecem no próprio PR.

### SonarQube local (antes de abrir o PR)

```bash
docker compose -f docker-compose.sonar.yml up -d   # SonarQube em http://localhost:9000 (login inicial admin/admin)
```

No SonarQube, gere um token em *My Account > Security* e rode a análise:

```bash
mvn verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<seu-token>
```

O token é pessoal: não o commite nem o coloque em arquivos do projeto.

### Meta de cobertura nos domínios críticos

O `mvn verify` falha se algum pacote de domínio ou de caso de uso de `cliente` ou `veiculo` ficar abaixo de **80% de linhas cobertas** (regra `check` do JaCoCo no `pom.xml`, propriedade `jacoco.minimo.linhas`). Novos domínios críticos (OS, orçamento, estoque) entram na regra conforme ganham testes.

### Pré-validação antes do push

O hook `.githooks/pre-push` roda `mvn clean verify` (testes + cobertura) antes de cada `git push` e bloqueia o envio se algo falhar. Ative uma vez por clone:

```bash
git config core.hooksPath .githooks
```

- Leva cerca de 10 a 60 segundos; o log completo fica em `.git/pre-push.log`.
- Usa `mvn`; sem Maven instalado, usa a imagem Docker `maven:3-amazoncorretto-25-alpine`.
- Em emergência, `git push --no-verify` pula o hook (o CI roda a mesma validação no PR).

Para ver os problemas do Sonar enquanto programa, instale a extensão **SonarQube for IDE** e conecte-a ao SonarCloud (*connected mode*, organização `18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`). Ela aplica as mesmas regras do quality gate direto no editor.

## Acesso ao Swagger / documentação da API

Com a aplicação rodando, acesse http://localhost:8081/api/v1/documentation.

## Decisão e justificativa do banco de dados escolhido

Usamos o **PostgreSQL 18**, rodando em container via Docker Compose. Os dados da oficina são relacionais (a OS reúne cliente, veículo, serviços e peças), e regras como a reserva de estoque precisam de transações ACID e de integridade garantida pelo banco (chaves estrangeiras e constraints `UNIQUE` para CPF/CNPJ e placa). Pelo teorema PACELC, precisamos de um banco que priorize consistência (CP/EC). Como a aplicação é um monólito com um único banco, não precisamos de um banco distribuído. Além disso, o PostgreSQL é open source e o time já tem familiaridade com ele. Os detalhes e as alternativas avaliadas estão na [ADR 002](docs/adr/0002-escolha-do-banco-de-dados.md).

## Usuários, credenciais de demonstração e autenticação

A autenticação usa JWT. O fluxo é:

1. Cadastrar um usuário em `POST /api/v1/auth/register`
2. Fazer login em `POST /api/v1/auth/login` informando `identifier` e `password`; a resposta traz o token
3. Enviar o token nas demais requisições no header `Authorization: Bearer <token>`

Perfis existentes: `ADMINISTRATOR`, `MANAGER` e `OPERATOR`.

Credenciais de demonstração: _a definir._

## Estrutura dos principais diretórios

```
src/main/java/br/com/fiap/oficinamecanicaapi
├── controller          # endpoints REST (admin, auth, cliente)
├── core
│   ├── domain          # modelos e entidades de domínio
│   ├── port
│   │   ├── in          # portas de entrada (casos de uso)
│   │   └── out         # portas de saída (persistência)
│   └── usecase         # implementação dos casos de uso
├── infrastructure
│   ├── config          # segurança (JWT) e documentação (OpenAPI)
│   └── persistence     # repositórios e adapters JPA
└── shared              # exceções, handler global e utilitários
src/test/java           # testes unitários e de controller
```

## Documentação DDD

_A definir._

## Vídeo da entrega

_A definir._
