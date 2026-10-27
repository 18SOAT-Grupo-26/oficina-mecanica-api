package br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

import java.util.List;

/**
 * Casos de uso da OS. Cliente, veículo, serviço e peça já existem nos próprios cadastros; a OS só os
 * referencia. Os dados (itens) mudam pelo {@link #atualizar} e o status manual, pelo
 * {@link #alterarStatus}. Os registros do orçamento (enviado, aprovado, rejeitado) são chamados pelo
 * contexto de orçamento (P3), na mesma transação dele.
 */
public interface OrdemServicoUseCasePortIn {

    /** Abre a OS para o cliente e o veículo, já com os itens que vierem; ela vai sozinha de RECEBIDA para EM_DIAGNOSTICO. */
    OrdemServicoResponse criar(OrdemServicoRequest request) throws ApplicationException;

    OrdemServicoResponse consultar(Long id) throws ApplicationException;

    List<OrdemServicoResponse> listar(Long idCliente) throws ApplicationException;

    /**
     * Atualização completa dos dados (cliente, veículo, itens): todos os campos são obrigatórios, e
     * lista vazia remove os itens daquele type. Campo igual ao atual não conta como alteração.
     */
    OrdemServicoResponse atualizar(Long id, OrdemServicoRequest request) throws ApplicationException;

    /** Leva a OS ao status pedido, que precisa ser o próximo da sequência (não volta, não pula, não repete). */
    OrdemServicoResponse alterarStatus(Long id, OrdemServicoRequest.Status request) throws ApplicationException;

    /** O orçamento foi enviado ao cliente: a OS vai para AGUARDANDO_APROVACAO. */
    OrdemServicoResponse registrarOrcamentoEnviado(Long id) throws ApplicationException;

    /** O cliente aprovou o orçamento: a OS vai para EM_EXECUCAO. */
    OrdemServicoResponse registrarOrcamentoAprovado(Long id) throws ApplicationException;

    /** O cliente rejeitou o orçamento: a OS vai para CANCELADA. */
    OrdemServicoResponse registrarOrcamentoRejeitado(Long id) throws ApplicationException;

    TempoMedioExecucaoResponse consultarTempoMedioExecucao();
}
