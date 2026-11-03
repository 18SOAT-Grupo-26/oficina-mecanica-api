package br.com.fiap.oficinamecanicaapi.core.domain.cliente.model;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Agregado raiz do contexto de cliente. O estado só muda pelos métodos de negócio
 * ({@link #atualizar}, {@link #desativar}); não há setters livres.
 */
@Getter
public class Cliente {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final Long id;
    private String nome;
    private Documento documento;
    private String email;
    private String telefone;
    private Endereco endereco;
    private boolean ativo;
    private final OffsetDateTime criadoEm;
    private final OffsetDateTime atualizadoEm;

    private Cliente(Long id, String nome, Documento documento, String email, String telefone,
                    Endereco endereco, boolean ativo, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        this.id = id;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        aplicarDados(nome, documento, email, telefone, endereco);
    }

    /**
     * Cria um cliente novo, ainda não persistido e ativo.
     */
    public static Cliente novo(String nome, Documento documento, String email, String telefone, Endereco endereco) {
        return new Cliente(null, nome, documento, email, telefone, endereco, true, null, null);
    }

    /**
     * Reconstrói um cliente já persistido (uso exclusivo dos adapters de saída).
     */
    public static Cliente reconstituir(Long id, String nome, Documento documento, String email, String telefone,
                                       Endereco endereco, boolean ativo,
                                       OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        return new Cliente(id, nome, documento, email, telefone, endereco, ativo, criadoEm, atualizadoEm);
    }

    public void atualizar(String nome, Documento documento, String email, String telefone, Endereco endereco) {
        aplicarDados(nome, documento, email, telefone, endereco);
    }

    public void desativar() {
        this.ativo = false;
    }

    private void aplicarDados(String nome, Documento documento, String email, String telefone, Endereco endereco) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome do cliente é obrigatório");
        }
        if (documento == null) {
            throw new DadoInvalidoException("O documento do cliente é obrigatório");
        }
        if (email == null || !email.trim().matches(EMAIL_REGEX)) {
            throw new DadoInvalidoException("E-mail inválido — deve conter '@' e um domínio válido");
        }
        if (telefone == null || telefone.isBlank()) {
            throw new DadoInvalidoException("O telefone do cliente é obrigatório");
        }
        this.nome = nome.trim();
        this.documento = documento;
        this.email = email.trim();
        this.telefone = telefone.trim();
        this.endereco = endereco;
    }
}
