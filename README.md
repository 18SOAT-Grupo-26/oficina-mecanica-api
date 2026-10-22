# Oficina Mecânica API

## Rodando com Docker

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) com Docker Compose

### 1. Configurar as variáveis de ambiente

Copie o arquivo de exemplo e ajuste os valores:

```bash
cp .env.example .env
```

| Variável            | Descrição                                       |
|---------------------|-------------------------------------------------|
| `POSTGRES_DB`       | Nome do banco de dados                          |
| `POSTGRES_USER`     | Usuário do Postgres                             |
| `POSTGRES_PASSWORD` | Senha do Postgres                               |
| `API_SECRET`        | Chave de assinatura do JWT (base64, mín. 64 bytes) |

### 2. Subir os containers

```bash
docker compose up -d --build
```

Isso sobe o Postgres (`db`) e a API (`api`). A API só inicia depois que o banco estiver saudável.

### 3. Acessar

- API: http://localhost:8081/api/v1
- Swagger: http://localhost:8081/api/v1/documentation

### Comandos úteis

```bash
docker compose logs -f api   # acompanhar logs da API
docker compose down          # parar os containers
docker compose down -v       # parar e apagar os dados do banco
```
