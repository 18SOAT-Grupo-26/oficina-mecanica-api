package br.com.fiap.oficinamecanicaapi.shared.handler;

import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DominioException;
import br.com.fiap.oficinamecanicaapi.shared.exception.LimiteExcedidoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.NaoAutenticadoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import io.micrometer.core.instrument.Metrics;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Toda resposta de erro passa por aqui e é logada uma única vez, com o mesmo traceId do corpo:
 * 4xx em WARN (sem stack trace), 5xx em ERROR (com stack trace).
 */
@RestControllerAdvice
public class HandlerException {

    private static final Logger log = LoggerFactory.getLogger(HandlerException.class);

    /**
     * Erros do domínio: o status sai do tipo da exceção, e não de um número dentro dela. O switch é exaustivo
     * porque {@link DominioException} é selada.
     */
    @ExceptionHandler(DominioException.class)
    public ResponseEntity<ErrorTemplate> handleDominio(DominioException e) {
        HttpStatus status = switch (e) {
            case DadoInvalidoException _ -> HttpStatus.BAD_REQUEST;
            case NaoAutenticadoException _ -> HttpStatus.UNAUTHORIZED;
            case RecursoNaoEncontradoException _ -> HttpStatus.NOT_FOUND;
            case ConflitoException _ -> HttpStatus.CONFLICT;
            case LimiteExcedidoException _ -> HttpStatus.TOO_MANY_REQUESTS;
        };

        String traceId = UUID.randomUUID().toString();
        contarErro(status, "negocio");
        if (log.isWarnEnabled()) {
            log.warn("action=erro_negocio status={} rota={} traceId={} mensagem={}",
                    status.value(), rota(), traceId, e.getMessage());
        }

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(status.value())
                .message(e.getMessage())
                .timestamp(GetLocalTime.getLocalTime())
                .traceId(traceId)
                .build();
        return ResponseEntity.status(status).body(errorBody);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorTemplate> handleUnreadableBody(HttpMessageNotReadableException e) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY,
                "Requisição inválida: verifique o formato e o tipo dos campos");
    }

    /**
     * Bean Validation (@Valid) — informa só o nome do campo, nunca o valor recebido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorTemplate> handleInvalidArgument(MethodArgumentNotValidException e) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "Campos inválidos: " + campos);
    }

    /**
     * Header obrigatório ausente (ex.: Authorization no /auth/validate); sem este handler cairia no 500.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorTemplate> handleMissingHeader(MissingRequestHeaderException e) {
        return buildError(HttpStatus.BAD_REQUEST, "Header obrigatório ausente: " + e.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorTemplate> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName());
    }

    /**
     * Rota inexistente (ex.: DELETE /clientes/ sem id); sem este handler o genérico devolveria 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorTemplate> handleRotaInexistente(NoResourceFoundException e) {
        return buildError(HttpStatus.NOT_FOUND, "Recurso não encontrado");
    }

    /**
     * Rota existe, mas não com este método (ex.: PATCH /clientes/{id}); sem este handler seria 500.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorTemplate> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " não suportado nesta rota");
    }

    /**
     * Query string malformada (ex.: parâmetro sem nome, ?=x): o Tomcat recusa ao ler os parâmetros; sem este handler seria 500.
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorTemplate> handleParametroInvalido(InvalidParameterException e) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetros da requisição inválidos");
    }

    /**
     * @PreAuthorize negado; sem este handler o genérico abaixo devolveria 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorTemplate> handleAccessDenied(AccessDeniedException e) {
        Metrics.counter("oficina.seguranca.acesso.negado", "status", "403").increment();
        return buildError(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    /**
     * Banco fora do ar ou lento demais: 503 explícito, sem detalhes da falha (RNF04, ADR 003).
     */
    @ExceptionHandler({DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class,
            QueryTimeoutException.class})
    public ResponseEntity<ErrorTemplate> handleDatabaseUnavailable(Exception e) {
        Metrics.counter("oficina.banco.indisponivel").increment();
        return buildError(HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço temporariamente indisponível. Tente novamente.", e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorTemplate> handleGenericException(Exception e) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", e);
    }

    private static ResponseEntity<ErrorTemplate> buildError(HttpStatus status, String message) {
        return buildError(status, message, null);
    }

    /**
     * Com causa: ERROR com stack trace (5xx). Sem causa: WARN (4xx). O traceId do log é o mesmo do corpo.
     */
    private static ResponseEntity<ErrorTemplate> buildError(HttpStatus status, String message, Exception causa) {
        String traceId = UUID.randomUUID().toString();
        if (causa != null) {
            contarErro(status, "servidor");
            if (log.isErrorEnabled()) {
                log.error("action=erro_servidor status={} rota={} traceId={} mensagem={}",
                        status.value(), rota(), traceId, message, causa);
            }
        } else {
            contarErro(status, "requisicao");
            if (log.isWarnEnabled()) {
                log.warn("action=erro_requisicao status={} rota={} traceId={} mensagem={}",
                        status.value(), rota(), traceId, message);
            }
        }

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(status.value())
                .message(message)
                .timestamp(GetLocalTime.getLocalTime())
                .traceId(traceId)
                .build();
        return ResponseEntity.status(status).body(errorBody);
    }

    /**
     * "GET /api/v1/clientes/9"; "-" fora de uma requisição (ex.: teste unitário).
     */
    private static String rota() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getMethod() + " " + atributos.getRequest().getRequestURI();
        }
        return "-";
    }

    /**
     * oficina.erros{status, tipo}: negocio/requisicao = recusas esperadas (4xx); servidor = falhas (5xx).
     */
    private static void contarErro(HttpStatus status, String tipo) {
        Metrics.counter("oficina.erros", "status", String.valueOf(status.value()), "tipo", tipo).increment();
    }
}