# ADR 001 — Monólito em camadas (ports & adapters)

- **Status:** Proposto
- **Data:** 2026-10-23

## Contexto

A API atende o fluxo de uma oficina mecânica. Ela cuida de autenticação, cadastro de clientes e veículos, catálogo de serviços, peças e insumos, ordens de serviço (OS) e o acompanhamento da OS pelo cliente. Esses contextos são muito ligados entre si: uma OS reúne cliente, veículo, serviços e peças, e precisa de consistência entre eles, por exemplo na baixa de estoque e no orçamento.

Restrições do projeto:

- Equipe pequena (grupo da pós-tech) e prazo curto por entrega.
- Um único banco PostgreSQL (ver [ADR 002](0002-escolha-do-banco-de-dados.md)).
- Execução local e avaliação via Docker Compose (`docker-compose.yml`).
- As regras de negócio precisam ser testadas sem subir o banco. Os testes de `core` usam JUnit e Mockito.
- A stack já foi definida: Java 25, Spring Boot 4, Spring Data JPA, MapStruct e Lombok. A escolha foi feita na reunião de 16/10 porque Java é a linguagem que a maioria da equipe domina.
- O projeto é **incremental**: o mesmo código será reaproveitado e ampliado nas próximas fases do Tech Challenge. A estrutura precisa aguentar essa evolução sem reescrita.
- Na mesma reunião, o André propôs a arquitetura hexagonal adaptada que ele já usa em outros projetos, "porque é bem fácil de se encontrar no fluxo". Ele montou a estrutura base do repositório nesse formato.
- O Event Storming (16/10) separou o problema em três agrupamentos: **ordem de serviço** (criação e ciclo de vida da OS, o core), **acompanhamento da OS pelo cliente** e **gestão administrativa** (cadastros de cliente, veículo, serviço e peça/insumo). Na OS, esses cadastros são apenas *selecionados*, e não criados.

Precisamos decidir duas coisas: **como a aplicação é implantada** (um ou vários deployables) e **como o código é organizado internamente**, para que as regras de negócio não fiquem presas ao framework e à persistência.

## Decisão

Adotamos um **monólito**: uma única aplicação Spring Boot, um artefato implantável e um banco. Internamente ele é **organizado em camadas no estilo ports & adapters (hexagonal)**, e todas as dependências apontam para o `core`.

### Camadas

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Adapter de entrada | `controller` | Endpoints REST. Uma interface `*API` com as anotações OpenAPI e um `*Controller` que a implementa e chama o port in. |
| Domínio | `core/domain` | Modelos e value objects com regras de negócio (`Cliente`, `Documento`, `Veiculo`, `Placa`). Sem Spring e sem JPA. |
| Portas | `core/port/in`, `core/port/out` | Contratos. O `PortIn` descreve os casos de uso oferecidos e o `PortOut`, o que o core precisa do mundo externo (persistência etc.). |
| Casos de uso | `core/usecase` | Orquestram o domínio. Implementam o `PortIn` e dependem só de `PortOut` e do domínio. |
| Adapters de saída e configuração | `infrastructure` | `persistence/<contexto>/`: entidade JPA, `*JpaRepository`, `*PersistenceMapper` e `*PersistenceAdapter`, que implementa o `PortOut`. `config/`: documentação e segurança, o que inclui os aspectos transversais (JWT, headers HTTP de segurança/CSP, sanitização de entradas). Esses aspectos nunca ficam no `core`. |
| Transversal | `shared` | Exceções (`ApplicationException`, `ErrorCode`), handler global e utilitários puros (validação de CPF/CNPJ, normalização de nome). |

Dentro de cada camada o código é **organizado por contexto** (`cliente`, `veiculo`, `servico`, `pecainsumo`, `ordemservico`...). Esses contextos seguem os agrupamentos do Event Storming:

