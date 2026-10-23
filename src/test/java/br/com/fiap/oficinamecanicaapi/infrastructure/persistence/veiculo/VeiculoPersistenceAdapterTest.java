package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente.ClienteEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente.ClienteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeiculoPersistenceAdapterTest {

    private static final Long ID = 5L;
    private static final Long CLIENTE_ID = 1L;
    private static final String PLACA = "BRA2E19";
    private static final String MARCA = "Volkswagen";
    private static final String MODELO = "Gol";
    private static final Integer ANO = 2020;
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-22T10:00:00-03:00");
    private static final PageRequest PRIMEIRA_PAGINA = PageRequest.of(0, 20, Sort.by("id"));

    @Mock
    private VeiculoJpaRepository repository;

    @Mock
    private ClienteJpaRepository clienteRepository;

    @Captor
    private ArgumentCaptor<VeiculoEntity> entityCaptor;

    private VeiculoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new VeiculoPersistenceAdapter(repository, clienteRepository,
                Mappers.getMapper(VeiculoPersistenceMapper.class));
    }

    @Test
    @DisplayName("Deve salvar o veículo vinculando a referência JPA do cliente pelo clienteId")
    void deveSalvarComReferenciaDoCliente() {
        ClienteEntity cliente = clienteEntity();
        Veiculo veiculo = Veiculo.novo(new Placa(PLACA), MARCA, MODELO, ANO, CLIENTE_ID);
        VeiculoEntity salvo = veiculoEntity(cliente);
        when(clienteRepository.getReferenceById(CLIENTE_ID)).thenReturn(cliente);
        when(repository.save(entityCaptor.capture())).thenReturn(salvo);

        Veiculo resultado = adapter.salvar(veiculo);

        VeiculoEntity enviado = entityCaptor.getValue();
        assertThat(enviado.getId()).isNull();
        assertThat(enviado.getPlaca()).isEqualTo(PLACA);
        assertThat(enviado.getCliente()).isSameAs(cliente);
        assertThat(enviado.isAtivo()).isTrue();
        assertThat(resultado.getId()).isEqualTo(ID);
        assertThat(resultado.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(resultado.getCriadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("Deve buscar veículo ativo por placa usando o valor normalizado")
    void deveBuscarPorPlaca() {
        when(repository.findByPlacaAndAtivoTrue(PLACA)).thenReturn(Optional.of(veiculoEntity(clienteEntity())));

        Optional<Veiculo> resultado = adapter.buscarAtivoPorPlaca(new Placa("bra-2e19"));

        assertThat(resultado).map(Veiculo::getPlaca).contains(new Placa(PLACA));
    }

    @Test
    @DisplayName("Deve listar todos os veículos ativos quando o cliente não for informado")
    void deveListarTodosAtivos() {
        when(repository.findAllByAtivoTrue(PRIMEIRA_PAGINA))
                .thenReturn(new PageImpl<>(List.of(veiculoEntity(clienteEntity())), PRIMEIRA_PAGINA, 1));

        Pagina<Veiculo> pagina = adapter.listarAtivos(null, 0, 20);

        assertThat(pagina.itens()).extracting(Veiculo::getId).containsExactly(ID);
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve filtrar os veículos ativos pelo cliente quando informado")
    void deveListarAtivosDoCliente() {
        when(repository.findAllByClienteIdAndAtivoTrue(CLIENTE_ID, PRIMEIRA_PAGINA))
                .thenReturn(new PageImpl<>(List.of(), PRIMEIRA_PAGINA, 0));

        Pagina<Veiculo> pagina = adapter.listarAtivos(CLIENTE_ID, 0, 20);

        assertThat(pagina.itens()).isEmpty();
        verify(repository).findAllByClienteIdAndAtivoTrue(CLIENTE_ID, PRIMEIRA_PAGINA);
    }

    @Test
    @DisplayName("Deve verificar a existência da placa pelo valor normalizado")
    void deveVerificarExistenciaDaPlaca() {
        when(repository.existsByPlaca(PLACA)).thenReturn(true);

        assertThat(adapter.existePorPlaca(new Placa(PLACA))).isTrue();
    }

    private static ClienteEntity clienteEntity() {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setId(CLIENTE_ID);
        return cliente;
    }

    private static VeiculoEntity veiculoEntity(ClienteEntity cliente) {
        VeiculoEntity entity = new VeiculoEntity();
        entity.setId(ID);
        entity.setPlaca(PLACA);
        entity.setMarca(MARCA);
        entity.setModelo(MODELO);
        entity.setAno(ANO);
        entity.setCliente(cliente);
        entity.setAtivo(true);
        entity.setCreatedAt(CRIADO_EM);
        entity.setUpdatedAt(CRIADO_EM);
        return entity;
    }
}
