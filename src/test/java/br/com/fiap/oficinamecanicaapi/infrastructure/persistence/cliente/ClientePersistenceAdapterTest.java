package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientePersistenceAdapterTest {

    private static final Documento CPF = new Documento("52998224725");

    @Mock
    private ClienteJpaRepository repository;

    @Mock
    private ClientePersistenceMapper mapper;

    @InjectMocks
    private ClientePersistenceAdapter adapter;

    @Mock
    private Cliente cliente;

    @Mock
    private ClienteEntity entity;

    @Test
    @DisplayName("salvar deve converter, persistir e devolver o domínio salvo")
    void salvar() {
        var salvo = mock(ClienteEntity.class);
        var clienteSalvo = mock(Cliente.class);
        when(mapper.toEntity(cliente)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(salvo);
        when(mapper.toDomain(salvo)).thenReturn(clienteSalvo);

        assertThat(adapter.salvar(cliente)).isSameAs(clienteSalvo);
    }

    @Test
    @DisplayName("buscarAtivoPorId deve mapear quando existe e devolver vazio quando não existe")
    void buscarAtivoPorId() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(entity));
        when(repository.findByIdAndAtivoTrue(2L)).thenReturn(Optional.empty());
        when(mapper.toDomain(entity)).thenReturn(cliente);

        assertThat(adapter.buscarAtivoPorId(1L)).containsSame(cliente);
        assertThat(adapter.buscarAtivoPorId(2L)).isEmpty();
    }

    @Test
    @DisplayName("buscarAtivoPorDocumento deve consultar pelo valor só com dígitos")
    void buscarAtivoPorDocumento() {
        when(repository.findByDocumentoAndAtivoTrue("52998224725")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cliente);

        assertThat(adapter.buscarAtivoPorDocumento(CPF)).containsSame(cliente);
    }

    @Test
    @DisplayName("existePorDocumento deve repassar o resultado do repositório")
    void existePorDocumento() {
        when(repository.existsByDocumento("52998224725")).thenReturn(true);

        assertThat(adapter.existePorDocumento(CPF)).isTrue();
    }

    @Test
    @DisplayName("listarAtivos deve paginar ordenando por id e devolver o total")
    void listarAtivos() {
        Pageable esperado = PageRequest.of(1, 2, Sort.by("id"));
        when(repository.findAllByAtivoTrue(esperado)).thenReturn(new PageImpl<>(List.of(entity), esperado, 3));
        when(mapper.toDomain(entity)).thenReturn(cliente);

        var pagina = adapter.listarAtivos(1, 2);

        assertThat(pagina.itens()).containsExactly(cliente);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamanho()).isEqualTo(2);
        assertThat(pagina.total()).isEqualTo(3);
        verify(repository).findAllByAtivoTrue(esperado);
    }
}
