# Diagramas de sequência: Ordem de Serviço

Fluxo principal da fase 1, do jeito que está no código: abertura da OS, orçamento (gerar → aprovar ou rejeitar) e
mudança manual de status. As camadas seguem a [ADR 001](../adr/0001-monolito-em-camadas.md), as transações e o estoque a
[ADR 003](../adr/0003-estrategia-de-transacoes.md) e os perfis a [ADR 004](../adr/0004-seguranca.md).

Convenções dos diagramas:

- Todas as rotas ficam sob `/api/v1` e exigem `Authorization: Bearer <token>` de um usuário `ADMIN` ou `FUNCIONARIO`.
- Os `*PortOut` são interfaces do `core`. Quem fala com o PostgreSQL são os `*PersistenceAdapter` em
  `infrastructure/persistence/<contexto>/`, que aparecem aqui só pelo SQL que geram.
- A área destacada é a transação do caso de uso (`@Transactional` no `OrdemServicoUseCase`). Qualquer
  `ApplicationException` dentro dela desfaz tudo, e o `HandlerException` devolve o `ErrorTemplate` com o status
  (400, 404 ou 409).

## 1. Abertura da OS

`POST /ordens-servico`. A OS entra como `RECEBIDA` e vai sozinha para `EM_DIAGNOSTICO`, porque já tem cliente e veículo.
O preço de cada item é copiado do cadastro na inclusão, e a peça só tem o saldo conferido: a reserva fica para o
orçamento.

```mermaid
sequenceDiagram
    autonumber
    actor F as Funcionário ou admin
    participant SEC as JwtAuthenticationFilter<br/>+ @PreAuthorize
    participant C as OrdemServicoController
    participant UC as OrdemServicoUseCase
    participant OS as OrdemServico<br/>(agregado)
    participant CAD as ClientePortOut, VeiculoPortOut,<br/>ServicoPortOut, PecaInsumoPortOut
    participant OSP as OrdemServicoPortOut
    participant DB as PostgreSQL

    F->>SEC: POST /ordens-servico<br/>{documentoCliente, idVeiculo, servicos, pecas}
    SEC->>SEC: valida o token e carrega o perfil
    break sem token válido ou perfil CLIENTE
        SEC-->>F: 401 ou 403
    end
    SEC->>C: criar(request)
    Note over C: @Valid no corpo: 400 se faltar campo
    C->>UC: criar(request)

    rect rgba(80, 140, 220, 0.12)
        UC->>CAD: buscarAtivoPorDocumento(documento)
        CAD->>DB: SELECT clientes
        Note right of UC: 404 se o cliente não existir
        UC->>CAD: buscarAtivoPorId(idVeiculo)
        CAD->>DB: SELECT veiculos
        Note right of UC: 404 se não existir, 409 se for de outro cliente
        UC->>OS: abrir(idCliente, idVeiculo, agora)
        OS-->>UC: OS em EM_DIAGNOSTICO<br/>(histórico: RECEBIDA, EM_DIAGNOSTICO)
        loop cada serviço
            UC->>CAD: buscarPorId(idServico)
            UC->>OS: incluirServico(id, nome, preço, quantidade)
        end
        loop cada peça
            UC->>CAD: buscarPorId(idPeca)
            UC->>UC: exigirSaldo: quantidade - reservado
            Note right of UC: 409 se o saldo não cobrir
            UC->>OS: incluirPeca(id, nome, preço, quantidade)
        end
        UC->>OSP: salvar(os)
        OSP->>DB: INSERT ordem_servico, item_servico_os,<br/>item_peca_os, historico_status_os
        OSP-->>UC: OS com os ids
    end

    UC->>UC: métrica oficina.os.criadas, log os_criada
    UC-->>C: OrdemServicoResponse
    C-->>F: 201 Created
```

## 2. Orçamento: gerar, aprovar ou rejeitar

O orçamento não é uma entidade separada: é o conjunto de itens da OS, que fica congelado ao ser gerado. A decisão do
cliente acontece fora do sistema, e o funcionário a registra (ADR 004). Cada passo é uma requisição e uma transação.

O estoque muda por `UPDATE` condicional atômico (ADR 003, seção 2): se a condição do `WHERE` não vale, nenhuma linha é
atualizada, o caso de uso devolve 409 e a transação inteira volta. As peças são atualizadas em ordem crescente de `id`,
para duas transações que disputam as mesmas peças travarem as linhas na mesma ordem e não darem deadlock.