- **Gestão administrativa**: `cliente`, `veiculo`, `servico` e `pecainsumo`. São CRUDs expostos em `controller/admin`.
- **Ordem de serviço** (core do domínio): `ordemservico`. A OS *referencia* os cadastros da gestão administrativa pelos ports desses contextos e não é dona deles.
- **Acompanhamento pelo cliente**: consulta do status da OS, exposta em `controller/cliente`.

A comunicação entre contextos é **síncrona**, por chamadas a ports dentro do mesmo processo. Não há fila nem eventos nesta fase.

### Convenções

- Código e artefatos do sistema (pacotes, classes, métodos, endpoints, documentação) ficam em **português**, conforme decidido em 21/10.
- Mensagens de commit ficam em **inglês**, seguindo Conventional Commits (decidido em 16/10).

### Regra de dependência

```mermaid
flowchart LR
    controller --> portIn[core/port/in]
    usecase[core/usecase] -. implementa .-> portIn
    usecase --> domain[core/domain]
    usecase --> portOut[core/port/out]
    infrastructure -. implementa .-> portOut
    infrastructure --> domain
    controller --> shared
    usecase --> shared
    infrastructure --> shared
```

| Camada | Pode depender de | Não pode depender de |
|---|---|---|
| `controller` | `core/port/in`, contratos (DTOs) do port in, `shared` | `core/usecase` (implementação), `infrastructure` |
| `core/domain` | `shared/exception` | Spring, JPA, `infrastructure`, `controller` |
| `core/port` | `core/domain`, DTOs dos casos de uso | entidades JPA, `infrastructure` |
| `core/usecase` | `core/domain`, `core/port`, `shared` | `infrastructure` (repositórios, entidades JPA), `controller` |
| `infrastructure` | `core/domain`, `core/port`, `shared` | `core/usecase` (implementação concreta), `controller` |
| `shared` | bibliotecas utilitárias | `core`, `infrastructure`, `controller` |

### Implementação de referência

Os contextos **`cliente`** e **`veiculo`** seguem o padrão por completo e servem de modelo para os novos:

- `ClienteUseCase` depende de `ClientePortOut`, que recebe e devolve o modelo de domínio `Cliente`.
- `ClienteEntity`, `ClienteJpaRepository`, `ClientePersistenceMapper` e `ClientePersistenceAdapter` ficam em `infrastructure/persistence/cliente/`.

### Exceções aceitas conscientemente

- **`@Service` nos casos de uso.** Evita uma configuração manual de beans para cada use case. O custo é uma dependência leve do Spring no `core/usecase`.
- **DTOs de request e response como contrato do port in.** Os controllers e os casos de uso trocam `*Request` e `*Response`, com anotações `jakarta.validation`, em vez de criar uma camada extra de comandos. Esses DTOs não são modelos de domínio e não devem carregar regra de negócio.

## Alternativas consideradas

### 1. Microsserviços (um serviço por contexto)

- **Prós:** deploy e escala independentes por contexto, isolamento de falhas, liberdade de tecnologia por serviço.
- **Contras:** a OS precisa de consistência entre cliente, veículo, serviço e estoque. Em serviços separados isso exigiria transações distribuídas ou sagas. Também traria latência de rede, service discovery, observabilidade distribuída e vários pipelines e bancos.
- **Por que não:** o custo operacional e de complexidade é desproporcional ao tamanho da equipe e do domínio. Não há hoje requisito de escala independente.

### 2. Camadas clássicas (controller → service → repository)

- **Prós:** é o padrão mais conhecido no Spring, tem menos classes e menos mapeamentos, e a curva de aprendizado é menor.
- **Contras:** as entidades JPA viram o modelo de domínio, então regra de negócio e persistência ficam acopladas. Os services dependem direto dos repositórios, o que dificulta testar sem banco e trocar a persistência.
- **Por que não:** perdemos o domínio testável e isolado. Parte do código atual (`servico`, `pecaInsumo`, `identity`, `user`) ainda segue esse formato e é listada abaixo como dívida.

### 3. Monólito modular com módulos isolados (Spring Modulith ou módulos Maven)

