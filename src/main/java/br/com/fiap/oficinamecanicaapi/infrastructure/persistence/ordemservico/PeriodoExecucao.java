package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import java.time.OffsetDateTime;

/** Entrada em EM_EXECUCAO e entrada em FINALIZADA de uma OS. */
public record PeriodoExecucao(OffsetDateTime inicio, OffsetDateTime fim) {
}
