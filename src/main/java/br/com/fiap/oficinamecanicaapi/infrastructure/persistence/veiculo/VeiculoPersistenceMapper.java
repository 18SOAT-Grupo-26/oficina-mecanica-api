package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VeiculoPersistenceMapper {

    /** O {@code cliente} é uma referência JPA resolvida pelo adapter a partir do {@code clienteId}. */
    @Mapping(target = "placa", source = "placa.valor")
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    VeiculoEntity toEntity(Veiculo veiculo);

    /** O agregado não tem setters; a reconstrução passa pela fábrica do domínio. */
    default Veiculo toDomain(VeiculoEntity entity) {
        return Veiculo.reconstituir(
                entity.getId(),
                new Placa(entity.getPlaca()),
                entity.getMarca(),
                entity.getModelo(),
                entity.getAno(),
                entity.getCliente().getId(),
                entity.isAtivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
