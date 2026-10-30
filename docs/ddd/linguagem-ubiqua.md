# Linguagem ubíqua

Cada termo tem uma única definição, usada do mesmo jeito no código, nos boards do Event Storming e na API.

Event Storming no Miro: https://miro.com/app/board/uXjVEYmxgDM=/

## Glossário

| Termo | Definição |
|---|---|
| **Ordem de Serviço (OS)** | Registro de um atendimento a um veículo, da abertura à entrega. Cliente e veículo são definidos na abertura e não mudam depois. |
| **Cliente** | Pessoa física ou jurídica identificada pelo documento (CPF ou CNPJ). |
| **Documento** | CPF (11 dígitos) ou CNPJ (14 caracteres) válido, guardado sem máscara. É único: não existem dois clientes com o mesmo documento. |
| **Endereço** | Objeto de valor do Cliente (logradouro, número, bairro, cidade, estado, CEP e complemento), sem regras de validação. |
| **Veículo** | Automóvel de um cliente, identificado pela placa, com marca, modelo e ano. |
| **Placa** | Identificação do veículo no formato antigo (AAA9999) ou Mercosul (AAA9A99), guardada sem hífen e em maiúsculas. É única: não existem dois veículos com a mesma placa. |
| **Serviço** | Trabalho do catálogo da oficina, com nome único e preço. |
| **Peça ou insumo** | Item físico do catálogo, com preço, controlado em estoque pela quantidade e pelo reservado. O saldo disponível é a quantidade menos o reservado. |
| **Item de serviço / Item de peça** | Serviço ou peça incluído na OS, com quantidade e o preço congelado na inclusão: mudar o preço no catálogo não altera a OS. Uma peça só é incluída se houver saldo disponível. |
| **Valor monetário** | Valor em reais com duas casas decimais, nunca negativo. É o preço de cada item e o subtotal (preço × quantidade). |
| **Orçamento** | Conjunto dos itens da OS com o subtotal de cada um. A equipe envia o orçamento para aprovação e registra a decisão do cliente (aprovado ou rejeitado). |
| **Status da OS** | Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue e Cancelada. A OS avança uma etapa por vez, sem voltar nem pular. |
| **Diagnóstico** | Etapa que começa automaticamente na abertura da OS; nela ainda se alteram serviços e peças. |
| **Reserva de estoque** | Peça separada para a OS quando o orçamento é enviado para aprovação. Aumenta o reservado sem mudar a quantidade. |
| **Baixa de estoque** | Saída definitiva da peça quando o orçamento é aprovado e a OS entra em Em execução. Diminui a quantidade e o reservado. No código, a ação aparece como `AcaoEstoque.CONSUMIR` e o método é `baixar`. |
| **Liberação de estoque** | Devolução da reserva quando o orçamento é rejeitado ou a OS é cancelada em Aguardando aprovação. Diminui o reservado. |
| **Histórico de status** | Registro de cada mudança: status antigo, status atualizado, data e hora. O primeiro registro (abertura em Recebida) não tem status antigo. |
| **Tempo de execução** | Tempo entre Em execução e Finalizada, calculado pelo histórico. |
| **Tempo médio de execução** | Média dos tempos de execução das OS que chegaram a Finalizada (inclusive as entregues), em minutos. |
| **Perfil** | Administrador (tudo, inclusive exclusões e cadastro de usuários), Funcionário (opera cadastros, sem excluir, e OS) e Cliente (só consulta as próprias OS). |
| **Cancelada** | Status da OS rejeitada ou cancelada, possível até Aguardando aprovação. A OS nunca é removida: o `DELETE /ordens-servico/{id}` é um soft delete, que não apaga a OS do banco, só a leva para Cancelada. O efeito é o mesmo do `PATCH /ordens-servico/{id}/status` com CANCELADA, mas só o Administrador pode chamar o DELETE. |

## Relação entre a documentação e o código

O código não tem eventos de domínio: cada evento do Event Storming corresponde à transição de status gravada no
histórico ou ao resultado do comando.

