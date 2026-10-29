package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.Year;

/**
 * Agregado raiz do contexto de veículo. O estado só muda pelos métodos de negócio
 * ({@link #atualizar}, {@link #desativar}); não há setters livres.
 */
@Getter
public class Veiculo {

    static final int ANO_MINIMO = 1900;

    private final Long id;
    private Placa placa;
    private String marca;
    private String modelo;
    private Integer ano;
    private Long clienteId;
    private boolean ativo;
    private final OffsetDateTime criadoEm;
    private final OffsetDateTime atualizadoEm;

    private Veiculo(Long id, Placa placa, String marca, String modelo, Integer ano, Long clienteId,
                    boolean ativo, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        this.id = id;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        aplicarDados(placa, marca, modelo, ano, clienteId);
    }

    /**
     * Cria um veículo novo, ainda não persistido e ativo.
     */
    public static Veiculo novo(Placa placa, String marca, String modelo, Integer ano, Long clienteId) {
        return new Veiculo(null, placa, marca, modelo, ano, clienteId, true, null, null);
    }

    /**
     * Reconstrói um veículo já persistido (uso exclusivo dos adapters de saída).
     */
    public static Veiculo reconstituir(Long id, Placa placa, String marca, String modelo, Integer ano,
                                       Long clienteId, boolean ativo,
                                       OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        return new Veiculo(id, placa, marca, modelo, ano, clienteId, ativo, criadoEm, atualizadoEm);
    }

    public void atualizar(Placa placa, String marca, String modelo, Integer ano, Long clienteId) {
        aplicarDados(placa, marca, modelo, ano, clienteId);
    }

    public void desativar() {
        this.ativo = false;
    }

    /**
     * Aceita até o ano seguinte ao atual, pois modelos do próximo ano já são vendidos.
     */
    static int anoMaximo() {
        return Year.now(GetLocalTime.FUSO).getValue() + 1;
    }

    private void aplicarDados(Placa placa, String marca, String modelo, Integer ano, Long clienteId) {
        if (placa == null) {
            throw new ApplicationException(400, "A placa do veículo é obrigatória");
        }
        if (marca == null || marca.isBlank()) {
            throw new ApplicationException(400, "A marca do veículo é obrigatória");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new ApplicationException(400, "O modelo do veículo é obrigatório");
        }
        if (ano == null || ano < ANO_MINIMO || ano > anoMaximo()) {
            throw new ApplicationException(400,
                    "Ano inválido — deve estar entre " + ANO_MINIMO + " e " + anoMaximo());
        }
        if (clienteId == null) {
            throw new ApplicationException(400, "O cliente do veículo é obrigatório");
        }
        this.placa = placa;
        this.marca = marca.trim();
        this.modelo = modelo.trim();
        this.ano = ano;
        this.clienteId = clienteId;
    }
}
