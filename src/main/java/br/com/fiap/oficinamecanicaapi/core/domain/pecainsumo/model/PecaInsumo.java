package br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.RegrasCatalogo;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.utils.NormalizadorNome;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Peça ou insumo do estoque, sem JPA. A entidade fica em {@code infrastructure/persistence/pecainsumo}.
 * Só muda pelos métodos com regra ({@link #novo}, {@link #atualizar}, {@link #desativar}), então o estoque nunca
 * fica incoerente ({@code 0 <= reservado <= quantidade}). Não é apagada: a OS antiga continua apontando para a peça
 * desativada; só não pode ser desativada enquanto houver unidades reservadas para orçamentos enviados. Reserva e baixa na OS continuam no UPDATE atômico do port (ADR 003).
 */
@Getter
public class PecaInsumo {

    private final Long id;
    private String nome;
    private String nomeNormalizado;
    private String descricao;
    private Integer quantidade;
    private Integer reservado;
    private BigDecimal preco;
    private boolean ativo;
    private final OffsetDateTime criadoEm;
    private final OffsetDateTime atualizadoEm;

    private PecaInsumo(Long id, String nome, String nomeNormalizado, String descricao, Integer quantidade,
                       Integer reservado, BigDecimal preco, boolean ativo, OffsetDateTime criadoEm,
                       OffsetDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.reservado = reservado;
        this.preco = preco;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Cria uma peça ou insumo novo, ainda não persistido e ativo; sem reservado informado, começa em zero.
     */
    public static PecaInsumo novo(String nome, String descricao, Integer quantidade, Integer reservado,
                                  BigDecimal preco) {
        PecaInsumo peca = new PecaInsumo(null, null, null, null, null, null, null, true, null, null);
        peca.aplicarDados(nome, descricao, quantidade, reservado, preco);
        return peca;
    }

    /**
     * Reconstrói uma peça já persistida (uso exclusivo dos adapters de saída). Não valida de novo: o dado já
     * passou pelas regras ao ser gravado, e um registro antigo não pode impedir a leitura.
     */
    public static PecaInsumo reconstituir(Long id, String nome, String nomeNormalizado, String descricao,
                                          Integer quantidade, Integer reservado, BigDecimal preco, boolean ativo,
                                          OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        return new PecaInsumo(id, nome, nomeNormalizado, descricao, quantidade, reservado, preco, ativo, criadoEm,
                atualizadoEm);
    }

    public void atualizar(String nome, String descricao, Integer quantidade, Integer reservado, BigDecimal preco) {
        aplicarDados(nome, descricao, quantidade, reservado, preco);
    }

    public void desativar() {
        if (reservado != null && reservado > 0) {
            throw new ConflitoException("A peça tem " + reservado
                    + " unidade(s) reservada(s) para orçamentos enviados e não pode ser desativada");
        }
        this.ativo = false;
    }

    private void aplicarDados(String nome, String descricao, Integer quantidade, Integer reservado,
                              BigDecimal preco) {
        String nomeValido = RegrasCatalogo.nome(nome);
        if (quantidade == null) {
            throw new DadoInvalidoException("O campo quantidade é obrigatório");
        }
        if (quantidade < 0) {
            throw new DadoInvalidoException("Quantidade inválida");
        }
        BigDecimal precoValido = RegrasCatalogo.preco(preco);
        int reservadoValido = reservado == null ? 0 : reservado;
        if (reservadoValido < 0 || reservadoValido > quantidade) {
            throw new DadoInvalidoException(
                    "Quantidade reservada inválida. Deve ser entre 0 e a quantidade em estoque");
        }
        String descricaoValida = RegrasCatalogo.descricao(descricao);
        this.nome = nomeValido;
        this.nomeNormalizado = NormalizadorNome.normalizar(nomeValido);
        this.descricao = descricaoValida;
        this.quantidade = quantidade;
        this.reservado = reservadoValido;
        this.preco = precoValido;
    }
}
