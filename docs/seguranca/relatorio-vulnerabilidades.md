# Relatório de análise de vulnerabilidades

- **Data:** 28/10/2026
- **Responsável:** Alexandre Silveira (segurança)
- **Commit analisado:** `develop` em `4663bd7` (merge do #114)
- **Ambiente:** Docker Compose local, com banco descartável e segredos gerados só para o teste

## 1. Resumo

A análise combinou revisão manual guiada pelo OWASP Top 10 (2021) com três ferramentas:

- **Gitleaks** para segredos no histórico do Git;
- **OWASP ZAP** para varredura dinâmica da API, autenticada;
- **Trivy** para CVEs da imagem Docker.

| Severidade | Encontrados | Corrigidos (mergeados) | Corrigidos (PR aberto) | Pendentes | Risco aceito |
|------------|:-----------:|:----------------------:|:----------------------:|:---------:|:------------:|
| Crítica    | 1           | —                      | 1                      | —         | —            |
| Alta       | 6           | 3                      | 2                      | 1         | —            |
| Média      | 4           | 3                      | 1                      | —         | —            |
| Baixa      | 6           | —                      | 1                      | 1         | 4            |

As 18 CVEs do Trivy contam como **2 achados**: um crítico (Tomcat) e um alto (Jackson). A correção de ambos é a mesma
atualização de versão, e a seção 4.3 detalha cada CVE.

**Antes e depois** dos PRs ainda abertos (#116, `fix/respostas-404-e-corp` e `fix/atualiza-spring-boot-4.1.2`):

| Ferramenta | `develop` (`4663bd7`)                                        | Com os PRs abertos aplicados                     |
|------------|--------------------------------------------------------------|--------------------------------------------------|
| OWASP ZAP  | 5 alertas, 44 respostas 500, 112 verificações aprovadas      | 2 alertas informativos, 0 respostas 500, 114 aprovadas |
| Trivy      | 4 críticas, 10 altas, 4 médias (Tomcat 11.0.24, Jackson)     | 0                                                |
| Gitleaks   | 4 ocorrências de segredo JWT no histórico (3 chaves)         | sem mudança: depende do item A1 (seção 5)        |

## 2. Escopo e metodologia

| Técnica | Ferramenta e versão | Como foi usada |
|---|---|---|
| Revisão manual | OWASP Top 10 (2021) | Leitura do código de segurança (filtro JWT, `SecurityConfig`, `RotasPublicas`, casos de uso de auth) e testes na API com `curl`: rotas sem token, perfil errado, login com usuário inexistente, força bruta, CORS e IDOR no acompanhamento de OS. |
| Segredos | Gitleaks v8.21.2 | Histórico completo (`--all`, 122 commits sem merge), com uma regra própria. **As regras padrão não detectam** segredo Base64 genérico: o teste com um controle positivo (token `ghp_...`) confirmou que a ferramenta funcionava, mas a chave JWT só apareceu com a regra `oficina-jwt-secret`. |
| DAST | OWASP ZAP 2.16.1 (`zap-api-scan.py`) | Guiado pelo OpenAPI (`/api/v1/api-docs-personalized`) e **autenticado como ADMIN**: um header `Authorization` é injetado em toda requisição, então as rotas protegidas também foram atacadas. |
| SCA / imagem | Trivy 0.58.1 | Imagem Docker da aplicação: pacotes do Alpine 3.24 e JARs dentro do fat jar. |

Fora do escopo: infraestrutura de produção (proxy, TLS, rede), que ainda não existe nesta fase, e testes de carga.

## 3. Achados

Severidade: **Crítica** (exploração remota sem autenticação com impacto alto), **Alta** (quebra de autenticação ou de
autorização, ou segredo exposto), **Média** (vazamento de informação ou facilitação de ataque), **Baixa** (endurecimento
e boas práticas).

| ID  | OWASP | Severidade | Achado | Origem | Status |
|-----|-------|------------|--------|--------|--------|
| A1  | A02 / A05 | Alta | Chave JWT real (chave B) no `.env.example`. Quem copia o exemplo usa uma chave pública, e qualquer pessoa consegue forjar um token de ADMIN. | Gitleaks + manual | **Pendente** (seção 5) |
| A2  | A01 | Alta | `POST /auth/cadastro` público: qualquer pessoa criava um usuário, inclusive ADMIN. | Manual | Corrigido no #108: só ADMIN cadastra, e o admin inicial é criado na subida a partir do `.env` |
| A3  | A01 | Alta | Rotas públicas com curinga (`/auth/**`) e token aceito como parâmetro de query/body. | Manual | Corrigido no #98 e no #99: lista explícita em `RotasPublicas` e `RotasProtegidasTest`, que exige 401 em toda rota não pública |
| A4  | A07 | Alta | Enumeração de usuários no login: usuário inexistente e senha errada respondiam de forma diferente. | Manual | Corrigido no #103: 401 igual e o mesmo custo de bcrypt (60 ms × 61 ms medidos) |
| A5  | A07 | Alta | Login de usuário inexistente com senha acima de 72 bytes respondia **500**, enquanto o usuário existente respondia 401. Isso reabria a enumeração do A4. | **ZAP** (Buffer Overflow / Format String no `/auth/login`) | Corrigido no PR #116, aguardando merge: comparação com um hash fictício gerado em tempo de execução |
| A6  | — (dependência) | **Crítica** | Tomcat 11.0.24 com 4 CVEs críticas (seção 4.3). | Trivy | Corrigido no PR `fix/atualiza-spring-boot-4.1.2`, aguardando abertura: Spring Boot 4.1.2 traz o Tomcat 11.0.27 |
| A7  | — (dependência) | Alta | Jackson 2.21.5 / 3.1.5 com 5 CVEs altas e 2 médias, de negação de serviço e desserialização (seção 4.3). | Trivy | Mesmo PR do A6: Jackson 2.21.7 / 3.1.7 |
| A8  | A07 | Média | Força bruta no login sem limite. | Manual | Corrigido no #114: 5 falhas em 15 min por identificador → 429. Identificadores inexistentes também contam |
| A9  | A09 / LGPD | Média | E-mail e nome de usuário em texto puro nos logs de login. | Manual | Corrigido no #114: `MascaraDados` (`a***@dominio`) |
| A10 | A05 | Média | CORS com `allowedOriginPatterns("*")` e `allowCredentials(true)`. | Manual | Corrigido no #102: só as origens de `CORS_ALLOWED_ORIGINS`, sem credenciais |
| A11 | A05 | Média | Rota inexistente, método não suportado e query malformada (`?=x`) respondiam **500**, com 44 ocorrências no ZAP. | ZAP (Application Error Disclosure, Server Error) | Corrigido no PR `fix/respostas-404-e-corp`, aguardando merge: 404, 405 e 400 |
| A12 | A05 | Baixa | Sem o header `Cross-Origin-Resource-Policy`. | ZAP (Insufficient Site Isolation Against Spectre) | Mesmo PR do A11: `same-origin` |
| A13 | A02 | Baixa | Uma terceira chave JWT (chave C), usada só no teste de integração (`IntegracaoBase`). Nenhum ambiente a usa. | Gitleaks | **Pendente**: gerar a chave em tempo de execução no teste, junto com o A1 |
| A14 | A05 | Baixa | `/actuator/prometheus` público. As métricas não têm dados pessoais, mas revelam volume e comportamento da API. | Manual | Risco aceito nesta fase (ADR 004); num ambiente exposto, rede interna ou autenticação |
| A15 | A07 | Baixa | Sem revogação de token: logout ou troca de perfil não invalidam um token já emitido (vale 1 h). | Manual | Risco aceito (ADR 004); versão do token no usuário fica para a fase 2 |
| A16 | A02 | Baixa | HTTP sem TLS no Compose. O JWT leva nome e e-mail em Base64. | Manual | Risco aceito: o HTTPS fica com o proxy ou ingress do deploy |
| A17 | A04 | Baixa | Limite de tentativas em memória: com mais de uma instância, cada uma conta separado. | Manual | Risco aceito: a fase 1 roda numa instância só |

Também corrigido, sem contar como vulnerabilidade: os placeholders do `application.yaml` usavam a sintaxe do shell
(`${VAR:-padrão}`), e o Spring transformava o padrão em `-padrão` (#113). Na minimização de dados para a LGPD, a
identidade foi removida e o usuário guarda só nome e e-mail (#100).

## 4. Resultado de cada ferramenta

### 4.1 Gitleaks

| Arquivo | Commit | Data | Chave | Situação na `develop` |
|---|---|---|---|---|
| `src/main/resources/application.yaml` | `26fe9db` | 18/10 | A | só no histórico (também na `main`) |
| `docker-compose.yml` | `2aeb199` | 22/10 | B | só no histórico |
| `.env.example` | `d4f2d60` | 27/10 | B | **presente** (A1) |
| `src/test/.../integracao/IntegracaoBase.java` | `c716b4c` | 28/10 | C | presente, só em teste (A13) |

As chaves foram comparadas por hash, sem exibir os valores: A, B e C são diferentes entre si. **A e B devem ser tratadas
como comprometidas**, porque removê-las do arquivo não as tira do histórico. Cada ambiente deve gerar a própria chave
com `openssl rand -base64 64`.

### 4.2 OWASP ZAP

| Alerta | Risco | `develop` | Depois | Leitura |
|---|---|:-:|:-:|---|
| Buffer Overflow / Format String (`/auth/login`) | Médio | 1 + 1¹ | 0 | O 500 do bcrypt (A5) |
| Application Error Disclosure | Baixo | 14 | 0 | Respostas 500 (A11) |
| A Server Error response code was returned | Baixo | 44 | 0 | Respostas 500 (A11) |
| Insufficient Site Isolation Against Spectre | Baixo | 6 | 0 | Header CORP (A12) |
| Unexpected Content-Type was returned | Info | 10 | 10 | Falso positivo: são os 404 em HTML do Tomcat **fora** do contexto `/api/v1`, em caminhos aleatórios que o ZAP testa |
| ZAP is Out of Date | Info | 1 | 1 | Aviso da própria ferramenta |

¹ Apareceu na primeira varredura de 28/10, feita na `develop` anterior (`c124536`). Os payloads do ZAP variam entre
execuções, por isso o achado também foi reproduzido manualmente: `POST /auth/login` com um identificador inexistente e
senha de 100 caracteres respondia 500. O PR #116 inclui um teste com bcrypt real que cobre esse caso.

Nenhum alerta de risco alto. As verificações de injeção (SQL, comando, SSTI, XSS refletido e persistente, XXE e path
traversal) passaram nas duas varreduras.

### 4.3 Trivy

O Alpine 3.24.3 (29 pacotes) não tem vulnerabilidade conhecida. Todas as CVEs vêm de JARs gerenciados pelo Spring Boot 4.1.1:

| CVE | Pacote | Sev. | Afeta este projeto? |
|---|---|---|---|
| CVE-2026-65182 | Tomcat 11.0.24 | Crítica | Não diretamente: bypass de *security constraints* do `web.xml`, que o projeto não usa (a autorização é do Spring Security) |
| CVE-2026-65637 | Tomcat 11.0.24 | Crítica | Não diretamente: correção incompleta ligada ao HTTP/2, que está desligado |
| CVE-2026-65905 | Tomcat 11.0.24 | Crítica | Não: autenticador DIGEST do Tomcat, não usado |
| CVE-2026-68525 | Tomcat 11.0.24 | Crítica | Não: autenticação FORM do Tomcat, não usada |
| CVE-2026-89407 | jackson-core 2.21.5 / 3.1.5 | Alta | **Sim, potencialmente**: ReDoS ao ler números enviados como string no JSON, em qualquer corpo de requisição |
| CVE-2026-89425 | jackson-core 2.21.5 / 3.1.5 | Alta | Pouco provável: só o parser de `DataInput` |
| CVE-2026-68497 | jackson-databind 2.21.5 / 3.1.5 | Alta | Não: nenhum campo `Duration`/`XMLGregorianCalendar` |
| CVE-2026-91776 | jackson-databind 2.21.5 / 3.1.5 | Alta | Não: nenhum `@JsonTypeInfo` |
| CVE-2026-91777 | jackson-databind 2.21.5 / 3.1.5 | Alta | Não: nenhum `@JsonIdentityInfo` |
| CVE-2026-19032 | jackson-databind 2.21.5 / 3.1.5 | Média | Não: nenhum campo `Path` |
| CVE-2026-83557 | jackson-databind 2.21.5 / 3.1.5 | Média | Não: nenhum `@JsonTypeInfo` |

Mesmo onde a exploração não se aplica, a correção é barata: o Spring Boot 4.1.2 traz o Tomcat 11.0.27 e o Jackson 3.1.7 e
2.21.7. Com a atualização, os 628 testes passaram e o Trivy zerou.

## 5. Pendências

1. **A1, alta:** trocar a chave do `.env.example` por um placeholder e orientar a geração com `openssl rand -base64 64`.
   Versionar a regra num `.gitleaks.toml` e rodar o Gitleaks no CI, para um segredo novo barrar o PR. Na mesma mudança,
   gerar a chave do teste de integração em tempo de execução (A13).
2. **Prevenção de dependências:** ligar o Dependabot (ou o Trivy) no CI, para que CVEs como as do A6 e do A7 sejam
   apontadas no PR, e não só numa análise pontual.
3. Repetir esta análise ao fim de cada fase, ou quando entrar uma dependência nova.

## 6. Controles que já estavam corretos

- **Autenticação:** JWT HS512 com expiração de 1 h. O `API_SECRET` é obrigatório (sem ele a API não sobe), e o perfil
  vem do banco a cada requisição, não do token (ADR 004).
- **Autorização:** negar por padrão. `RotasPublicas` é uma lista explícita, a autorização usa `@PreAuthorize` por perfil
  (`ADMIN`, `FUNCIONARIO`, `CLIENTE`), e o `RotasProtegidasTest` verifica todas as rotas protegidas a cada build.
- **IDOR:** o cliente só vê as próprias OS. O identificador vem do token, não da URL (testado com dois clientes).
- **Senhas:** bcrypt (`DelegatingPasswordEncoder`) e `PoliticaSenha`.
- **Injeção:** Spring Data JPA, com JPQL e queries nativas só com parâmetros nomeados.
- **CSRF:** desligado de forma justificada, porque a API é stateless e o token vai no header, sem cookie.
- **Headers:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e `Cache-Control: no-store`. A API só responde
  JSON.
- **Erros:** `include-stacktrace: never`, mensagem genérica e `traceId` no 500.

## 7. Como reproduzir

Evidências brutas em [`evidencias/`](evidencias/), sem segredos (conferidas contra as chaves e as senhas usadas no teste):

- `gitleaks/regras.toml`: regras padrão + `oficina-jwt-secret`;
- `gitleaks/relatorio-redigido.json`: segredo redigido e e-mail dos autores removido;
- `zap/antes/` e `zap/depois/`: relatórios em HTML (para leitura) e em JSON;
- `trivy/antes.json` e `trivy/depois.json`.

Na raiz do repositório:

```bash
# Gitleaks: histórico completo, segredos redigidos
docker run --rm -v "$PWD":/repo:ro -v "$PWD/docs/seguranca/evidencias/gitleaks":/cfg zricethezav/gitleaks:v8.21.2 \
  detect --source /repo --log-opts="--all" --config /cfg/regras.toml --redact \
  --report-format json --report-path /cfg/relatorio.json

# Trivy: imagem da aplicação
docker build -t oficina-mecanica-api:scan .
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock:ro aquasec/trivy:0.58.1 image \
  --db-repository public.ecr.aws/aquasecurity/trivy-db:2 \
  --java-db-repository public.ecr.aws/aquasecurity/trivy-java-db:1 oficina-mecanica-api:scan

# OWASP ZAP: com o Compose no ar e um token de ADMIN obtido em POST /api/v1/auth/login (sem o prefixo "Bearer ")
docker run --rm --network oficina-mecanica-api_default -v "$PWD/zap":/zap/wrk:rw zaproxy/zap-stable:2.16.1 zap-api-scan.py \
  -t http://oficina-mecanica-api:8081/api/v1/api-docs-personalized -f openapi \
  -O http://oficina-mecanica-api:8081 -r zap-relatorio.html -J zap-relatorio.json \
  -z "-config replacer.full_list(0).description=auth -config replacer.full_list(0).enabled=true \
      -config replacer.full_list(0).matchtype=REQ_HEADER -config replacer.full_list(0).matchstr=Authorization \
      -config replacer.full_list(0).regex=false -config replacer.full_list(0).replacement=Bearer\ $TOKEN"
```

O Trivy usa o mirror da AWS porque o `ghcr.io` limita downloads anônimos e o download da base pode travar. Nunca use as
chaves A, B ou C para subir o ambiente de teste: gere um `API_SECRET` novo.

## Referências

- [ADR 004 — Segurança: autenticação e autorização](../adr/0004-seguranca.md)
- [OWASP Top 10 (2021)](https://owasp.org/Top10/)
- [Gitleaks](https://github.com/gitleaks/gitleaks), [OWASP ZAP](https://www.zaproxy.org/docs/docker/api-scan/),
  [Trivy](https://trivy.dev/)