```mermaid
sequenceDiagram
    autonumber
    actor F as Funcionário ou admin
    participant C as OrdemServicoController
    participant UC as OrdemServicoUseCase
    participant OS as OrdemServico<br/>(agregado)
    participant PEC as PecaInsumoPortOut
    participant OSP as OrdemServicoPortOut
    participant DB as PostgreSQL

    Note over F,DB: Gerar o orçamento: EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
    F->>C: PATCH /ordens-servico/{id}/gerar-orcamento
    C->>UC: registrarOrcamentoEnviado(id)
    rect rgba(80, 140, 220, 0.12)
        UC->>OSP: buscarPorId(id)
        OSP->>DB: SELECT ordem_servico e itens
        Note right of UC: 404 se a OS não existir
        UC->>OS: aguardarAprovacao(agora)
        Note right of OS: 409 se não estiver em EM_DIAGNOSTICO<br/>ou não tiver nenhum item
        loop cada peça, em ordem crescente de id
            UC->>PEC: reservar(idPeca, quantidade)
            PEC->>DB: UPDATE peca_insumo SET reservado = reservado + q<br/>WHERE id = :id AND quantidade - reservado >= q
            DB-->>PEC: linhas atualizadas (0 ou 1)
            Note right of UC: 0 linhas: 409 "Saldo insuficiente para reservar"
        end
        UC->>OSP: salvar(os)
        OSP->>DB: UPDATE ordem_servico, INSERT historico_status_os
    end
    UC->>UC: métricas oficina.os.transicoes e oficina.os.orcamento.valor
    C-->>F: 200 OK (AGUARDANDO_APROVACAO)

    Note over F,DB: O cliente decide fora do sistema e o funcionário registra a decisão

    alt cliente aprovou: AGUARDANDO_APROVACAO → EM_EXECUCAO
        F->>C: PATCH /ordens-servico/{id}/aprovar-orcamento
        C->>UC: registrarOrcamentoAprovado(id)
        rect rgba(80, 140, 220, 0.12)
            UC->>OSP: buscarPorId(id)
            UC->>OS: iniciarExecucao(agora)
            Note right of OS: 409 se não estiver em AGUARDANDO_APROVACAO
            loop cada peça, em ordem crescente de id
                UC->>PEC: baixar(idPeca, quantidade)
                PEC->>DB: UPDATE peca_insumo SET quantidade = quantidade - q,<br/>reservado = reservado - q<br/>WHERE id = :id AND reservado >= q AND quantidade >= q
            end
            UC->>OSP: salvar(os)
            OSP->>DB: UPDATE ordem_servico, INSERT historico_status_os
        end
        C-->>F: 200 OK (EM_EXECUCAO)
    else cliente rejeitou: AGUARDANDO_APROVACAO → CANCELADA
        F->>C: PATCH /ordens-servico/{id}/rejeitar-orcamento
        C->>UC: registrarOrcamentoRejeitado(id)
        rect rgba(80, 140, 220, 0.12)
            UC->>OSP: buscarPorId(id)
            UC->>OS: rejeitarOrcamento(agora)
            Note right of OS: 409 se não estiver em AGUARDANDO_APROVACAO
            loop cada peça, em ordem crescente de id
                UC->>PEC: liberar(idPeca, quantidade)
                PEC->>DB: UPDATE peca_insumo SET reservado = reservado - q<br/>WHERE id = :id AND reservado >= q
            end
            UC->>OSP: salvar(os)
            OSP->>DB: UPDATE ordem_servico, INSERT historico_status_os
        end
        C-->>F: 200 OK (CANCELADA)
    end
    Note right of UC: baixar ou liberar com 0 linhas: 409<br/>"Não há reserva suficiente" e rollback
```

## 3. Mudança manual de status e cancelamento

Pelo `PATCH /status` só se finaliza, entrega ou cancela. Os outros status são automáticos (vêm da abertura e do
orçamento) e são recusados com 409. O `DELETE /ordens-servico/{id}` não apaga a OS: é o mesmo cancelamento, liberado
só para o `ADMIN`.

```mermaid
sequenceDiagram
    autonumber
    actor F as Funcionário ou admin
    participant C as OrdemServicoController
    participant UC as OrdemServicoUseCase
    participant OS as OrdemServico<br/>(agregado)
    participant PEC as PecaInsumoPortOut
    participant OSP as OrdemServicoPortOut
    participant DB as PostgreSQL

    alt PATCH /ordens-servico/{id}/status {status}
        F->>C: alterarStatus(id, status)
        C->>UC: alterarStatus(id, status)
    else DELETE /ordens-servico/{id} (só ADMIN)
        F->>C: cancelar(id)
        C->>UC: cancelar(id) = alterarStatus(id, CANCELADA)
    end

    rect rgba(80, 140, 220, 0.12)
        UC->>OSP: buscarPorId(id)
        OSP->>DB: SELECT ordem_servico e itens
        break destino EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO ou RECEBIDA
            UC-->>F: 409 (status automático ou transição inválida)
        end
        alt FINALIZADA (de EM_EXECUCAO)
            UC->>OS: finalizar(agora)
            UC->>UC: timer oficina.os.tempo.execucao
        else ENTREGUE (de FINALIZADA)
            UC->>OS: entregar(agora)
            UC->>UC: timer oficina.os.tempo.total
        else CANCELADA (até AGUARDANDO_APROVACAO)
            UC->>OS: cancelar(agora)
            opt a OS estava em AGUARDANDO_APROVACAO
                loop cada peça, em ordem crescente de id
                    UC->>PEC: liberar(idPeca, quantidade)
                    PEC->>DB: UPDATE peca_insumo SET reservado = reservado - q<br/>WHERE id = :id AND reservado >= q
                end
            end
        end
        Note right of OS: 409 se a transição não for permitida<br/>(a OS só avança uma etapa e não cancela a partir de EM_EXECUCAO)
        UC->>OSP: salvar(os)
        OSP->>DB: UPDATE ordem_servico, INSERT historico_status_os
    end
    UC->>UC: métrica oficina.os.transicoes, log os_status_alterado
    C-->>F: 200 OK com a OS (PATCH) ou 204 No Content (DELETE)
```
