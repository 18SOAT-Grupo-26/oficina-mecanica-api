package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.RegrasCatalogo;
import br.com.fiap.oficinamecanicaapi.shared.utils.NormalizadorNome;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Serviço do catálogo, sem JPA. A entidade fica em {@code infrastructure/persistence/servico}.
 * Só muda pelos métodos com regra ({@link #novo}, {@link #atualizar}), então nunca fica em estado inválido.
 */
@Getter
public class Servico {

    private final Long id;
    private String nome;
    private String nomeNormalizado;
    private String descricao;
    private BigDecimal preco;
    private final OffsetDateTime criadoEm;
    private final OffsetDateTime atualizadoEm;

    private Servico(Long id, String nome, String nomeNormalizado, String descricao, BigDecimal preco,
                    OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.descricao = descricao;
        this.preco = preco;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Cria um serviço novo, ainda não persistido, validando nome, preço e descrição.
     */
    public static Servico novo(String nome, String descricao, BigDecimal preco) {
        Servico servico = new Servico(null, null, null, null, null, null, null);
        servico.aplicarDados(nome, descricao, preco);
        return servico;
    }

    /**
     * Reconstrói um serviço já persistido (uso exclusivo dos adapters de saída). Não valida de novo: o dado já
     * passou pelas regras ao ser gravado, e um registro antigo não pode impedir a leitura.
     */
    public static Servico reconstituir(Long id, String nome, String nomeNormalizado, String descricao, BigDecimal preco,
                                       OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        return new Servico(id, nome, nomeNormalizado, descricao, preco, criadoEm, atualizadoEm);
    }

    public void atualizar(String nome, String descricao, BigDecimal preco) {
        aplicarDados(nome, descricao, preco);
    }

    private void aplicarDados(String nome, String descricao, BigDecimal preco) {
        String nomeValido = RegrasCatalogo.nome(nome);
        BigDecimal precoValido = RegrasCatalogo.preco(preco);
        String descricaoValida = RegrasCatalogo.descricao(descricao);
        this.nome = nomeValido;
        this.nomeNormalizado = NormalizadorNome.normalizar(nomeValido);
        this.descricao = descricaoValida;
        this.preco = precoValido;
    }
}
