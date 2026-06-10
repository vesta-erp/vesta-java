package br.com.fiap.vesta.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Recurso não encontrado: {} [{}]", ex.getMessage(), req.getRequestURI());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado",
            ex.getMessage(), VestaErrorCode.RECURSO_NAO_ENCONTRADO, req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        log.warn("Regra de negócio violada: {} [{}]", ex.getMessage(), req.getRequestURI());
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada",
            ex.getMessage(), VestaErrorCode.REGRA_DE_NEGOCIO_VIOLADA, req);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
        log.warn("Conflito de dados: {} [{}]", ex.getMessage(), req.getRequestURI());
        return buildProblemDetail(HttpStatus.CONFLICT, "Conflito de dados",
            ex.getMessage(), VestaErrorCode.CONFLITO_DE_DADOS, req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        log.warn("Acesso negado: {} [{}]", ex.getMessage(), req.getRequestURI());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Acesso negado",
            ex.getMessage(), VestaErrorCode.ACESSO_NEGADO, req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Autenticação falhou [{}]", req.getRequestURI());
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Autenticação falhou",
            "Credenciais inválidas", VestaErrorCode.AUTENTICACAO_FALHOU, req);
    }

    // Spring Security envolve qualquer exceção não esperada de UserDetailsService neste tipo.
    // Sem este handler, falhas de conectividade Oracle durante autenticação caem no catch-all (500 ERRO_INTERNO).
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ProblemDetail handleInternalAuth(InternalAuthenticationServiceException ex, HttpServletRequest req) {
        Throwable cause = ex.getCause();
        if (cause instanceof DataAccessException dae) {
            log.error("Erro de banco durante autenticação [{}]", req.getRequestURI(), dae);
            return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de banco de dados",
                "Erro interno ao acessar o banco de dados.",
                VestaErrorCode.ERRO_BANCO_DE_DADOS, req);
        }
        log.error("Falha inesperada durante autenticação [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Erro interno. Tente novamente em instantes.",
            VestaErrorCode.ERRO_INTERNO, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        log.warn("Campos inválidos: {} [{}]", errors.keySet(), req.getRequestURI());
        ProblemDetail pd = buildProblemDetail(HttpStatus.BAD_REQUEST, "Erro de validação",
            "Campos inválidos", VestaErrorCode.CAMPOS_INVALIDOS, req);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Permissão insuficiente [{}]", req.getRequestURI());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Acesso negado",
            "Permissão insuficiente", VestaErrorCode.ACESSO_NEGADO, req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccess(DataAccessException ex, HttpServletRequest req) {
        SQLException sqlEx = findSQLException(ex);
        if (sqlEx != null && sqlEx.getErrorCode() == 2391) {
            log.error("Limite de conexões Oracle excedido [{}]", req.getRequestURI(), ex);
            return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço temporariamente indisponível",
                "O serviço atingiu o limite de conexões com o banco de dados. Tente novamente em instantes.",
                VestaErrorCode.LIMITE_CONEXOES_DB, req);
        }
        log.error("Erro de acesso ao banco de dados [{}]", req.getRequestURI(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de banco de dados",
            "Erro interno ao acessar o banco de dados.",
            VestaErrorCode.ERRO_BANCO_DE_DADOS, req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Erro interno. Tente novamente em instantes.",
            VestaErrorCode.ERRO_INTERNO, req);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail,
                                              VestaErrorCode errorCode, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errorId", resolveErrorId());
        pd.setProperty("errorCode", errorCode.name());
        return pd;
    }

    private String resolveErrorId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static SQLException findSQLException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sql) return sql;
            t = t.getCause();
        }
        return null;
    }
}