- **Prós:** fronteiras entre contextos verificadas pelo build ou por testes, e facilidade para extrair um microsserviço no futuro.
- **Contras:** traz mais configuração e mais decisões agora, como eventos entre módulos e a API pública de cada módulo, e a equipe ainda está consolidando o padrão de camadas.
- **Por que não, por enquanto:** fica como **evolução natural** desta decisão. A organização por contexto dentro de cada camada já prepara esse caminho.

### 4. Clean Architecture "pura" (nenhuma dependência de framework no core)

- **Prós:** isolamento total do framework, com o core sendo Java puro.
- **Contras:** exige configuração manual de beans, comandos e modelos de saída separados dos DTOs, e muito mais mapeamento.
- **Por que não:** pouco ganho prático para o tamanho do projeto. Aceitamos as exceções listadas na Decisão.

### 5. Vertical Slice (organização por funcionalidade)

- **Prós:** cada funcionalidade (ex.: abrir OS, aprovar orçamento) fica numa fatia própria, com entrada, caso de uso e persistência juntos. Tem menos abstrações, baixo acoplamento entre funcionalidades e combina bem com CQRS.
- **Contras:** há risco de duplicação e de inconsistência entre as fatias. Várias funcionalidades da OS usam as mesmas regras (reserva de estoque, validação de CPF/CNPJ, placa), que teriam de ser compartilhadas de qualquer forma. O padrão também não isola o domínio do framework e da persistência.
- **Por que não:** o domínio gira em torno da OS, que tem regras ricas e compartilhadas entre funcionalidades, e o problema que queremos resolver é testar essas regras sem banco. A organização por contexto dentro de cada camada já agrupa o código de cada assunto.

### 6. Onion Architecture

- **Prós:** tem o mesmo objetivo da hexagonal: dependências apontando para o domínio e contratos (ports) entre o núcleo e a infraestrutura, mas com anéis nomeados (domínio, serviços de domínio, serviços de aplicação e infraestrutura).
- **Contras:** na prática, a diferença é de nomenclatura. A separação entre serviços de domínio e de aplicação acrescenta uma camada de que o projeto ainda não precisa.
- **Por que não:** é muito próxima do que adotamos. Ficamos com o vocabulário de ports & adapters porque a estrutura base do repositório já foi montada nele (`port/in`, `port/out`).

### 7. Comunicação assíncrona entre contextos (fila/eventos)

- **Prós:** desacopla os contextos, por exemplo na reserva de estoque ou na notificação de mudança de status, e prepara o terreno para uma futura separação em serviços.
- **Contras:** exige um broker, idempotência, retentativas e consistência eventual, além de deixar os testes e o debug mais difíceis.
- **Por que não, por enquanto:** foi discutida no Event Storming (16/10) e descartada nesta fase ("vai criar problema pra gente"). Não haverá notificação ativa: o cliente consulta o status da OS quando quiser. Se for necessária nas próximas fases, entra como um novo adapter de saída, sem mudar o `core`.

## Consequências

### Positivas

- Deploy, execução local e debug simples: uma imagem Docker e um banco.
- Transações locais no PostgreSQL, sem consistência distribuída (ver [ADR 003](0003-estrategia-de-transacoes.md)).
- Domínio e casos de uso testáveis sem banco, com mocks dos ports (ex.: `ClienteUseCaseTest`, `VeiculoUseCaseTest`, `PlacaTest`).
- A troca ou evolução de um adapter (outro banco, outro canal de entrada) fica isolada em `infrastructure` e `controller`.
- Fronteiras por contexto facilitam a evolução para um monólito modular ou a extração de um serviço.
- Facilita a evolução entre as fases do Tech Challenge. Mensageria, observabilidade e integrações externas podem entrar como novos adapters, sem reescrever o domínio.

### Negativas e trade-offs

