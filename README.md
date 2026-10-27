# Oficina Mecânica API

[![CI](https://github.com/18SOAT-Grupo-26/oficina-mecanica-api/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/18SOAT-Grupo-26/oficina-mecanica-api/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=18SOAT-Grupo-26_oficina-mecanica-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=18SOAT-Grupo-26_oficina-mecanica-api)

## Objetivo do projeto

API para gestão de oficina mecânica.

## Descrição resumida da solução

Funcionalidades disponíveis até o momento:

- Autenticação de usuários com JWT (cadastro, login e validação de token)
- CRUD de clientes, com validação de CPF/CNPJ e busca por documento (perfil `ADMINISTRATOR`)
- CRUD de veículos, com validação de placa e busca por placa (perfil `ADMINISTRATOR`)
- CRUD de serviços (perfil `ADMINISTRATOR`)
- CRUD de peças e insumos (perfis `MANAGER` e `ADMINISTRATOR`)
- Em desenvolvimento: ordem de serviço e acompanhamento da OS pelo cliente

## Arquitetura utilizada

Arquitetura hexagonal (ports and adapters):

- `core` concentra o domínio, os casos de uso e as portas de entrada (`port/in`) e saída (`port/out`)
- `controller` é o adapter de entrada (REST)
- `infrastructure` é o adapter de saída (persistência com JPA) e as configurações de segurança e documentação

A decisão, as camadas, a regra de dependência e as alternativas avaliadas estão
na [ADR 001](docs/adr/0001-monolito-em-camadas.md). Todas as decisões de arquitetura estão
no [índice de ADRs](docs/adr/README.md).

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
- Flyway (migrations)
- Prometheus e Grafana (observabilidade local)
- JaCoCo, SonarCloud e SonarQube (qualidade e cobertura)

## Pré-requisitos

- [Git](https://git-scm.com/downloads)
- [Docker](https://docs.docker.com/get-docker/) com Docker Compose v2 (comando `docker compose`)
- Memória para o Docker: ~2 GB para API + banco; ~4 GB livres se for subir também o SonarQube (no Windows, ajuste em
  *Docker Desktop > Settings > Resources* ou no `.wslconfig`)
- Opcional, para rodar fora do Docker: Java 25 e Maven 3.9+

## Instruções para execução

```bash
git clone git@github.com:18SOAT-Grupo-26/oficina-mecanica-api.git
# ou, sem chave SSH: git clone https://github.com/18SOAT-Grupo-26/oficina-mecanica-api.git
cd oficina-mecanica-api
cp .env.example.example .env.example          # PowerShell: Copy-Item .env.example.example .env.example
docker compose up -d --build
```

Isso sobe o Postgres e a API. A API só inicia depois que o banco estiver saudável. Os serviços de apoio sobem sob
demanda, via *profiles*:

```bash
docker compose --profile observability up -d   # + Prometheus e Grafana
docker compose --profile sonar up -d           # + SonarQube local
```

| Serviço      | Container                     | Porta | Profile         |
|--------------|-------------------------------|-------|-----------------|
| `db`         | `oficina-mecanica-db`         | 5432  | (padrão)        |
| `api`        | `oficina-mecanica-api`        | 8081  | (padrão)        |
| `prometheus` | `oficina-mecanica-prometheus` | 9090  | `observability` |
| `grafana`    | `oficina-mecanica-grafana`    | 3000  | `observability` |
| `sonarqube`  | `oficina-mecanica-sonarqube`  | 9000  | `sonar`         |

Depois de alterar o código, rode `docker compose up -d --build` novamente para reconstruir a imagem.

Endereços:

- API: http://localhost:8081/api/v1
- Health check: http://localhost:8081/api/v1/actuator/health
- Métricas (formato Prometheus): http://localhost:8081/api/v1/actuator/prometheus
- Prometheus: http://localhost:9090 (profile `observability`)
- Grafana: http://localhost:3000, login `admin` / valor de `GRAFANA_ADMIN_PASSWORD` (profile `observability`)
- SonarQube: http://localhost:9000, login inicial `admin` / `admin`, com troca de senha no primeiro acesso (profile `sonar`)

Comandos úteis:

```bash
docker compose logs -f api                 # acompanhar logs da API
docker compose ps                          # status dos containers
docker compose --profile "*" down          # parar todos os containers, inclusive os de profile
docker compose --profile "*" down -v       # parar e APAGAR os volumes: banco, SonarQube, Prometheus e Grafana
```

> Para apagar só os dados do banco: `docker compose down` e depois `docker volume rm oficina-mecanica-api_pgdata` (o
> prefixo é o nome da pasta do projeto; confira com `docker volume ls`).

### Banco de dados e migrations (Flyway)

O schema é criado e evoluído pelo **Flyway**, que roda sozinho na subida da API:

- o container `db` cria o banco `oficina` na primeira subida;
- a API aplica as migrations de `src/main/resources/db/migration` que ainda não rodaram: tabelas
  (`V1__schema_inicial.sql`) e perfis `ADMINISTRATOR`, `MANAGER` e `OPERATOR` (`V2__perfis_iniciais.sql`);
- o Hibernate roda com `ddl-auto: validate`: só confere se as entidades batem com as tabelas. A API não sobe se alguma
  entidade divergir do schema.

Para mudar o banco (nova tabela, coluna, índice...), crie um novo arquivo `V<próximo número>__descricao.sql` junto com a
alteração da entidade. **Nunca edite uma migration já aplicada**: o Flyway valida o checksum e a API deixa de subir.

Possíveis problemas:

- **"Found non-empty schema(s) ... without schema history table"**: o volume foi criado antes das migrations e já tem
- **Portas 9000, 9090 ou 3000 em uso**: troque a porta publicada do serviço correspondente no `docker-compose.yml` (ex.:
  `"9001:9000"`).
- **SonarQube reinicia ou não abre**: falta memória para o Docker; veja os pré-requisitos.
- **`required variable API_SECRET is missing`**: falta o arquivo `.env`; rode `cp .env.example .env`.
- **Rodando a API fora do Docker (IDE ou `mvn spring-boot:run`)**: suba só o banco com `docker compose up -d db` (ele já
  cria o banco `oficina`) e defina as variáveis da seção [Variáveis de ambiente](#variáveis-de-ambiente):

  ```powershell
  # PowerShell
  $env:DATASOURCE_USERNAME="postgres"; $env:DATASOURCE_PASSWORD="postgres"; $env:API_SECRET="<segredo>"
  mvn spring-boot:run
  ```

  ```bash
  # bash / Git Bash
  export DATASOURCE_USERNAME=postgres DATASOURCE_PASSWORD=postgres API_SECRET="<segredo>"
  mvn spring-boot:run
  ```

## Variáveis de ambiente

| Variável                 | Padrão                                     | Obrigatória | Descrição                                                         |
|--------------------------|--------------------------------------------|-------------|-------------------------------------------------------------------|
| `DATASOURCE_HOST`        | `jdbc:postgresql://localhost:5432/oficina` | não         | URL JDBC completa do banco                                        |
| `DATASOURCE_USERNAME`    | vazio                                      | sim         | usuário do banco                                                  |
| `DATASOURCE_PASSWORD`    | —                                          | sim         | senha do banco                                                    |
| `API_SECRET`             | —                                          | sim         | chave de assinatura do JWT, em Base64 (`openssl rand -base64 64`) |
| `CORS_ALLOWED_ORIGINS`   | vazio                                      | não         | origens do front que podem chamar a API pelo navegador, separadas por vírgula (ex.: `http://localhost:3000`); vazio = nenhuma |
| `SERVER_PORT`            | `8081`                                     | não         | porta HTTP da API                                                 |
| `APPLICATION_PREFIX`     | `api/v1`                                   | não         | prefixo (context-path) das rotas                                  |
| `APPLICATION_NAME`       | `oficina-mecanica-api`                     | não         | nome da aplicação                                                 |
| `LOG_PATH`               | `/tmp/logs`                                | não         | diretório dos logs                                                |
| `GRAFANA_ADMIN_PASSWORD` | `admin`                                    | não         | senha do admin do Grafana (só no compose)                         |

No Docker Compose, as variáveis da API já estão no `docker-compose.yml`; só `API_SECRET` e `GRAFANA_ADMIN_PASSWORD` vêm
do `.env`.

## Instruções para execução dos testes

Os testes não dependem do banco. Para rodá-los sem instalar Java ou Maven localmente:

```bash
# bash / Git Bash
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app maven:3-amazoncorretto-25-alpine mvn test
```

```powershell
# PowerShell
docker run --rm -v "${PWD}:/app" -v "$env:USERPROFILE\.m2:/root/.m2" -w /app maven:3-amazoncorretto-25-alpine mvn test
```

O volume `.m2` guarda as dependências entre execuções. Com Java 25 e Maven instalados, basta rodar `mvn test`.

## Qualidade de código e cobertura (Sonar + JaCoCo)

`mvn verify` roda os testes e gera o relatório de cobertura do JaCoCo em `target/site/jacoco/index.html` (o XML em
`target/site/jacoco/jacoco.xml` é o que o Sonar lê).

### SonarCloud (automático)

O workflow `.github/workflows/ci.yml` roda build e testes em todo push e PR para `main` e `develop`. Quando o secret
`SONAR_TOKEN` está configurado no repositório, o workflow também envia a análise ao SonarCloud (organização
`18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`). O resultado e o quality gate aparecem no próprio PR.

### SonarQube local (antes de abrir o PR)

```bash
docker compose --profile sonar up -d   # SonarQube em http://localhost:9000 (login inicial admin/admin)
```

No SonarQube, gere um token em *My Account > Security* e rode a análise:

```bash
mvn verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<seu-token>
```

Os dados do SonarQube (token, projeto) ficam em volumes e sobrevivem a `docker compose down`; só são apagados com
`down -v`.

### Meta de cobertura nos domínios críticos

O `mvn verify` falha se algum pacote de domínio ou de caso de uso de `cliente` ou `veiculo` ficar abaixo de **80% de
linhas cobertas** (regra `check` do JaCoCo no `pom.xml`, propriedade `jacoco.minimo.linhas`). Novos domínios críticos
(OS, orçamento, estoque) entram na regra conforme ganham testes.

### Pré-validação antes do push

O hook `.githooks/pre-push` roda `mvn clean verify` (testes + cobertura) antes de cada `git push` e bloqueia o envio se
algo falhar. Ative uma vez por clone:

```bash
git config core.hooksPath .githooks
```

- Leva cerca de 10 a 60 segundos; o log completo fica em `.git/pre-push.log`.
- Usa `mvn`; sem Maven instalado, usa a imagem Docker `maven:3-amazoncorretto-25-alpine`. No Windows, o hook roda pelo
  Git Bash (já incluído no Git for Windows).
- Em emergência, `git push --no-verify` pula o hook (o CI roda a mesma validação no PR).

Para ver os problemas do Sonar enquanto programa, instale a extensão **SonarQube for IDE** e conecte-a ao SonarCloud
(*connected mode*, organização `18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`). Ela aplica as mesmas
regras do quality gate direto no editor.

## Acesso ao Swagger / documentação da API

- Swagger UI: http://localhost:8081/api/v1/documentation
- OpenAPI (JSON): http://localhost:8081/api/v1/api-docs-personalized

Para chamar endpoints protegidos pelo Swagger, faça login em `POST /auth/login`, clique em **Authorize** e cole o token.

## Observabilidade (Prometheus e Grafana)

```bash
docker compose --profile observability up -d
```

- A API expõe métricas Micrometer em `/api/v1/actuator/prometheus` (sem autenticação).
- O Prometheus (http://localhost:9090) coleta a API a cada 15 s; a configuração fica em `prometheus/prometheus.yaml`. Em
  *Status > Targets*, o job `oficina-mecanica-api` deve aparecer como `UP`.
- O Grafana (http://localhost:3000) já sobe com o Prometheus configurado como fonte de dados
  (`grafana/provisioning/datasources`). Para um painel pronto de JVM/Spring, importe o dashboard de ID **4701** (*JVM
  Micrometer*) em *Dashboards > New > Import*.

### Collection do Postman

A collection [`docs/postman/oficina-mecanica-api.postman_collection.json`](docs/postman/oficina-mecanica-api.postman_collection.json) traz todos os endpoints implementados (auth, peças e insumos, serviços, clientes, veículos e actuator). Para usar:

1. No Postman, **Import** → selecione o arquivo.
2. Rode **Auth → Cadastrar usuário** e depois **Auth → Login**. O token fica salvo na variável `token` e é enviado como Bearer nas demais requisições.
3. Os cadastros salvam os ids criados (`pecaInsumoId`, `servicoId`, `clienteId`, `veiculoId`) para as requisições seguintes.

Também dá para executar a collection inteira no **Runner**, na ordem das pastas. CPF, placa, nomes e username são gerados a cada execução, então ela pode ser rodada várias vezes sem violar as constraints `UNIQUE`. Para apontar para outro ambiente, altere a variável `baseUrl` (padrão: `http://localhost:8081/api/v1`).

## Decisão e justificativa do banco de dados escolhido

Usamos o **PostgreSQL 18**, rodando em container via Docker Compose. Os dados da oficina são relacionais (a OS reúne
cliente, veículo, serviços e peças), e regras como a reserva de estoque precisam de transações ACID e de integridade
garantida pelo banco (chaves estrangeiras e constraints `UNIQUE` para CPF/CNPJ e placa). Pelo teorema PACELC, precisamos
de um banco que priorize consistência (CP/EC). Como a aplicação é um monólito com um único banco, não precisamos de um
banco distribuído. Além disso, o PostgreSQL é open source e o time já tem familiaridade com ele. Os detalhes e as
alternativas avaliadas estão na [ADR 002](docs/adr/0002-escolha-do-banco-de-dados.md).

## Usuários, credenciais de demonstração e autenticação

A autenticação usa JWT:

| Método | Rota                    | Acesso                     |
|--------|-------------------------|----------------------------|
| `POST` | `/api/v1/auth/cadastro` | público                    |
| `POST` | `/api/v1/auth/login`    | público                    |
| `GET`  | `/api/v1/auth/validate` | header `Authorization`     |
| `*`    | `/api/v1/clientes`      | `ADMINISTRATOR`            |
| `*`    | `/api/v1/veiculos`      | `ADMINISTRATOR`            |
| `*`    | `/api/v1/servico`       | `ADMINISTRATOR`            |
| `*`    | `/api/v1/insumos`       | `MANAGER`, `ADMINISTRATOR` |

> **Limitação conhecida:** `POST /auth/cadastro` é público e aceita qualquer `perfil`, inclusive `ADMINISTRATOR`. Use
> apenas em ambiente local até a regra de criação de administradores ser restringida.

1. Cadastrar um usuário (o `documento` aceita CPF ou CNPJ, com ou sem máscara):

```bash
curl -X POST http://localhost:8081/api/v1/auth/cadastro -H "Content-Type: application/json" -d '{
  "identidade": {
    "nomeCompleto": "Ana Administradora",
    "primeiroNome": "Ana",
    "documento": "52998224725",
    "rg": "123456789",
    "dataNascimento": "1990-05-10",
    "contato": { "telefone": "11999999999", "email": "ana@oficina.com.br" },
    "endereco": { "logradouro": "Av. Paulista", "numero": "1000", "bairro": "Bela Vista",
                  "cidade": "São Paulo", "estado": "SP", "cep": "01310100", "complemento": "" }
  },
  "usuario": { "nomeUsuario": "admin", "senha": "Admin@123", "trocaSenhaObrigatoria": false, "perfil": "ADMINISTRATOR" }
}'
```

A senha precisa ter de 8 a 64 caracteres, com maiúscula, minúscula, dígito e um destes especiais:
`!@#$%^&*()-_=+[]{}<>?/,.:|~`.

2. Fazer login (o `identificador` aceita nome de usuário ou e-mail); a resposta traz o token:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{ "identificador": "admin", "senha": "Admin@123" }'
```

3. Enviar o token nas demais requisições no header `Authorization: Bearer <token>`.

Perfis existentes: `ADMINISTRATOR`, `MANAGER` e `OPERATOR`, criados pela migration `V2__perfis_iniciais.sql`. Nenhum usuário é criado automaticamente: o primeiro administrador é cadastrado pelo próprio `POST /api/v1/auth/cadastro` com `"perfil": "ADMINISTRATOR"`.

Credenciais de demonstração: `admin` / `Admin@123`, depois de rodar o cadastro acima.

## Estrutura dos principais diretórios

```
src/main/java/br/com/fiap/oficinamecanicaapi
├── controller          # endpoints REST: admin (APIs internas da oficina), auth, cliente (acompanhamento de OS pelo cliente final)
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
