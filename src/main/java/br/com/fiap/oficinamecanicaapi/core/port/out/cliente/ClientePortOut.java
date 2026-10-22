package br.com.fiap.oficinamecanicaapi.core.port.out.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Optional;

public interface ClientePortOut {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarAtivoPorId(Long id);

    Optional<Cliente> buscarAtivoPorDocumento(Documento documento);

    /** Considera também clientes inativos, pois o documento é único na tabela. */
    boolean existePorDocumento(Documento documento);

    Pagina<Cliente> listarAtivos(int pagina, int tamanho);
}
