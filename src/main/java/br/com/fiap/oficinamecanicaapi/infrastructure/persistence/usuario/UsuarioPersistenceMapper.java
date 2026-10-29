package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        imports = {GetLocalTime.class, Perfil.class, Set.class, Collectors.class}
)
public interface UsuarioPersistenceMapper {

    // Usuario não tem permissões — mapeamento apenas do perfil
    @Mapping(target = "perfil",
            expression = "java(Perfil.valueOf(entity.getPerfil().getNome()))")
    Usuario toDomain(UsuarioEntity entity);

    // DetalhesUsuario tem permissões — mapeamento aqui
    @Mapping(target = "usuario", expression = "java(toDomain(entity))")
    @Mapping(target = "permissoes",
            expression = "java(entity.getPerfil().getPermissoes().stream().map(p -> p.getNome()).collect(Collectors.toSet()))")
    DetalhesUsuario toDetalhesUsuario(UsuarioEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nome", source = "usuario.nome")
    @Mapping(target = "perfil", source = "perfilEntity")
    @Mapping(target = "createdAt", expression = "java(GetLocalTime.getLocalTime())")
    @Mapping(target = "updatedAt", expression = "java(GetLocalTime.getLocalTime())")
    @Mapping(target = "ativo", constant = "true")
    UsuarioEntity toEntityCadastro(
            Usuario usuario,
            PerfilEntity perfilEntity
    );
}
