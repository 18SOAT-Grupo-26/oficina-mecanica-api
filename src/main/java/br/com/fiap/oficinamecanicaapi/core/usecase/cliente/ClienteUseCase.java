package br.com.fiap.oficinamecanicaapi.core.usecase.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.cliente.mapper.ClienteMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClienteUseCase implements ClienteUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ClienteUseCase.class);

    static final int PAGINA_PADRAO = 0;
    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    private final ClientePortOut clientePortOut;
    private final ClienteMapper clienteMapper;

    public ClienteUseCase(ClientePortOut clientePortOut, ClienteMapper clienteMapper) {
        this.clientePortOut = clientePortOut;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public ClienteResponse criar(ClienteRequest request) throws ApplicationException {
        Documento documento = new Documento(request.documento());
        validarDocumentoDisponivel(documento);

        Cliente cliente = Cliente.novo(request.nome(), documento, request.email(), request.telefone(),
                request.endereco());
        Cliente salvo = clientePortOut.salvar(cliente);

        log.info("action=cliente_criado id={}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Override
    public ClienteResponse buscarPorId(Long id) throws ApplicationException {
        return clienteMapper.toResponse(buscarClienteAtivo(id));
    }

    @Override
    public ClienteResponse buscarPorDocumento(String documento) throws ApplicationException {
        return clientePortOut.buscarAtivoPorDocumento(new Documento(documento))
                .map(clienteMapper::toResponse)
                .orElseThrow(ClienteUseCase::clienteNaoEncontrado);
    }

    @Override
    public Pagina<ClienteResponse> listar(Integer pagina, Integer tamanho) {
        int paginaValida = pagina == null || pagina < 0 ? PAGINA_PADRAO : pagina;
        int tamanhoValido = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        return clientePortOut.listarAtivos(paginaValida, tamanhoValido).map(clienteMapper::toResponse);
    }

    @Override
    public ClienteResponse atualizar(Long id, ClienteRequest request) throws ApplicationException {
        Cliente cliente = buscarClienteAtivo(id);

        Documento documento = new Documento(request.documento());
        if (!documento.equals(cliente.getDocumento())) {
            validarDocumentoDisponivel(documento);
        }

        cliente.atualizar(request.nome(), documento, request.email(), request.telefone(), request.endereco());
        Cliente salvo = clientePortOut.salvar(cliente);

        log.info("action=cliente_atualizado id={}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Override
    public void desativar(Long id) throws ApplicationException {
        Cliente cliente = buscarClienteAtivo(id);
        cliente.desativar();
        clientePortOut.salvar(cliente);

        log.info("action=cliente_desativado id={}", id);
    }

    private Cliente buscarClienteAtivo(Long id) {
        return clientePortOut.buscarAtivoPorId(id).orElseThrow(ClienteUseCase::clienteNaoEncontrado);
    }

    private void validarDocumentoDisponivel(Documento documento) {
        if (clientePortOut.existePorDocumento(documento)) {
            throw new ApplicationException(409, "Já existe um cliente cadastrado com este documento");
        }
    }

    private static ApplicationException clienteNaoEncontrado() {
        return new ApplicationException(404, "Cliente não encontrado");
    }
}
