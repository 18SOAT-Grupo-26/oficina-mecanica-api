package br.com.fiap.oficinamecanicaapi.infrastructure.config;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

/**
 * Cria o primeiro ADMINISTRATOR na subida, a partir de ADMIN_* do ambiente (.env), quando ainda não existe nenhum.
 * A senha nunca vai para o repositório nem para o log. Passa pelas mesmas regras do cadastro (política de senha,
 * e-mail e unicidade); se forem recusadas, a subida falha para o problema não passar despercebido.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioUseCasePortIn usuarioUseCase;
    private final String nome;
    private final String email;
    private final String usuario;
    private final String senha;

    public AdministradorInicial(UsuarioUseCasePortIn usuarioUseCase,
                                @Value("${api.admin.nome:Administrador}") String nome,
                                @Value("${api.admin.email:}") String email,
                                @Value("${api.admin.usuario:}") String usuario,
                                @Value("${api.admin.senha:}") String senha) {
        this.usuarioUseCase = usuarioUseCase;
        this.nome = nome;
        this.email = email;
        this.usuario = usuario;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioUseCase.existeAdministrador()) {
            log.info("action=admin_inicial_ignorado motivo=administrador_existente");
            return;
        }
        if (Stream.of(email, usuario, senha).anyMatch(valor -> valor == null || valor.isBlank())) {
            log.warn("action=admin_inicial_ausente: nenhum ADMINISTRATOR cadastrado e ADMIN_EMAIL, ADMIN_USUARIO "
                    + "ou ADMIN_SENHA não definidos; defina-os no .env para poder cadastrar usuários");
            return;
        }
        try {
            usuarioUseCase.cadastrar(new Usuario(nome, email, usuario, senha, false, Perfil.ADMINISTRATOR));
            log.info("action=admin_inicial_criado");
        } catch (ApplicationException | DataIntegrityViolationException e) {
            // outra instância subindo ao mesmo tempo pode ter criado o administrador primeiro (409 ou constraint)
            if (usuarioUseCase.existeAdministrador()) {
                log.info("action=admin_inicial_ignorado motivo=criado_por_outra_instancia");
                return;
            }
            throw new IllegalStateException("Administrador inicial (ADMIN_*) recusado: " + e.getMessage(), e);
        }
    }
}
