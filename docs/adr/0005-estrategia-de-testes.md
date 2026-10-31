# ADR 005 — Estratégia de testes

- **Status:** Aceito
- **Data:** 2026-10-23
- **Responsável:** Ana Albuquerque

## Contexto

O projeto é incremental e vai ser ampliado nas próximas fases do Tech Challenge. Cada mudança precisa de uma forma
rápida de mostrar que as regras de negócio continuam certas, principalmente na ordem de serviço (OS), no orçamento e no
estoque, que são o core do domínio.

Requisitos não funcionais relacionados ([Miro](https://miro.com/app/board/uXjVEYDydiQ=/?share_link_id=413287603523)):

- **RNF09:** casos de uso testáveis isoladamente, sem iniciar servidor HTTP ou banco de dados.
- **RNF20:** cobertura de pelo menos 80% com testes unitários.
- **RNF10:** documentação OpenAPI gerada a partir dos contratos implementados e fiel a eles.
- **RNF08:** estoque e orçamento consistentes mesmo com falhas e requisições concorrentes (ver
  [ADR 003](0003-estrategia-de-transacoes.md)).

O que as ADRs anteriores já definem:

- O `core` (domínio e casos de uso) não depende de banco nem de HTTP. Os casos de uso falam com o mundo externo só pelos
  `PortOut` ([ADR 001](0001-monolito-em-camadas.md)).
- Parte das garantias fica fora do Java: as constraints do PostgreSQL, as migrations do Flyway
  ([ADR 002](0002-escolha-do-banco-de-dados.md)) e o `WHERE` dos `UPDATE`s de estoque (ADR 003).
- A segurança depende de configuração do Spring: rotas públicas, filtro JWT e `@PreAuthorize`
  ([ADR 004](0004-seguranca.md)).

Restrições:

- Equipe pequena e prazo curto. Os testes precisam rodar rápido na máquina de cada um e no CI.
- O pipeline do GitHub Actions já roda `mvn verify` em cada push e PR para `main` e `develop`, com análise no
  SonarCloud. Também existe um SonarQube local no compose.
- Stack definida: JUnit 5, Mockito, AssertJ e os starters de teste do Spring Boot 4.

## Decisão

Adotamos uma **pirâmide de testes** em que a base são os testes unitários do `core`, sem Spring, banco ou HTTP. Em cima
deles vêm os testes de fatia (controllers e segurança com `@WebMvcTest`) e, no topo, poucos testes de integração com
**PostgreSQL de verdade via Testcontainers**. A cobertura é medida pelo **JaCoCo** e **barra o build** abaixo de 80%
nos pacotes críticos.

### 1. Níveis de teste

| Nível                          | O que cobre                                                                                       | Ferramentas                                       | Exemplos                                                                  |
|--------------------------------|---------------------------------------------------------------------------------------------------|---------------------------------------------------|---------------------------------------------------------------------------|
| Unitário de domínio            | Value objects, agregados, máquina de estados da OS                                                | JUnit 5 e AssertJ, sem mocks                      | `PlacaTest`, `DocumentoTest`, `OrdemServicoTest`, `StatusOrdemServicoTest` |
| Unitário de caso de uso        | Regras de aplicação com os `PortOut` mockados (RNF09)                                             | JUnit 5 e Mockito (`@ExtendWith(MockitoExtension)`) | `ClienteUseCaseTest`, `OrdemServicoUseCaseTest`, `AuthenticationUseCaseTest` |
| Unitário de adapter e mapper   | Tradução entre domínio e entidade JPA, com o `JpaRepository` mockado                              | JUnit 5 e Mockito                                 | `VeiculoPersistenceAdapterTest`, `OrdemServicoPersistenceMapperTest`      |
| Fatia web (controller)         | Contrato HTTP: status, JSON, validação, `HandlerException` e acesso por perfil (401, 403 e 2xx)   | `@WebMvcTest` + `MockMvc`, port in com `@MockitoBean` | `ClienteControllerTest`, `MinhasOrdensControllerTest`                     |
| Guardas de segurança           | Nenhuma rota fora de `RotasPublicas` aceita requisição sem token. CORS só para as origens configuradas | `@WebMvcTest` com todos os controllers            | `RotasProtegidasTest`, `CorsConfigTest`                                   |
| Integração (`*IT`)             | Migrations, `ddl-auto: validate`, `@Query` nativas de estoque, rollback e fluxos ponta a ponta    | `@SpringBootTest` + Testcontainers (`postgres:18-alpine`) | `MigracoesIT`, `PecaInsumoRepositoryIT`, `OrdemServicoJpaRepositoryIT`, `FluxoOrdemServicoIT`, `SegurancaIT` |
| Manual e de aceite             | Fluxo completo como o avaliador vai usar                                                          | Collection do Postman e Swagger                   | `docs/postman/`                                                           |

Hoje (29/10) a `develop` tem **702 execuções de teste unitário** (com os parametrizados), que não precisam de banco, e
**28 de integração** nos `*IT`.

### 2. Regras para escrever testes

- **Teste de caso de uso não sobe o Spring.** O caso de uso é instanciado com os `PortOut` mockados. Uma falha de banco
  ou de estoque é simulada com o mock devolvendo `false` ou lançando a exceção. Por exemplo, `reservar` devolvendo
  `false` simula falta de saldo (ADR 003).
- **Teste de domínio não usa mock.** Se um modelo de domínio precisa de mock para ser testado, ele está dependendo de
  algo de fora do domínio.
- **Teste de controller testa o contrato, e não a regra.** O port in é mockado. Cada rota protegida tem pelo menos um
  caso por perfil que importa: sem token (401), perfil sem acesso (403) e perfil com acesso (2xx).
- **Tempo controlado.** Quem depende da hora recebe um `Clock` (`ClockConfig`), e o teste usa um `Clock.fixed`. Não há
  `Thread.sleep` nem `LocalDateTime.now()` solto nos testes.
- **Nomes em português**, com `@DisplayName` descrevendo o comportamento esperado ("Deve responder 409 quando...").
- **Testes de integração terminam em `IT`.** Eles rodam pelo `maven-failsafe-plugin`, na fase `integration-test`. Os
  unitários ficam no `surefire`, na fase `test`. Sem Docker na máquina, os `*IT` são **pulados**, e não reprovados
  (`@EnabledIf` com `DockerClientFactory`). No CI, o runner do GitHub tem Docker, e eles rodam sempre.
- **Banco limpo a cada `*IT`.** Os dados de um teste não vazam para o próximo. O administrador inicial é criado pelo
  próprio `AdministradorInicial`, como na aplicação, e os outros usuários são cadastrados com o token dele.

### 3. Cobertura e qualidade

- **JaCoCo** em todo `mvn verify`: relatório em `target/site/jacoco/` e regra `check` que **falha o build** se um
  pacote de `cliente`, `veiculo`, `ordemservico` ou `pecainsumo` (domínio e caso de uso) ficar abaixo de **80% de
  linhas** (`jacoco.minimo.linhas`, RNF20). Outros pacotes entram na regra conforme ganham testes.
- **SonarCloud** em cada PR (quality gate visível no próprio PR) e **SonarQube** local (`docker compose --profile
  sonar`) para conferir antes de abrir o PR. Os dois leem o XML do JaCoCo.
- **Hook `pre-push`** (`.githooks/pre-push`): roda `mvn clean verify` antes do push e bloqueia o envio se um teste
  falhar ou a cobertura cair. O CI repete a validação, então o hook só antecipa o erro.
- **Contrato da documentação (RNF10):** o `OpenApiConfigTest` confere que o esquema `bearerAuth` está aplicado às rotas
  protegidas e ausente das públicas.

### 4. Testes de integração com Testcontainers

Os unitários não conseguem provar o que depende do PostgreSQL: o `WHERE` dos `UPDATE`s de estoque, as constraints
`UNIQUE`, as migrations e o rollback da transação do caso de uso. Para isso, usamos o **Testcontainers** com a mesma
imagem do compose (`postgres:18-alpine`). O `@ServiceConnection` do Spring Boot liga o datasource ao container, e o
Flyway aplica as mesmas migrations da aplicação.

Esses testes ficam no topo da pirâmide e são poucos. Eles cobrem o que só o banco garante:

- todas as migrations aplicam do zero, e o Hibernate valida as entidades (`MigracoesIT`);
- `reservar`, `liberar` e `baixar` recusam movimento sem saldo ou sem reserva (`PecaInsumoRepositoryIT`);
- as consultas da OS (OS por cliente, contagem por status e períodos de execução para o tempo médio) rodam no
  PostgreSQL (`OrdemServicoJpaRepositoryIT`);
- o ciclo da OS até `ENTREGUE` movimenta o estoque, e uma OS com mais peças que o saldo responde 409 sem gravar nada
  (`FluxoOrdemServicoIT`);
- login, token adulterado, 401 e 403 com a cadeia de segurança completa (`SegurancaIT`).

Os testes de integração entraram na `develop` no #111, com o `maven-failsafe-plugin`.

## Alternativas consideradas

| Alternativa                                     | Tema              | Situação                                          |
|-------------------------------------------------|-------------------|---------------------------------------------------|
| **Pirâmide com base unitária no `core`**        | Estratégia        | **Escolhida**                                     |
| Só testes de ponta a ponta (`@SpringBootTest`)  | Estratégia        | Descartada: lentos e dependentes de banco         |
| **Testcontainers (PostgreSQL real)**            | Integração        | **Escolhida**                                     |
| H2 em memória nos testes                        | Integração        | Descartada: SQL e constraints diferentes          |
| Banco do `docker compose` nos testes            | Integração        | Descartada: estado compartilhado entre execuções  |
| **JaCoCo com mínimo por pacote crítico**        | Cobertura         | **Escolhida**                                     |
| Mínimo de 80% no projeto inteiro                | Cobertura         | Descartada por enquanto: pune código sem regra    |
| Testes de arquitetura (ArchUnit)                | Arquitetura       | Evolução (ADR 001)                                |
| Testes de contrato (Spring Cloud Contract, Pact) | Contrato          | Não se aplica: sem consumidores conhecidos        |
| Testes de mutação (PIT)                         | Qualidade         | Evolução futura                                   |
| Testes de carga (k6, Gatling)                   | Desempenho        | Fase 2                                            |

### 1. Só testes de ponta a ponta com `@SpringBootTest`

- **Prós:** cada teste exercita o sistema inteiro, como em produção, e pega erro de configuração e de integração.
- **Contras:** cada classe sobe o contexto completo e um banco, então a suíte fica lenta. Quando um teste falha, é
  difícil saber qual camada quebrou. Também contraria o RNF09.
- **Por que não:** a regra de negócio está no `core`, que é testável sem infraestrutura. Os testes de ponta a ponta
  ficam só para o que não dá para provar de outro jeito.

### 2. H2 em memória nos testes de integração

- **Prós:** nada para instalar e sobe em milissegundos.
- **Contras:** as migrations são SQL do PostgreSQL ([ADR 002](0002-escolha-do-banco-de-dados.md)), e parte delas não
  roda no H2, nem no modo de compatibilidade. O comportamento de lock e de `READ COMMITTED` que a ADR 003 usa no estoque
  também é diferente. Um teste verde no H2 não garante nada no PostgreSQL.
- **Por que não:** o motivo de ter teste de integração é provar o comportamento do banco real.

### 3. Usar o banco do `docker compose` nos testes

- **Prós:** não precisa de mais nenhuma dependência.
- **Contras:** o teste depende de alguém ter subido o compose antes, apaga ou suja os dados de desenvolvimento, e os
  resultados mudam conforme o estado do banco.
- **Por que não:** o Testcontainers cria um banco descartável por execução, igual no CI e na máquina de cada um.

### 4. Mínimo de 80% no projeto inteiro

- **Prós:** uma regra só, que cobre tudo.
- **Contras:** pune classes sem regra de negócio (configuração, DTOs, entidades JPA) e incentiva testes que só
  aumentam o número. Quando esta ADR foi escrita, também travaria o build: o projeto estava em **79,9% de linhas**.
  Em 29/10, está em **97,6%**.
- **Por que não, por enquanto:** a regra fica onde está o risco: domínio e casos de uso de `cliente`, `veiculo`,
  `ordemservico` e `pecainsumo` (estoque). O total do projeto continua acompanhado no Sonar, com meta de 80% (RNF20).

### 5. Testes de arquitetura (ArchUnit)

- **Prós:** a regra de dependência da ADR 001 vira teste, e um `import` de `infrastructure` dentro do `core` reprova o
  PR.
- **Contras:** mais uma dependência e regras para manter. Hoje o `core` ainda tem exceções conhecidas (ADR 001 e ADR
  004), então as regras nasceriam com exceções.
- **Por que não, por enquanto:** é a mitigação já prevista na ADR 001. Entra quando as dívidas do `core` forem pagas,
  para nascer sem exceções.

### 6. Testes de contrato, mutação e carga

- **Contrato (Pact, Spring Cloud Contract):** servem quando há consumidores conhecidos da API. Hoje não há. O contrato
  publicado é o OpenAPI, coberto pelo `OpenApiConfigTest` e pelos testes de controller.
- **Mutação (PIT):** mede se os testes realmente pegam erros, e não só se passam pelas linhas. É lento para rodar a
  cada PR. Fica como evolução para os pacotes críticos.
- **Carga (k6, Gatling):** não há requisito de volume nesta fase (ADR 002). Fica para a fase 2.

## Consequências

### Positivas

- A suíte unitária roda em segundos, sem Docker nem banco, e cada falha aponta para a camada que quebrou.
- O RNF09 é verificado na prática: nenhum teste de caso de uso sobe o Spring.
- O build falha se a cobertura dos domínios críticos cair, tanto no `pre-push` quanto no CI.
- Uma rota nova sem proteção, ou uma origem de CORS a mais, reprova o PR pelos guardas de segurança.
- O que depende do PostgreSQL (migrations, `UPDATE`s de estoque, constraints e rollback) é testado contra o PostgreSQL
  real, com a mesma imagem da aplicação.
- A collection do Postman serve ao mesmo tempo de teste manual e de roteiro para a avaliação e para o vídeo.

### Negativas e trade-offs

- **Mocks podem esconder erro de integração.** Um teste de caso de uso passa mesmo que o adapter real se comporte
  diferente do mock. Por isso o comportamento do banco tem os `*IT`.
- **Os `*IT` precisam de Docker.** Sem ele, o `mvn verify` local pula os `*IT` (extensão `DockerDisponivel`) e fica
  verde. O hook `pre-push` não aceita isso e bloqueia o push quando algum `*IT` foi pulado, e no PR o CI roda tudo.
- **Concorrência sem teste automatizado.** Nenhum teste dispara duas reservas ou duas aprovações ao mesmo tempo. A
  garantia do RNF08 sob concorrência vem do desenho do SQL (ADR 003), e não de um teste. O próximo passo é um `*IT` com
  duas threads disputando a última unidade da mesma peça.
- **Cobertura fora da regra não trava o build.** Os pacotes que estavam baixos quando esta ADR foi escrita ganharam
  testes (em 29/10: `core/usecase/auth` e `infrastructure/config/metrics` com 100%, adapters de `servico` e
  `pecainsumo` com 94% e 95%), mas só os pacotes da regra do JaCoCo reprovam o build se caírem. Os outros são
  acompanhados no Sonar e entram na regra conforme o time decidir.
- **Teste de controller depende do mock do filtro.** Os testes de controller mockam o `JwtTokenUseCasePortIn` e o
  `UsuarioUseCasePortIn` para simular o login. A validação real do token só é coberta pelo `SegurancaIT`.

## Referências

- [ADR 001 — Monólito em camadas (ports & adapters)](0001-monolito-em-camadas.md)
- [ADR 002 — Escolha do banco de dados](0002-escolha-do-banco-de-dados.md)
- [ADR 003 — Estratégia de transações](0003-estrategia-de-transacoes.md)
- [ADR 004 — Segurança: autenticação e autorização](0004-seguranca.md)
- [Requisitos não funcionais (Miro)](https://miro.com/app/board/uXjVEYDydiQ=/?share_link_id=413287603523)
- Mike Cohn, *Succeeding with Agile* (2009): pirâmide de testes
- Martin Fowler, [*The Practical Test Pyramid*](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Spring Boot — Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [Testcontainers — PostgreSQL module](https://java.testcontainers.org/modules/databases/postgres/)
- [JaCoCo — check goal](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html)
