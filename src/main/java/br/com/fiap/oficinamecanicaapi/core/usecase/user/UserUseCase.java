package br.com.fiap.oficinamecanicaapi.core.usecase.user;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.User;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.UserDetails;
import br.com.fiap.oficinamecanicaapi.core.port.in.user.UserUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.user.mapper.UserMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.user.RoleRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.user.UserRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.PasswordPolicy;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.UserPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserUseCase implements UserUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(UserUseCase.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserPayload userPayload;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public UserUseCase(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       UserPayload userPayload,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.userPayload = userPayload;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    public void registerUser(User userRequest, IdentityEntity identityEntity) {
        try {
            validatePayloads(userRequest);
            passwordPolicy.validate(userRequest.password());

            var roleEntity = roleRepository.findByName(userRequest.role().name())
                    .orElseThrow(() -> new ApplicationException(400,
                            "Role not found: " + userRequest.role().name()));

            var encodedUser = new User(
                    userRequest.username(),
                    passwordEncoder.encode(userRequest.password()),
                    userRequest.changeRequired(),
                    userRequest.role()
            );

            userRepository.save(userMapper.persistUser(encodedUser, identityEntity, roleEntity));
            log.info("action=user_register_success username={}", userRequest.username());
        } catch (RuntimeException e) {
            log.error("action=user_register_failure username={} error={}",
                    userRequest.username(), e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDetails retrieveUser(SigninRequest request) {
        var identifier = request.identifier();
        var identifierType = userPayload.isEmail(identifier) ? "email" : "username";
        try {
            var entity = userPayload.isEmail(identifier)
                    ? userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new ApplicationException(404, "User not found"))
                    : userRepository.findByUsername(identifier)
                    .orElseThrow(() -> new ApplicationException(404, "User not found"));
            log.info("action=user_retrieve_success identifier_type={}", identifierType);
            return userMapper.toUserDetails(entity);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                log.warn("action=user_retrieve_not_found identifier_type={}", identifierType);
            }
            throw new ApplicationException(404, "User not found");
        }
    }

    private void validatePayloads(User userRequest) {
        log.info("Validating user payload: {}", userRequest.username());
        userPayload.validateUserPayload(userRequest);
    }
}