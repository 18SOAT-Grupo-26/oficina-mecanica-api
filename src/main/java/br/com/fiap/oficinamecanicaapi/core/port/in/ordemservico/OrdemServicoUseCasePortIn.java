package br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico;


import java.util.List;

/**
 * Casos de uso da OS. Cliente, veículo, serviço e peça já existem nos próprios cadastros; a OS só os
 * referencia. Os dados (itens) mudam pelo {@link #atualizar} e o status manual, pelo
 * {@link #alterarStatus}. Os registros do orçamento (enviado, aprovado, rejeitado) são chamados pelo
 * contexto de orçamento (P3), na mesma transação dele.
 */
public interface OrdemServicoUseCasePortIn {

    /**
     * Abre a OS para o cliente e o veículo, já com os itens que vierem; ela vai sozinha de RECEBIDA para EM_DIAGNOSTICO.
     */
    OrdemServicoResponse criar(OrdemServicoRequest request);

    OrdemServicoResponse consultar(Long id);

    List<OrdemServicoResponse> listar(Long idCliente);

    /**
     * Consulta feita pelo próprio cliente: a OS de outro cliente responde 404, como se não existisse.
     */
    OrdemServicoResponse consultarDoCliente(Long id, Long idCliente);

    /**
     * Atualização completa dos dados (cliente, veículo, itens): todos os campos são obrigatórios, e
     * lista vazia remove os itens daquele type. Campo igual ao atual não conta como alteração.
     */
    OrdemServicoResponse atualizar(Long id, OrdemServicoRequest request);

    /**
     * Leva a OS ao status pedido, que precisa ser o próximo da sequência (não volta, não pula, não repete).
     */
    OrdemServicoResponse alterarStatus(Long id, OrdemServicoRequest.Status request);

    /**
     * O orçamento foi enviado ao cliente: a OS vai para AGUARDANDO_APROVACAO.
     */
    OrdemServicoResponse registrarOrcamentoEnviado(Long id);

    /**
     * O cliente aprovou o orçamento: a OS vai para EM_EXECUCAO.
     */
    OrdemServicoResponse registrarOrcamentoAprovado(Long id);

    /**
     * O cliente rejeitou o orçamento: a OS vai para CANCELADA.
     */
    OrdemServicoResponse registrarOrcamentoRejeitado(Long id);

    TempoMedioExecucaoResponse consultarTempoMedioExecucao();

    /**
     * Soft delete da OS: leva a CANCELADA, como o {@link #alterarStatus} com CANCELADA, sem removê-la.
     */
    void cancelar(Long id);
}
