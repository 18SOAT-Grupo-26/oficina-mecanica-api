package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientePersistenceMapper {

    @Mapping(target = "documento", source = "documento.valor")
    @Mapping(target = "tipoDocumento", expression = "java(cliente.getDocumento().tipo())")
    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    ClienteEntity toEntity(Cliente cliente);

    Endereco toEndereco(EnderecoEmbeddable endereco);

    /**
     * O agregado não tem setters; a reconstrução passa pela fábrica do domínio.
     */
    default Cliente toDomain(ClienteEntity entity) {
        return Cliente.reconstituir(
                entity.getId(),
                entity.getNome(),
                new Documento(entity.getDocumento()),
                entity.getEmail(),
                entity.getTelefone(),
                toEndereco(entity.getEndereco()),
                entity.isAtivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
