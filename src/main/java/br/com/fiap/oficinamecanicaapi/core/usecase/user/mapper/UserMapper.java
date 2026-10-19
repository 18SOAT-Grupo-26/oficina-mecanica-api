package br.com.fiap.oficinamecanicaapi.core.usecase.user.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.entity.RoleEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.entity.UserEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.Role;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.User;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.UserDetails;
import br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper.IdentityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = {IdentityMapper.class},
        imports = {OffsetDateTime.class, Role.class, Set.class, Collectors.class}
)
public interface UserMapper {

    // User não tem permissions — mapeamento apenas de role
    @Mapping(target = "role",
            expression = "java(Role.valueOf(entity.getRole().getName()))")
    User toResponse(UserEntity entity);

    // UserDetails tem permissions — mapeamento aqui
    @Mapping(target = "identity", source = "identity")
    @Mapping(target = "user", expression = "java(toResponse(entity))")
    @Mapping(target = "permissions",
            expression = "java(entity.getRole().getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet()))")
    UserDetails toUserDetails(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identity", source = "identityEntity")
    @Mapping(target = "role", source = "roleEntity")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "active", constant = "true")
    UserEntity persistUser(
            User userRequest,
            IdentityEntity identityEntity,
            RoleEntity roleEntity
    );
}