- Mais classes e mapeamentos por contexto (entidade JPA, modelo de domínio, DTOs e mappers).
- Curva de aprendizado: ports, adapters e a regra de dependência são novos para parte da equipe. Num projeto do nosso tamanho, a quantidade de abstrações pesa mais do que pesaria num sistema grande.
- A aplicação escala só como um todo. Um bug ou um deploy afeta todos os contextos.
- Hoje, a regra de dependência depende de disciplina e de code review. A mitigação planejada é adicionar testes de arquitetura (ArchUnit) ao pipeline que já roda Sonar e JaCoCo em cada PR, para que uma violação de camada reprove o PR.
- Observabilidade (métricas e padronização de logs) ficou para as próximas fases (decisão de 21/10). Quando entrar, ficará em `infrastructure`/config, sem acoplar o `core`.

### Dívida técnica conhecida (código fora do padrão em 2026-10-23)

1. Os casos de uso de `identity` e `user` usam repositórios JPA direto (`infrastructure.persistence.repository.*`), sem `PortOut`.
2. Há entidades JPA dentro de `core/domain` (`identity`, `user`, `servico`, `pecaInsumo`). Elas devem ir para `infrastructure/persistence/<contexto>/`, com modelo de domínio próprio e `*PersistenceMapper`. O modelo de `identity`/`user` já está em revisão pelo grupo: `Identity` tem dados comuns a cliente e funcionário (19/10), e `User` tem campos demais (21/10). Os itens 1 e 2 devem ser feitos junto com essa revisão, que também precisa resolver a sobreposição entre `Cliente`/`Endereco` e `Identity`/`Address`.
3. Alguns ports expõem entidades JPA: `ServicoPortOut`, `PecaInsumoPortOut` e os `PortIn` de `identity` e `user`.
4. `shared/utils/helpers/IdentityPayload` e `UserPayload` dependem de `core` e de `infrastructure`.
5. `JwtAuthenticationFilter` injeta a implementação `JwtTokenProviderUseCase` em vez de `JwtTokenUseCasePortIn`.
6. Spring Security está no domínio e nos casos de uso: `Role` implementa `GrantedAuthority`, e `PasswordEncoder` é usado direto. Isso deve virar um adapter e um port de hash de senha.
7. Os DTOs `*Request` e `*Response` estão em `core/domain/*/model`, misturados aos modelos de domínio. Devem ir para um pacote de contratos do port in.
8. Há dois layouts de repositório: `persistence/repository/<contexto>` e `persistence/<contexto>/*JpaRepository`. Padronizar no segundo.
9. O `@Transactional` fica nos adapters, mas `IdentityUseCase` o declara no use case. A regra será definida na ADR 003.
10. O pacote `pecaInsumo` está em camelCase. A convenção Java pede `pecainsumo`.
11. Ordem de serviço: `OrdemServicoUseCase` está em `usecase/catalogo` e `OrdemServicoUseCasePortIn` na raiz de `port/in`. Os dois devem ir para o contexto `ordemservico`, já que a OS é o core e não faz parte do catálogo. Os controllers de OS ainda são esqueletos, sem interface `*API`.
12. Os contextos `identity`, `user` e `auth` ainda usam nomes em inglês (`registerIdentity`, `AddressEntity`...). Devem ser renomeados para português, conforme a convenção decidida em 21/10.
13. Não existe teste automatizado das regras de camada. Proposta: adicionar ArchUnit com as regras da tabela acima e rodá-lo no pipeline de CI (Sonar/JaCoCo).

Cada item deve virar uma issue e ser corrigido em PRs separados por contexto.

## Referências

- [ADR 002 — Escolha do banco de dados](0002-escolha-do-banco-de-dados.md)
- [ADR 003 — Estratégia de transações](0003-estrategia-de-transacoes.md): inclui a reserva de estoque na geração do orçamento e a liberação quando o orçamento é reprovado, discutidas no Event Storming.
- Event Storming do grupo (Miro) e a reunião de 16/10/2026
- Decisões registradas no grupo do time entre 16/10 e 23/10/2026
- Alistair Cockburn, *Hexagonal Architecture* (2005)