| Elemento do DDD | Termo | Classe ou método | Endpoint |
|---|---|---|---|
| Agregado raiz | Ordem de Serviço | `OrdemServico` | `/ordens-servico` |
| Agregado raiz | Cliente | `Cliente` | `/clientes` |
| Agregado raiz | Veículo | `Veiculo` | `/veiculos` |
| Agregado raiz | Serviço | `Servico` | `/servico` |
| Agregado raiz | Peça ou insumo | `PecaInsumo` | `/insumos` |
| Entidade | Item de serviço / Item de peça | `ItemServico`, `ItemPeca` (base `ItemOrdemServico`) | corpo do POST e do PUT da OS |
| Objeto de valor | Status da OS | `StatusOrdemServico` (enum) | `PATCH /ordens-servico/{id}/status` |
| Objeto de valor | Histórico de status | `RegistroHistorico` | resposta do GET da OS |
| Objeto de valor | Valor monetário | `ValorMonetario` | preço e subtotal dos itens |
| Objeto de valor | Documento / Placa / Endereço | `Documento`, `Placa`, `Endereco` | `/clientes`, `/veiculos` |
| Serviço de domínio | Tempo médio de execução | `CalculadoraTempoMedioExecucao.calcular` | `GET /ordens-servico/tempo-medio-execucao` |
| Comando → evento | Cadastrar cliente → Cliente cadastrado | `ClienteUseCase.criar`, `Cliente.novo` | `POST /clientes` |
| Comando → evento | Cadastrar veículo → Veículo cadastrado | `VeiculoUseCase.criar`, `Veiculo.novo` | `POST /veiculos` |
| Comando → evento | Abrir OS → OS aberta | `OrdemServicoUseCase.criar`, `OrdemServico.abrir` | `POST /ordens-servico` |
| Comando → evento | (automático na abertura) → Status atualizado: Em diagnóstico | `OrdemServico.abrir` | `POST /ordens-servico` |
| Comando → evento | Editar serviços e peças → Itens da OS alterados | `incluirServico`, `incluirPeca`, `removerServico`, `removerPeca` | `PUT /ordens-servico/{id}` |
| Comando → evento | Enviar orçamento → Orçamento enviado | `aguardarAprovacao`, `registrarOrcamentoEnviado` | `PATCH /ordens-servico/{id}/gerar-orcamento` |
| Comando → evento | Registrar aprovação → Orçamento aprovado | `iniciarExecucao`, `registrarOrcamentoAprovado` | `PATCH /ordens-servico/{id}/aprovar-orcamento` |
| Comando → evento | Registrar rejeição → Orçamento rejeitado | `rejeitarOrcamento`, `registrarOrcamentoRejeitado` | `PATCH /ordens-servico/{id}/rejeitar-orcamento` |
| Comando → evento | Finalizar OS → OS finalizada | `finalizar` | `PATCH /ordens-servico/{id}/status` (FINALIZADA) |
| Comando → evento | Entregar veículo → Veículo entregue | `entregar` | `PATCH /ordens-servico/{id}/status` (ENTREGUE) |
| Comando → evento | Cancelar OS → OS cancelada | `cancelar` | `PATCH /ordens-servico/{id}/status` (CANCELADA) · `DELETE /ordens-servico/{id}` |
| Comando → evento | Cadastrar serviço → Serviço cadastrado | `ServicoUseCase.criar` | `POST /servico` |
| Comando → evento | Cadastrar peça → Peça cadastrada | `PecaInsumoUseCase.criar` | `POST /insumos` |
| Comando → evento | Atualizar quantidade → Estoque ajustado | `PecaInsumoUseCase.atualizar` | `PUT /insumos/{id}` |
| Política | Toda mudança de status → Histórico de status gravado | `OrdemServico.registrarTransicao` | todos os comandos de status da OS |
| Política | Peça incluída na OS → Saldo conferido | `OrdemServicoUseCase.exigirSaldo` | `POST /ordens-servico` · `PUT /ordens-servico/{id}` |
| Política | Orçamento enviado → Reserva de estoque | `PecaInsumoPortOut.reservar` | `PATCH /ordens-servico/{id}/gerar-orcamento` |
| Política | Orçamento aprovado → Baixa de estoque | `PecaInsumoPortOut.baixar` | `PATCH /ordens-servico/{id}/aprovar-orcamento` |
| Política | Orçamento rejeitado ou OS cancelada em Aguardando aprovação → Liberação de estoque | `PecaInsumoPortOut.liberar` | `PATCH /ordens-servico/{id}/rejeitar-orcamento` · `PATCH /ordens-servico/{id}/status` (CANCELADA) · `DELETE /ordens-servico/{id}` |
| Modelo de leitura | OS com itens e histórico (equipe) | `OrdemServicoUseCase.consultar`, `listar` | `GET /ordens-servico/{id}` · `GET /ordens-servico/listar/{idCliente}` |
| Modelo de leitura | Andamento e histórico da OS (Cliente) | `MinhasOrdensController`, `OrdemServicoUseCase.consultarDoCliente` | `GET /minhas-ordens` · `GET /minhas-ordens/{id}` |
| Modelo de leitura | Tempo médio de execução | `consultarTempoMedioExecucao` | `GET /ordens-servico/tempo-medio-execucao` |
| Ator | Administrador | `Perfil.ADMIN` | tudo, inclusive `POST /auth/cadastro` e os `DELETE` |
| Ator | Funcionário | `Perfil.FUNCIONARIO` | cadastros (sem `DELETE`) e todas as rotas de `/ordens-servico`, exceto `DELETE /ordens-servico/{id}` |
| Ator | Cliente | `Perfil.CLIENTE` (usuário ligado ao cliente por `clienteId`) | `GET /minhas-ordens` · `GET /minhas-ordens/{id}` |

Todos os perfis se autenticam em `POST /auth/login` e chamam as demais rotas com o token JWT.
