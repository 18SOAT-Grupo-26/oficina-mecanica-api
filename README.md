# Oficina Mecânica API

[![CI](https://github.com/18SOAT-Grupo-26/oficina-mecanica-api/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/18SOAT-Grupo-26/oficina-mecanica-api/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=18SOAT-Grupo-26_oficina-mecanica-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=18SOAT-Grupo-26_oficina-mecanica-api)

## Objetivo do projeto

MVP do back-end do Sistema Integrado de Atendimento e Execução de Serviços de uma oficina mecânica de médio porte, que
hoje controla o atendimento com anotações manuais e planilhas. A API substitui esse controle e ataca os problemas
levantados pela oficina:

| Problema da oficina | Como a API resolve |
|---|---|
| Erros na priorização dos atendimentos | cada atendimento vira uma ordem de serviço com status, data de abertura e histórico, acompanhada pela equipe |
| Falhas no controle de peças e insumos | estoque com quantidade e reservado: o orçamento enviado reserva as peças, a aprovação dá baixa e a rejeição devolve |
| Dificuldade em acompanhar o status dos serviços | status com transições controladas e consulta do andamento pelo próprio cliente, em `/minhas-ordens` |
| Perda de histórico de clientes e veículos | cadastro de clientes (CPF/CNPJ validado) e veículos (placa validada), com o histórico de status de cada OS |
| Ineficiência no fluxo de orçamentos e autorizações | orçamento gerado a partir dos serviços e peças da OS, com envio, aprovação e rejeição registrados |

O projeto aplica Domain-Driven Design e segue boas práticas de qualidade de software (testes, cobertura mínima de 80% nos
domínios críticos, análise estática) e de segurança (JWT, perfis de acesso, análise de vulnerabilidades).

## Descrição resumida da solução

Funcionalidades disponíveis até o momento:

- Autenticação com JWT (login, validação de token e cadastro de usuários pelo `ADMIN`), com bloqueio temporário do
  login após falhas seguidas (429)
- CRUD de clientes, com validação de CPF/CNPJ e busca por documento (`ADMIN` e `FUNCIONARIO`; exclusão só `ADMIN`)
- CRUD de veículos, com validação de placa e busca por placa (`ADMIN` e `FUNCIONARIO`; exclusão só `ADMIN`)
- CRUD de peças e insumos, com saldo em estoque e quantidade reservada (`ADMIN` e `FUNCIONARIO`; exclusão só `ADMIN`)
- CRUD de serviços (somente `ADMIN`)
- Ordem de serviço (`ADMIN` e `FUNCIONARIO`; cancelamento só `ADMIN`): abertura com serviços e peças, envio,
  aprovação e rejeição do orçamento (com reserva, baixa e devolução do estoque), mudança de status com histórico,
  listagem paginada com filtro por cliente e status e tempo médio de execução
- Acompanhamento das próprias OS pelo cliente (perfil `CLIENTE`, em `/minhas-ordens`)
- Observabilidade com Prometheus e Grafana (os 4 sinais de ouro e métricas de negócio)

## Arquitetura utilizada

Monólito em camadas (ports & adapters):

- `core` concentra o domínio, os casos de uso e as portas de entrada (`port/in`) e saída (`port/out`), sem JPA nem
  Spring Web
- `controller` é o adapter de entrada (REST), separado em `admin`, `auth` e `cliente`
- `infrastructure` é o adapter de saída (persistência com JPA, com as entidades em `persistence/<contexto>/`) e as
  configurações de segurança, documentação e métricas
- `shared` guarda as exceções, o tratamento global de erros e os utilitários

A decisão, as camadas, a regra de dependência e as alternativas avaliadas estão
na [ADR 001](docs/adr/0001-monolito-em-camadas.md). Todas as decisões de arquitetura estão
no [índice de ADRs](docs/adr/README.md).

Os diagramas ficam em `docs/arquitetura/`:

- [Modelo C4](docs/arquitetura/c4.md): contexto, contêineres e componentes da API
- [Diagramas de sequência](docs/arquitetura/sequencia-ordem-servico.md): fluxo da OS (abertura, orçamento e mudança de
  status)
- [Diagrama de estados](docs/arquitetura/estados-ordem-servico.md): status da OS e transições permitidas
- [DER do banco de dados](docs/arquitetura/der-banco-de-dados.md): tabelas e relacionamentos

## Tecnologias utilizadas

- Java 25
- Spring Boot 4 (Web MVC, Data JPA, Validation, Security, Actuator)
- PostgreSQL 18
- Flyway (migrations)
- JWT (jjwt)
- MapStruct e Lombok
- springdoc-openapi (Swagger UI)
- Micrometer, Prometheus e Grafana (observabilidade)
- JUnit 5, Mockito e Testcontainers (testes unitários e de integração)
- JaCoCo, SonarCloud e SonarQube (qualidade e cobertura)
- Docker e Docker Compose

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

cp .env.example .env              # PowerShell: Copy-Item .env.example .env
# edite o .env e troque todos os <ALTERE_AQUI> (veja Variáveis de ambiente)
docker compose up -d --build
```

O `docker compose up` sobe todos os serviços do [docker-compose.yml](docker-compose.yml). Para subir só a API e o
banco (sem o SonarQube, que pesa mais): `docker compose up -d --build db api prometheus grafana`.

| Serviço      | Container                     | Porta na máquina                |
|--------------|-------------------------------|---------------------------------|
| `db`         | `oficina-mecanica-db`         | `DB_HOST_PORT` (padrão 5432)    |
| `api`        | `oficina-mecanica-api`        | `SERVER_PORT` (padrão 8081)     |
| `prometheus` | `oficina-mecanica-prometheus` | 9090                            |
| `grafana`    | `oficina-mecanica-grafana`    | 3000                            |
| `sonarqube`  | `oficina-mecanica-sonarqube`  | 9000                            |

Dentro do Docker a API sempre escuta na 8081; `SERVER_PORT` muda só a porta publicada na sua máquina. Se mudar as
portas, ajuste os endereços abaixo.

Endereços:

- API: http://localhost:8081/api/v1
- Health check: http://localhost:8081/api/v1/actuator/health
- Métricas (formato Prometheus): http://localhost:8081/api/v1/actuator/prometheus
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000, login `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` do `.env`
- SonarQube: http://localhost:9000, login inicial `admin` / `admin`, com troca de senha no primeiro acesso

Comandos úteis:

```bash
docker compose logs -f api                 # acompanhar logs da API
docker compose ps                          # status dos containers
docker compose up -d --build api           # reconstruir a imagem da API depois de mudar o código ou o application.yaml
docker compose down                        # parar todos os containers
docker compose down -v                     # parar e APAGAR os volumes: banco, SonarQube, Prometheus e Grafana
```

> Para apagar só os dados do banco: `docker compose down` e depois `docker volume rm oficina-mecanica-api_pgdata` (o
> prefixo é o nome da pasta do projeto; confira com `docker volume ls`).

### Banco de dados e migrations (Flyway)

O schema é criado e evoluído pelo **Flyway**, que roda sozinho na subida da API:

- o container `db` cria o banco `oficina` e o usuário `DATASOURCE_USERNAME` na primeira subida (volume vazio);
- a API aplica as migrations de `src/main/resources/db/migration` que ainda não rodaram: tabelas
  (`V1__schema_inicial.sql`), perfis (`V2__perfis_iniciais.sql`, trocados por `ADMIN`, `FUNCIONARIO` e `CLIENTE` na
  `V10__perfis_fase_1.sql`) e as demais evoluções do schema;
- o Hibernate roda com `ddl-auto: validate`: só confere se as entidades batem com as tabelas. A API não sobe se alguma
  entidade divergir do schema.

Para mudar o banco (nova tabela, coluna, índice...), crie um novo arquivo `V<próximo número>__descricao.sql` junto com a
alteração da entidade. **Nunca edite uma migration já aplicada**: o Flyway valida o checksum e a API deixa de subir.

Possíveis problemas:

- **`password authentication failed`**: o volume do banco foi criado com outro usuário ou senha. O PostgreSQL só lê
  `DATASOURCE_USERNAME`/`DATASOURCE_PASSWORD` ao criar o volume; recrie com `docker compose down -v` (apaga os dados).
  No Windows, confira também se não há um PostgreSQL instalado na máquina ocupando a 5432 (`netstat -ano | findstr :5432`);
  se houver, troque `DB_HOST_PORT` (o `DATASOURCE_HOST` do `.env.example` já usa essa porta).
- **`Migration checksum mismatch`**: uma migration já aplicada foi alterada (inclusive por reformatação da IDE).
  Restaure o arquivo original pelo git; não use `flyway repair` para esconder a diferença.
- **A API sobe, mas o Prometheus não a encontra**: confira no log `Tomcat started on port 8081` e se não há duas
  instâncias da API (Docker e IDE) disputando a porta.
- **Portas 9000, 9090 ou 3000 em uso**: troque a porta publicada do serviço no `docker-compose.yml` (ex.: `"9001:9000"`).
- **SonarQube reinicia ou não abre**: falta memória para o Docker; veja os pré-requisitos.
- **A API não sobe com erro de senha do administrador**: `ADMIN_SENHA` está fora da política de senha (veja
  [Usuários](#usuários-credenciais-de-demonstração-e-autenticação)).

## Variáveis de ambiente

Todas ficam no `.env` (copiado do [`.env.example`](.env.example)). O Compose repassa à API as que estão no
`environment` do serviço `api`; fora do Docker, a API lê o `.env` direto.

| Variável                           | Padrão                                  | Obrigatória        | Descrição |
|------------------------------------|-----------------------------------------|--------------------|-----------|
| `API_SECRET`                       | vazio (chave aleatória a cada subida)   | fora do ambiente local | chave de assinatura do JWT (HS512, mínimo 64 bytes): `openssl rand -base64 64`. Vazia, a API gera uma chave aleatória e os tokens deixam de valer a cada reinício |
| `DATASOURCE_USERNAME`              | — (no Compose: `postgres`)              | sim                | usuário do banco (o mesmo cria o usuário do container `db`) |
| `DATASOURCE_PASSWORD`              | — (no Compose: `postgres`)              | sim                | senha do banco |
| `DATASOURCE_HOST`                  | `jdbc:postgresql://localhost:5432/oficina` | não             | URL JDBC usada fora do Docker (no Compose, a API usa o serviço `db`) |
| `DB_HOST_PORT`                     | `5432`                                  | não                | porta do banco publicada na máquina |
| `DATASOURCE_POOL_SIZE`             | `10`                                    | não                | tamanho máximo do pool de conexões (Hikari) |
| `DATASOURCE_CONNECTION_TIMEOUT_MS` | `5000`                                  | não                | espera máxima por uma conexão do pool; depois disso a API responde 503 |
| `DATASOURCE_QUERY_TIMEOUT_MS`      | `5000`                                  | não                | tempo máximo de uma consulta; depois disso a API responde 503 |
| `ADMIN_EMAIL`                      | —                                       | para criar o admin | e-mail do administrador inicial, criado na subida se não existir nenhum `ADMIN` |
| `ADMIN_USUARIO`                    | —                                       | para criar o admin | nome de usuário (login) do administrador inicial |
| `ADMIN_SENHA`                      | —                                       | para criar o admin | senha do administrador inicial (segue a política de senha) |
| `ADMIN_NOME`                       | `Administrador`                         | não                | nome do administrador inicial |
| `JWT_EXPIRATION_MINUTES`           | `60`                                    | não                | validade do token de login, em minutos |
| `LOGIN_MAX_TENTATIVAS`             | `5`                                     | não                | falhas de login seguidas de uma conta antes do bloqueio temporário (429) |
| `LOGIN_JANELA_MINUTOS`             | `15`                                    | não                | janela de contagem e duração do bloqueio do login, em minutos |
| `CORS_ALLOWED_ORIGINS`             | vazio                                   | não                | origens do front que podem chamar a API pelo navegador, separadas por vírgula; vazio = nenhuma |
| `SERVER_PORT`                      | `8081`                                  | não                | porta HTTP da API (no Compose, só a porta publicada) |
| `APPLICATION_PREFIX`               | `api/v1`                                | não                | prefixo (context-path) das rotas; se mudar, ajuste o healthcheck e o `prometheus.yaml` |
| `APPLICATION_NAME`                 | `oficina-mecanica-api`                  | não                | nome da aplicação |
| `LOG_LEVEL`                        | `INFO`                                  | não                | nível de log do pacote da aplicação |
| `LOG_PATH`                         | `/tmp/logs`                             | não                | diretório dos logs |
| `GRAFANA_ADMIN_USER`               | `admin`                                 | não                | usuário do admin do Grafana (só no Compose) |
| `GRAFANA_ADMIN_PASSWORD`           | `admin`                                 | não                | senha do admin do Grafana (só no Compose) |

## Instruções para execução dos testes

| Comando      | O que roda | Precisa de |
|--------------|------------|------------|
| `mvn test`   | testes unitários (`*Test`): domínio, casos de uso, controllers, segurança e arquitetura | Java e Maven, ou Docker (comando abaixo) |
| `mvn verify` | unitários + testes de integração (`*IT`), cobertura JaCoCo e a meta de 80% | Docker aberto (Testcontainers sobe um PostgreSQL 18 só para o teste) e Java e Maven, ou só Docker (comando abaixo) |

Os testes de integração ficam no pacote da classe que exercitam (ex.: `SegurancaIT`, `FluxoOrdemServicoIT`,
`MigracoesIT`) e usam a base `src/test/java/.../integracao/IntegracaoBase.java`. Sem Docker, eles são pulados.

O `RegraDependenciaTest` (ArchUnit) confere a regra de dependência da
[ADR 001](docs/adr/0001-monolito-em-camadas.md): um `import` de `infrastructure` dentro do `core`, por exemplo,
reprova o build. A única exceção é o `Perfil`, que implementa `GrantedAuthority` (dívida conhecida).

Sem Java ou Maven instalados, os unitários rodam num container:

```bash
# bash / Git Bash
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app maven:3-amazoncorretto-25-alpine mvn test
```

```powershell
# PowerShell
docker run --rm -v "${PWD}:/app" -v "$env:USERPROFILE\.m2:/root/.m2" -w /app maven:3-amazoncorretto-25-alpine mvn test
```

O volume `.m2` guarda as dependências entre execuções.

O `mvn verify` também roda num container. Ele precisa do socket do Docker para o Testcontainers subir o PostgreSQL:

```bash
# bash / Git Bash
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal --add-host=host.docker.internal:host-gateway \
  -w /app maven:3-amazoncorretto-25-alpine mvn verify
```

```powershell
# PowerShell
docker run --rm -v "${PWD}:/app" -v "$env:USERPROFILE\.m2:/root/.m2" -v /var/run/docker.sock:/var/run/docker.sock `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal --add-host=host.docker.internal:host-gateway `
  -w /app maven:3-amazoncorretto-25-alpine mvn verify
```

## Qualidade de código e cobertura (Sonar + JaCoCo)

`mvn verify` roda os testes e gera o relatório de cobertura do JaCoCo em `target/site/jacoco/index.html` (o XML em
`target/site/jacoco/jacoco.xml` é o que o Sonar lê).

### SonarCloud (automático)

O workflow `.github/workflows/ci.yml` roda `mvn verify` em todo push e PR para `main` e `develop`. Quando o secret
`SONAR_TOKEN` está configurado no repositório, o workflow também envia a análise ao SonarCloud (organização
`18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`). O resultado e o quality gate aparecem no próprio PR.

### SonarQube local (antes de abrir o PR)

No SonarQube, gere um token em *My Account > Security* e rode a análise:

```bash
mvn verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<seu-token>
```

Os dados do SonarQube (token, projeto, hotspots revisados) ficam em volumes e sobrevivem a `docker compose down`; só
são apagados com `down -v`.

### Meta de cobertura nos domínios críticos

O `mvn verify` falha se algum pacote de domínio ou de caso de uso de `cliente`, `veiculo`, `ordemservico` ou
`pecainsumo` (estoque) ficar abaixo de **80% de linhas cobertas** (regra `check` do JaCoCo no `pom.xml`, propriedade
`jacoco.minimo.linhas`).

### Pré-validação antes do push

O hook `.githooks/pre-push` roda `mvn clean verify` (testes + cobertura) antes de cada `git push` e bloqueia o envio se
algo falhar. Ative uma vez por clone:

```bash
git config core.hooksPath .githooks
```

Enquanto roda, ele mostra o progresso, uma linha por classe de teste, e a subida do banco e do Spring nos testes de
integração:

```
▶ Testes unitários
  ✔ ClienteUseCaseTest                       21 testes  1.3 s
  ✘ NormalizadorNomeTest                     1 falha(s) de 9
▶ Testes de integração
    PostgreSQL (Testcontainers) no ar em 1.1 s
    Flyway: migrations aplicadas até a V10
    Spring iniciado em 9.3 s
  ✔ FluxoOrdemServicoIT                      5 testes  0.9 s
▶ Cobertura
[pre-push] OK: cobertura total de linhas 98.0%
```

- Se um teste falhar, o hook mostra o teste, o valor esperado e o recebido, e bloqueia o push.
- **O Docker precisa estar aberto.** Sem ele, os testes de integração são pulados e o hook bloqueia o push, porque o
  envio não pode passar sem eles.
- `PRE_PUSH_QUIET=1 git push` roda sem o progresso na tela: só o resultado.
- O log completo fica em `.git/pre-push.log`.
- Usa `mvn`; sem Maven instalado, usa a imagem Docker `maven:3-amazoncorretto-25-alpine`, com acesso ao Docker do host
  para os testes de integração. No Windows, o hook roda pelo Git Bash (já incluído no Git for Windows).
- Em emergência, `git push --no-verify` pula o hook (o CI roda a mesma validação no PR).

Para ver os problemas do Sonar enquanto programa, instale a extensão **SonarQube for IDE** e conecte-a ao SonarCloud
(*connected mode*, organização `18soat-grupo-26`, projeto `18SOAT-Grupo-26_oficina-mecanica-api`).

## Acesso ao Swagger / documentação da API

- Swagger UI: http://localhost:8081/api/v1/documentation
- OpenAPI (JSON): http://localhost:8081/api/v1/api-docs-personalized

Para chamar endpoints protegidos pelo Swagger, faça login em `POST /auth/login`, clique em **Authorize** e cole o token
**sem o prefixo `Bearer `**: o Swagger já o acrescenta e, com o prefixo duplicado, a API responde 401.

### Respostas de erro

Todo erro volta no mesmo formato (`ErrorTemplate`: `errorCode`, `message`, `timestamp` e `traceId`), sem stack trace.
O `HandlerException` converte cada tipo de erro no status HTTP:

| Status | Quando |
|--------|--------|
| 400 | Dado inválido: Bean Validation (`@Valid`), regra de formato do domínio (CPF/CNPJ, placa, nome, preço, estoque), parâmetro de tipo errado ou header obrigatório ausente |
| 401 | Token ausente, inválido ou expirado, ou credenciais inválidas no login |
| 403 | Perfil sem permissão para a rota (ex.: `FUNCIONARIO` tentando excluir) |
| 404 | Recurso não encontrado ou desativado |
| 409 | Conflito com uma regra de negócio: duplicidade, transição de status inválida, saldo insuficiente, item em uso |
| 422 | Body ausente ou ilegível (JSON malformado, enum desconhecido) |
| 429 | Muitas tentativas de login |
| 500 | Erro inesperado ou de configuração do servidor (ex.: perfil ausente na tabela `perfis`) |
| 503 | Banco fora do ar ou lento demais |

Desde o #139, a validação de serviço e de peça responde **400**, como a dos outros cadastros. Antes, respondia 422.

### Collection do Postman

A collection [`docs/postman/oficina-mecanica-api.postman_collection.json`](docs/postman/oficina-mecanica-api.postman_collection.json)
traz todos os endpoints: auth, peças e insumos, serviços, clientes, veículos, ordem de serviço, acompanhamento da OS
pelo cliente (`/minhas-ordens`), exclusões e actuator. Para usar:

1. No Postman, **Import** → selecione o arquivo.
2. Preencha as variáveis da collection `adminUsuario` e `adminSenha` com o `ADMIN_USUARIO` e o `ADMIN_SENHA` do seu
   `.env` (elas vão vazias no arquivo).
3. Rode **Auth → Login administrador**: o token fica salvo na variável `token` e é enviado como Bearer nas demais
   requisições.
4. Os cadastros salvam os ids criados (`pecaInsumoId`, `servicoId`, `clienteId`, `veiculoId`, `ordemId`) para as
   requisições seguintes.

CPF, placa, nomes e username são gerados a cada execução, então a collection pode ser rodada várias vezes sem violar as
constraints `UNIQUE`. Para apontar para outro ambiente, altere a variável `baseUrl` (padrão: `http://localhost:8081/api/v1`).

## Observabilidade (Prometheus e Grafana)

- A API expõe métricas Micrometer em `/api/v1/actuator/prometheus` (sem autenticação, como o `/actuator/health`).
- O Prometheus (http://localhost:9090) coleta a API a cada 15 s (`prometheus/prometheus.yaml`). Em *Status > Targets*,
  um dos jobs deve estar `UP`: `oficina-mecanica-api` (API no Docker) ou `oficina-mecanica-api-local` (API na IDE).
- Regras de gravação dos 4 sinais de ouro em `prometheus/regras-sinais-ouro.yaml` e alertas em `prometheus/alertas.yaml`
  (veja em *Alerts*; não há Alertmanager configurado).
- O Grafana (http://localhost:3000) já sobe com o Prometheus como fonte de dados e com o dashboard **Oficina Mecânica
  API** (pasta *Oficina Mecânica*, também a tela inicial), provisionados em `grafana/provisioning`. O dashboard é
  organizado pelos 4 sinais de ouro:
    - **Latência:** p50/p95/p99 das requisições, p95 por rota, % acima de 1 s, tempo das consultas e espera por conexão;
    - **Tráfego:** requisições por segundo por rota e resultado, logins, OS abertas e cadastros;
    - **Erros:** taxa de 5xx e 4xx, erros por tipo (negócio × falha), banco indisponível, acessos negados;
    - **Saturação:** heap, CPU, pool de conexões, threads HTTP, GC e OS em andamento;
    - mais as métricas de negócio da OS e do estoque.

## Decisão e justificativa do banco de dados escolhido

Usamos o **PostgreSQL 18**, rodando em container via Docker Compose. Os dados da oficina são relacionais (a OS reúne
cliente, veículo, serviços e peças), e regras como a reserva de estoque precisam de transações ACID e de integridade
garantida pelo banco (chaves estrangeiras e constraints `UNIQUE` para CPF/CNPJ e placa). Pelo teorema PACELC, precisamos
de um banco que priorize consistência (CP/EC). Como a aplicação é um monólito com um único banco, não precisamos de um
banco distribuído. Além disso, o PostgreSQL é open source e o time já tem familiaridade com ele. Os detalhes e as
alternativas avaliadas estão na [ADR 002](docs/adr/0002-escolha-do-banco-de-dados.md).

## Usuários, credenciais de demonstração e autenticação

A autenticação usa JWT. As regras de acesso estão em [ADR 004](docs/adr/0004-seguranca.md):

| Método | Rota | Acesso |
|--------|------|--------|
| `POST` | `/api/v1/auth/login` | público |
| `GET`  | `/api/v1/auth/validacao` | público (valida o token do header `Authorization`) |
| `POST` | `/api/v1/usuarios` | `ADMIN` |
| `GET`, `POST`, `PUT` | `/api/v1/clientes`, `/veiculos`, `/servicos`, `/pecas-insumos` | `ADMIN`, `FUNCIONARIO` |
| `DELETE` | `/api/v1/clientes`, `/veiculos`, `/servicos`, `/pecas-insumos` | `ADMIN` |
| `GET`, `POST`, `PUT`, `PATCH` | `/api/v1/ordens-servico` | `ADMIN`, `FUNCIONARIO` |
| `DELETE` | `/api/v1/ordens-servico/{id}` (cancelar OS) | `ADMIN` |
| `GET` | `/api/v1/minhas-ordens`, `/api/v1/minhas-ordens/{id}` | `CLIENTE` (só as OS do próprio cliente) |
| `GET` | `/api/v1/actuator/health`, `/api/v1/actuator/prometheus`, Swagger | público |

Qualquer outra rota exige token (sem token: 401; perfil sem permissão: 403).

Só um `ADMIN` logado cadastra usuários. O primeiro administrador é criado pela própria API na subida, a partir
das variáveis do `.env` — nenhuma credencial fica no repositório:

1. Defina no `.env` `ADMIN_EMAIL`, `ADMIN_USUARIO` e `ADMIN_SENHA` (opcional: `ADMIN_NOME`) e suba a API. Se ainda não
   existir nenhum `ADMIN`, ele é criado; se as variáveis faltarem, a API sobe sem administrador e avisa no log.
   A senha precisa ter de 8 a 64 caracteres, com maiúscula, minúscula, dígito e um destes especiais:
   `!@#$%^&*()-_=+[]{}<>?/,.:|~` (senha fora da política impede a subida, com a mensagem no log).

2. Faça login com esse administrador (o `identificador` aceita nome de usuário ou e-mail); a resposta traz o token já
   com o prefixo `Bearer `:

```bash
source .env   # carrega ADMIN_USUARIO e ADMIN_SENHA no terminal
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login -H "Content-Type: application/json" \
  -d "{\"identificador\": \"$ADMIN_USUARIO\", \"senha\": \"$ADMIN_SENHA\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

3. Cadastre outros usuários enviando o token (perfil `ADMIN`, `FUNCIONARIO` ou `CLIENTE`). Hoje
   `trocaSenhaObrigatoria` precisa ser `false`:

```bash
curl -X POST http://localhost:8081/api/v1/usuarios -H "Content-Type: application/json" -H "Authorization: $TOKEN" -d '{
  "nome": "Bruno Funcionário", "email": "bruno@oficina.com.br",
  "nomeUsuario": "bruno", "senha": "Bruno@123", "trocaSenhaObrigatoria": false, "perfil": "FUNCIONARIO"
}'
```

O usuário `CLIENTE` é ligado a um cliente já cadastrado pelo `clienteId` (obrigatório para esse perfil e proibido
para os outros; um login por cliente). Ele só enxerga as OS desse cliente, em `/minhas-ordens`:

```bash
curl -X POST http://localhost:8081/api/v1/usuarios -H "Content-Type: application/json" -H "Authorization: $TOKEN" -d '{
  "nome": "Maria da Silva", "email": "maria@email.com",
  "nomeUsuario": "maria", "senha": "Maria@123", "trocaSenhaObrigatoria": false, "perfil": "CLIENTE", "clienteId": 1
}'
```

4. Envie o token nas demais requisições no header `Authorization` (o valor devolvido pelo login já inclui `Bearer `).

O token vale `JWT_EXPIRATION_MINUTES` (padrão 60 min). Após `LOGIN_MAX_TENTATIVAS` (padrão 5) falhas de login de uma
mesma conta em `LOGIN_JANELA_MINUTOS` (padrão 15), o login dela responde **429** até a janela expirar.

Perfis existentes (migration `V10__perfis_fase_1.sql`): `ADMIN` (tudo, inclusive exclusões, serviços e cadastro de
usuários), `FUNCIONARIO` (cadastros sem exclusão e ordens de serviço) e `CLIENTE` (consulta das próprias OS).
Credenciais de demonstração: as do administrador inicial definidas no seu `.env` (`ADMIN_USUARIO` / `ADMIN_SENHA`).

## Análise de vulnerabilidades

O [relatório de vulnerabilidades](docs/seguranca/relatorio-vulnerabilidades.md) reúne a revisão manual (OWASP Top 10)
e as varreduras com Gitleaks (segredos no histórico), OWASP ZAP (API autenticada) e Trivy (CVEs da imagem Docker).
Para cada achado, ele traz a severidade, o PR da correção ou a justificativa do risco aceito. As evidências brutas
(antes e depois das correções) ficam em [`docs/seguranca/evidencias/`](docs/seguranca/evidencias/).

## Estrutura dos principais diretórios

```
src/main/java/br/com/fiap/oficinamecanicaapi
├── controller          # endpoints REST: admin (APIs da oficina), auth, cliente (acompanhamento de OS pelo cliente final)
├── core
│   ├── domain          # modelos de domínio, sem JPA
│   ├── port
│   │   ├── in          # portas de entrada (casos de uso)
│   │   └── out         # portas de saída (persistência)
│   └── usecase         # implementação dos casos de uso
├── infrastructure
│   ├── config          # segurança (JWT), documentação (OpenAPI), métricas e administrador inicial
│   └── persistence     # entidades JPA, repositórios e adapters
└── shared              # exceções, handler global e utilitários
src/main/resources/db/migration   # migrations do Flyway
src/test/java                     # testes unitários (*Test) e de integração (*IT)
docs                              # ADRs, diagramas, DDD, collection do Postman e relatório de segurança
prometheus, grafana               # configuração da observabilidade
```

## Documentação DDD

- [Event Storming no Miro](https://miro.com/app/board/uXjVEYmxgDM=/): board original
- [Event Storming (PDF)](docs/ddd/event-storming.pdf): criação da OS, acompanhamento da OS e peças e insumos
- [Agregados e contextos delimitados (PDF)](docs/ddd/diagramas.pdf): agregados e os
  contextos Atendimento, Ordem de Serviço e Catálogo e Estoque
- [Linguagem ubíqua](docs/ddd/linguagem-ubiqua.md): glossário e relação entre a documentação e o código

## Vídeo da entrega

_A definir._

## Licença

Distribuído sob a licença descrita em [LICENSE](LICENSE).
