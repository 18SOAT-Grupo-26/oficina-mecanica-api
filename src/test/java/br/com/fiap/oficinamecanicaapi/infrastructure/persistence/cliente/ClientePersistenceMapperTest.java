package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Endereco;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClientePersistenceMapperTest {

    private static final Long ID = 7L;
    private static final String NOME = "Oficina Parceira Ltda";
    private static final String CNPJ = "12ABC34501DE35";
    private static final String EMAIL = "contato@parceira.com";
    private static final String TELEFONE = "1133334444";
    private static final Endereco ENDERECO = new Endereco("Av. B", "200", "Bela Vista", "São Paulo", "SP",
            "01310000", "Sala 3");
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-01T09:00:00-03:00");
    private static final OffsetDateTime ATUALIZADO_EM = OffsetDateTime.parse("2026-10-20T18:30:00-03:00");

    private final ClientePersistenceMapper mapper = Mappers.getMapper(ClientePersistenceMapper.class);

    @Test
    @DisplayName("Deve converter o agregado Cliente em entidade JPA com documento, tipo e endereço")
    void deveConverterParaEntidade() {
        Cliente cliente = Cliente.reconstituir(ID, NOME, new Documento(CNPJ), EMAIL, TELEFONE, ENDERECO, true,
                CRIADO_EM, ATUALIZADO_EM);

        ClienteEntity entity = mapper.toEntity(cliente);

        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getDocumento()).isEqualTo(CNPJ);
        assertThat(entity.getTipoDocumento()).isEqualTo(TipoDocumento.CNPJ);
        assertThat(entity.getEndereco().getLogradouro()).isEqualTo(ENDERECO.logradouro());
        assertThat(entity.getEndereco().getComplemento()).isEqualTo(ENDERECO.complemento());
        assertThat(entity.isAtivo()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(CRIADO_EM);
        assertThat(entity.getUpdatedAt()).isEqualTo(ATUALIZADO_EM);
    }

    @Test
    @DisplayName("Deve reconstruir o agregado Cliente a partir da entidade JPA sem perder dados")
    void deveConverterParaDominio() {
        Cliente original = Cliente.reconstituir(ID, NOME, new Documento(CNPJ), EMAIL, TELEFONE, ENDERECO, false,
                CRIADO_EM, ATUALIZADO_EM);

        Cliente reconstruido = mapper.toDomain(mapper.toEntity(original));

        assertThat(reconstruido).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    @DisplayName("Deve manter o endereço nulo quando o cliente não tiver endereço")
    void deveManterEnderecoNulo() {
        Cliente cliente = Cliente.novo(NOME, new Documento(CNPJ), EMAIL, TELEFONE, null);

        Cliente reconstruido = mapper.toDomain(mapper.toEntity(cliente));

        assertThat(reconstruido.getEndereco()).isNull();
    }
}
