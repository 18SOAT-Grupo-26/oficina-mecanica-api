package br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico;

/**
 * Tempo médio de execução das OS que passaram por FINALIZADA (de EM_EXECUCAO a FINALIZADA).
 * {@code mediaMinutos} é nulo quando nenhuma OS foi considerada; os minutos são truncados.
 */
public record TempoMedioExecucaoResponse(
        Long mediaMinutos,
        int quantidadeOrdens
) {
}
