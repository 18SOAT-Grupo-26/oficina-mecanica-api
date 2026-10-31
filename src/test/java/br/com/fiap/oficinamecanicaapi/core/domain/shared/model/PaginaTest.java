package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    @Test
    @DisplayName("Deve converter os itens mantendo página, tamanho e total")
    void deveConverterItens() {
        Pagina<Integer> pagina = new Pagina<>(List.of(1, 2, 3), 2, 3, 9);

        Pagina<String> convertida = pagina.map(numero -> "item-" + numero);

        assertThat(convertida.itens()).containsExactly("item-1", "item-2", "item-3");
        assertThat(convertida.pagina()).isEqualTo(2);
        assertThat(convertida.tamanho()).isEqualTo(3);
        assertThat(convertida.total()).isEqualTo(9);
    }

    @Test
    @DisplayName("Busca por chave única deve trazer o item só na primeira página, com total 1")
    void deveMontarPaginaDeNoMaximoUm() {
        assertThat(Pagina.deNoMaximoUm(Optional.of("a"), 1, 20)).isEqualTo(new Pagina<>(List.of("a"), 1, 20, 1));
        assertThat(Pagina.deNoMaximoUm(Optional.of("a"), 2, 20)).isEqualTo(new Pagina<>(List.of(), 2, 20, 1));
        assertThat(Pagina.deNoMaximoUm(Optional.empty(), 1, 20)).isEqualTo(new Pagina<>(List.of(), 1, 20, 0));
    }

    @ParameterizedTest(name = "página {0} → {1}")
    @CsvSource(value = {"NULL, 1", "-1, 1", "0, 1", "1, 1", "4, 4"}, nullValues = "NULL")
    @DisplayName("Página ausente, zero ou negativa deve virar a primeira (1)")
    void deveValidarPagina(Integer informada, int esperada) {
        assertThat(Pagina.paginaValida(informada)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "tamanho {0} → {1}")
    @CsvSource(value = {"NULL, 20", "0, 20", "-5, 20", "1, 1", "50, 50", "100, 100", "101, 100", "1000, 100"},
            nullValues = "NULL")
    @DisplayName("Tamanho ausente ou menor que 1 deve usar o padrão; acima do máximo deve ser limitado")
    void deveValidarTamanho(Integer informado, int esperado) {
        assertThat(Pagina.tamanhoValido(informado)).isEqualTo(esperado);
    }
}
