package br.com.fiap.oficinamecanicaapi.core.usecase.usuario.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PerfilEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        imports = {OffsetDateTime.class, Perfil.class, Set.class, Collectors.class}
)
public interface UsuarioMapper {

    // Usuario não tem permissões — mapeamento apenas do perfil
    @Mapping(target = "perfil",
            expression = "java(Perfil.valueOf(entity.getPerfil().getNome()))")
    Usuario toResponse(UsuarioEntity entity);

    // DetalhesUsuario tem permissões — mapeamento aqui
    @Mapping(target = "usuario", expression = "java(toResponse(entity))")
    @Mapping(target = "permissoes",
            expression = "java(entity.getPerfil().getPermissoes().stream().map(p -> p.getNome()).collect(Collectors.toSet()))")
    DetalhesUsuario toDetalhesUsuario(UsuarioEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nome", source = "usuario.nome")
    @Mapping(target = "perfil", source = "perfilEntity")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "ativo", constant = "true")
    UsuarioEntity toEntityCadastro(
            Usuario usuario,
            PerfilEntity perfilEntity
    );
}
