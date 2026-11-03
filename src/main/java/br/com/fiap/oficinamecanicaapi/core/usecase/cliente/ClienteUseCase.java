package br.com.fiap.oficinamecanicaapi.core.usecase.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.cliente.mapper.ClienteMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteUseCase implements ClienteUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ClienteUseCase.class);

    private final ClientePortOut clientePortOut;
    private final ClienteMapper clienteMapper;
    private final MeterRegistry meterRegistry;

    public ClienteUseCase(ClientePortOut clientePortOut,
                          ClienteMapper clienteMapper,
                          MeterRegistry meterRegistry) {
        this.clientePortOut = clientePortOut;
        this.clienteMapper = clienteMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Documento documento = new Documento(request.documento());
        validarDocumentoDisponivel(documento);

        Cliente cliente = Cliente.novo(request.nome(), documento, request.email(), request.telefone(),
                request.endereco());
        Cliente salvo = clientePortOut.salvar(cliente);

        contar("criado");
        log.info("action=cliente_criado id={}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return clienteMapper.toResponse(buscarClienteAtivo(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ClienteResponse> listar(String documento, Integer pagina, Integer tamanho) {
        int numero = Pagina.paginaValida(pagina);
        int itensPorPagina = Pagina.tamanhoValido(tamanho);
        Pagina<Cliente> clientes = documento == null
                ? clientePortOut.listarAtivos(numero, itensPorPagina)
                : Pagina.deNoMaximoUm(clientePortOut.buscarAtivoPorDocumento(new Documento(documento)), numero, itensPorPagina);
        return clientes.map(clienteMapper::toResponse);
    }

    @Override
    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarClienteAtivo(id);
        Documento documento = new Documento(request.documento());
        if (!documento.equals(cliente.getDocumento())) {
            validarDocumentoDisponivel(documento);
            log.info("action=cliente_documento_alterado id={}", id);
        }

        cliente.atualizar(request.nome(), documento, request.email(), request.telefone(), request.endereco());
        Cliente salvo = clientePortOut.salvar(cliente);

        contar("atualizado");
        log.info("action=cliente_atualizado id={}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Cliente cliente = buscarClienteAtivo(id);
        cliente.desativar();
        clientePortOut.salvar(cliente);

        contar("desativado");
        log.info("action=cliente_desativado id={}", id);
    }

    private Cliente buscarClienteAtivo(Long id) {
        return clientePortOut.buscarAtivoPorId(id).orElseThrow(ClienteUseCase::clienteNaoEncontrado);
    }

    private void validarDocumentoDisponivel(Documento documento) {
        if (clientePortOut.existePorDocumento(documento)) {
            throw new ConflitoException("Já existe um cliente cadastrado com este documento");
        }
    }

    private static RecursoNaoEncontradoException clienteNaoEncontrado() {
        return new RecursoNaoEncontradoException("Cliente não encontrado");
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "cliente", "operacao", operacao).increment();
    }
}
