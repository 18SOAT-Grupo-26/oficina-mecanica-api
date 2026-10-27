package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ordem de Serviço: abertura, consulta, itens e status. Toda resposta traz a OS com itens e histórico de status. */
public interface OrdemServicoAPI {

    @Operation(summary = "Criar OS",
            description = "Toda OS nasce RECEBIDA e vai sozinha para EM_DIAGNOSTICO (os dois ficam no histórico). Cliente (CPF/CNPJ), veículo, serviços e peças são obrigatórios; as listas de itens podem vir vazias. Todos devem estar cadastrados, e o preço do item fica congelado. Cliente e veículo não mudam depois.",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "201", description = "OS criada", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cliente, veículo, serviços ou peças ausentes, ou campos inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Body ausente ou ilegível", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente, veículo, serviço ou peça não cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Veículo de outro cliente, item repetido ou saldo insuficiente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody OrdemServicoRequest request) throws ApplicationException;

    @Operation(summary = "Consultar OS", description = "Status, itens e histórico.", tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "OS encontrada", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<OrdemServicoResponse> consultar(@PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Listar OS", description = "Status, itens e histórico.", tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "OS encontradas", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Nenhuma OS encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/listar/{idCliente}")
    ResponseEntity<List<OrdemServicoResponse>> listar(@PathVariable Long idCliente) throws ApplicationException;

    @Operation(summary = "Atualizar dados da OS",
            description = """
                    Atualização completa, com o mesmo body da criação: cliente, veículo, serviços e peças são obrigatórios.
                    Lista vazia remove os itens daquele tipo; dado igual ao atual não conta como alteração.
                    A partir de EM_EXECUCAO (e com a OS cancelada) nada pode ser editado.
                    Cliente e veículo precisam ser os da abertura (outro responde 409: cancele a OS e abra outra).
                    Itens só mudam em EM_DIAGNOSTICO. O status não muda por aqui.""",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "OS atualizada", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cliente, veículo, serviços ou peças ausentes, ou campos inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "OS ou cadastro não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "OS em EM_EXECUCAO ou depois, cliente ou veículo diferente, itens fora de EM_DIAGNOSTICO, item repetido ou saldo insuficiente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<OrdemServicoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody OrdemServicoRequest request) throws ApplicationException;

    @Operation(summary = "Mudar status da OS",
            description = """
                    Só avança para o próximo status: não volta, não pula e não repete o atual.
                    Manuais: FINALIZADA e ENTREGUE.
                    CANCELADA cancela a OS sem removê-la: só até AGUARDANDO_APROVACAO.
                    EM_DIAGNOSTICO vem da abertura da OS; AGUARDANDO_APROVACAO e EM_EXECUCAO, do envio e da aprovação do orçamento.""",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "Status alterado", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Status de destino ausente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Transição inválida ou status automático", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Status desconhecido ou body ilegível", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PatchMapping("/{id}/status")
    ResponseEntity<OrdemServicoResponse> alterarStatus(@PathVariable Long id, @Valid @RequestBody OrdemServicoRequest.Status request) throws ApplicationException;


    @Operation(summary = "Gerar Orcamento",
            description = "Gera Orçamento",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "Orçamento gerado", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Conflito ao gerar orçamento", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Entidade não processável", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PatchMapping("/{id}/gerar-orcamento")
    ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable Long id) throws ApplicationException;


    @Operation(summary = "Aprovar orçamento",
            description = "Aprova o orçamento enviado: a OS vai para EM_EXECUCAO e o estoque reservado das peças é baixado. Só vale para OS em AGUARDANDO_APROVACAO.",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "Orçamento aprovado", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "OS fora de AGUARDANDO_APROVACAO ou saldo reservado insuficiente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PatchMapping("/{id}/aprovar-orcamento")
    ResponseEntity<OrdemServicoResponse> aprovarOrcamento(@PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Rejeitar orçamento",
            description = "Rejeita o orçamento enviado: a OS é cancelada e a reserva de estoque das peças é liberada, sem consumo. Só vale para OS em AGUARDANDO_APROVACAO.",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "Orçamento rejeitado", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "OS fora de AGUARDANDO_APROVACAO ou reserva inconsistente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PatchMapping("/{id}/rejeitar-orcamento")
    ResponseEntity<OrdemServicoResponse> rejeitarOrcamento(@PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Consultar tempo médio de execução",
            description = "Média de EM_EXECUCAO a FINALIZADA das OS que passaram por FINALIZADA (inclusive as entregues), em minutos truncados. Sem OS finalizada, a média vem nula.",
            tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "Tempo médio calculado", content = @Content(schema = @Schema(implementation = TempoMedioExecucaoResponse.class)))
    @GetMapping("/tempo-medio-execucao")
    ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao();
}
