package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoUseCaseTest {

    private static final Long ID = 1L;

    @Mock
    private ServicoPortOut servicoPortOut;

    @Mock
    private ServicoMapper servicoMapper;

    private ServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ServicoUseCase(servicoPortOut, servicoMapper);
    }

    private static ServicoRequest request(String nome, String descricao, String preco) {
        return new ServicoRequest(nome, descricao, preco == null ? null : new BigDecimal(preco));
    }

    @Test
    @DisplayName("criar persiste serviço com nome normalizado e preço com 2 casas")
    void criarPersisteServico() {
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(false);
        when(servicoPortOut.salvar(any())).thenAnswer(inv -> {
            ServicoEntity e = inv.getArgument(0);
            e.setId(ID);
            return e;
        });
        ServicoResponse esperado = new ServicoResponse(ID, "Troca de Óleo", null, new BigDecimal("80.00"), null, null);
        when(servicoMapper.toResponse(any())).thenReturn(esperado);

        ServicoResponse response = useCase.criar(request("  Troca de Óleo ", null, "80"));

        assertThat(response.id()).isEqualTo(ID);
        ArgumentCaptor<ServicoEntity> captor = ArgumentCaptor.forClass(ServicoEntity.class);
        verify(servicoPortOut).salvar(captor.capture());
        ServicoEntity salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Troca de Óleo");
        assertThat(salvo.getNomeNormalizado()).isEqualTo("troca de oleo");
        assertThat(salvo.getPreco().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("criar rejeita nome com menos de 3 caracteres (422)")
    void criarRejeitaNomeCurto() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("ab", null, "10")));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("O nome deve ter no mínimo 3 caracteres");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita nome duplicado ignorando maiúsculas, acentos e espaços (409)")
    void criarRejeitaNomeDuplicado() {
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("  TROCA   de óleo", null, "10")));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        assertThat(ex.getMessage()).isEqualTo("Já existe um serviço cadastrado com este nome");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço zero ou negativo (422)")
    void criarRejeitaPrecoNaoPositivo() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", null, "0")));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço com mais de duas casas decimais (422)")
    void criarRejeitaPrecoComTresCasas() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", null, "10.123")));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita descrição com mais de 500 caracteres (422)")
    void criarRejeitaDescricaoLonga() {
        String descricao = "a".repeat(501);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.criar(request("Alinhamento", descricao, "10")));

        assertThat(ex.getErrorCode()).isEqualTo(422);
        assertThat(ex.getMessage()).isEqualTo("A descrição deve ter no máximo 500 caracteres");
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
