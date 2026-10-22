# Oficina Mecânica API

API para gestão de oficina mecânica, construída com Java 25 e Spring Boot 4, usando PostgreSQL como banco de dados.

## Pré-requisitos

- [Git](https://git-scm.com/downloads)
- [Docker](https://docs.docker.com/get-docker/)

## Rodando a aplicação

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

## Acessar

- API: http://localhost:8081/api/v1
- Swagger: http://localhost:8081/api/v1/documentation
- Health check: http://localhost:8081/api/v1/actuator/health

## Comandos úteis

```bash
docker compose logs -f api   # acompanhar logs da API
docker compose ps            # status dos containers
docker compose down          # parar os containers
docker compose down -v       # parar e apagar os dados do banco
```
