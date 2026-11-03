package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemOrdemServicoTest {

    private static final ValorMonetario PRECO = ValorMonetario.de("30");

    @Test
    @DisplayName("Deve criar item de serviço novo, sem id, com o preço informado congelado")
    void deveCriarItemServico() {
        ItemServico item = ItemServico.novo(7L, "  Troca de óleo ", PRECO, 1);

        assertThat(item.getId()).isNull();
        assertThat(item.getIdServico()).isEqualTo(7L);
        assertThat(item.getDescricao()).isEqualTo("Troca de óleo");
        assertThat(item.getPrecoUnitario()).isEqualTo(PRECO);
        assertThat(item.getSubtotal()).isEqualTo(PRECO);
    }

    @Test
    @DisplayName("Deve calcular o subtotal do item de peça pela quantidade")
    void deveCalcularSubtotalItemPeca() {
        ItemPeca item = ItemPeca.novo(3L, "Filtro de óleo", PRECO, 2);

        assertThat(item.getIdPeca()).isEqualTo(3L);
        assertThat(item.getQuantidade()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualTo(ValorMonetario.de("60"));
    }

    @Test
    @DisplayName("Deve reconstituir item já persistido com o id")
    void deveReconstituirItem() {
        assertThat(ItemServico.reconstituir(1L, 7L, "Alinhamento", PRECO, 1).getId()).isEqualTo(1L);
        assertThat(ItemPeca.reconstituir(2L, 3L, "Filtro", PRECO, 1).getId()).isEqualTo(2L);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Deve rejeitar quantidade menor ou igual a zero com erro 400")
    void deveRejeitarQuantidadeInvalida(int quantidade) {
        assertErro400(() -> ItemPeca.novo(3L, "Filtro", PRECO, quantidade), "A quantidade do item deve ser maior que zero");
        assertErro400(() -> ItemServico.novo(7L, "Alinhamento", PRECO, quantidade), "A quantidade do item deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar preço nulo com erro 400")
    void deveRejeitarPrecoNulo() {
        assertErro400(() -> ItemServico.novo(7L, "Alinhamento", null, 1), "O preço unitário do item é obrigatório");
    }

    @Test
    @DisplayName("Deve rejeitar descrição em branco com erro 400")
    void deveRejeitarDescricaoEmBranco() {
        assertErro400(() -> ItemServico.novo(7L, " ", PRECO, 1), "A descrição do item é obrigatória");
        assertErro400(() -> ItemPeca.novo(3L, null, PRECO, 1), "A descrição do item é obrigatória");
    }

    @Test
    @DisplayName("Deve rejeitar item sem referência ao catálogo com erro 400")
    void deveRejeitarSemReferenciaAoCatalogo() {
        assertErro400(() -> ItemServico.novo(null, "Alinhamento", PRECO, 1), "O serviço do item é obrigatório");
        assertErro400(() -> ItemPeca.novo(null, "Filtro", PRECO, 1), "A peça do item é obrigatória");
    }

    private static void assertErro400(Executable acao, String mensagem) {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class, acao);
        assertThat(exception.getMessage()).isEqualTo(mensagem);
    }
}
