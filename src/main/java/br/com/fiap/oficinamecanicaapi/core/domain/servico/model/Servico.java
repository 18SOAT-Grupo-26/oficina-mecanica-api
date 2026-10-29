package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Serviço do catálogo, sem JPA. A entidade fica em {@code infrastructure/persistence/servico}.
 */
@Getter
@Setter
public class Servico {

    private Long id;
    private String nome;
    private String nomeNormalizado;
    private String descricao;
    private BigDecimal preco;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;
}
