package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper.ServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoUseCaseTest {

    private static final Long ID = 1L;
    private static final Long INSUMO_ID = 10L;

    @Mock
    private ServicoPortOut servicoPortOut;

    @Mock
    private PecaInsumoPortOut pecaInsumoPortOut;

    @Mock
    private ServicoMapper servicoMapper;

    private ServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ServicoUseCase(servicoPortOut, pecaInsumoPortOut, servicoMapper);
    }

    private static ServicoRequest request(String nome, String descricao, String preco, List<Long> insumos) {
        return new ServicoRequest(nome, descricao, preco == null ? null : new BigDecimal(preco), insumos);
    }

    @Test
    @DisplayName("criar persiste serviço com nome normalizado, preço com 2 casas e insumos vinculados")
    void criarPersisteServicoComInsumos() {
        PecaInsumoEntity insumo = new PecaInsumoEntity();
        insumo.setId(INSUMO_ID);
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(false);
        when(pecaInsumoPortOut.buscarPorIds(anyCollection())).thenReturn(List.of(insumo));
        when(servicoPortOut.salvar(any())).thenAnswer(inv -> {
            ServicoEntity e = inv.getArgument(0);
            e.setId(ID);
            return e;
        });
        ServicoResponse esperado = new ServicoResponse(ID, "Troca de Óleo", null, new BigDecimal("80.00"),
                List.of(), null, null);
        when(servicoMapper.toResponse(any())).thenReturn(esperado);

        ServicoResponse response = useCase.criar(request("  Troca de Óleo ", null, "80", List.of(INSUMO_ID, INSUMO_ID)));

        assertThat(response.id()).isEqualTo(ID);
        ArgumentCaptor<ServicoEntity> captor = ArgumentCaptor.forClass(ServicoEntity.class);
        verify(servicoPortOut).salvar(captor.capture());
        ServicoEntity salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Troca de Óleo");
        assertThat(salvo.getNomeNormalizado()).isEqualTo("troca de oleo");
        assertThat(salvo.getPreco().scale()).isEqualTo(2);
        assertThat(salvo.getPecasInsumo()).containsExactly(insumo);
    }

    @Test
    @DisplayName("criar rejeita nome com menos de 3 caracteres (422)")
    void criarRejeitaNomeCurto() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("ab", null, "10", List.of())));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("O nome deve ter no mínimo 3 caracteres");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita nome duplicado ignorando maiúsculas, acentos e espaços (409)")
    void criarRejeitaNomeDuplicado() {
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("  TROCA   de óleo", null, "10", List.of())));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        assertThat(ex.getMessage()).isEqualTo("Já existe um serviço cadastrado com este nome");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço zero ou negativo (422)")
    void criarRejeitaPrecoNaoPositivo() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", null, "0", List.of())));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço com mais de duas casas decimais (422)")
    void criarRejeitaPrecoComTresCasas() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", null, "10.123", List.of())));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita descrição com mais de 500 caracteres (422)")
    void criarRejeitaDescricaoLonga() {
        String descricao = "a".repeat(501);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", descricao, "10", List.of())));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("A descrição deve ter no máximo 500 caracteres");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita insumo inexistente (422)")
    void criarRejeitaInsumoInexistente() {
        when(servicoPortOut.existePorNomeNormalizado("alinhamento")).thenReturn(false);
        when(pecaInsumoPortOut.buscarPorIds(anyCollection())).thenReturn(List.of());

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", null, "10", List.of(INSUMO_ID))));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("buscarPorId retorna 404 quando serviço não existe")
    void buscarPorIdNaoEncontrado() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.buscarPorId(ID));

        assertThat(ex.getErrorCode()).isEqualTo(404);
    }
}
