package br.com.fiap.vesta.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void handleNotFound_returns404ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RECURSO_NAO_ENCONTRADO"))
            .andExpect(jsonPath("$.errorId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void handleBusinessRule_returns422ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("REGRA_DE_NEGOCIO_VIOLADA"))
            .andExpect(jsonPath("$.errorId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleConflict_returns409ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("CONFLITO_DE_DADOS"))
            .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void handleUnauthorized_returns403ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACESSO_NEGADO"))
            .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void handleAccessDenied_returns403ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACESSO_NEGADO"))
            .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void handleBadCredentials_returns401ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTENTICACAO_FALHOU"))
            .andExpect(jsonPath("$.errorId").isNotEmpty())
            .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
    }

    @Test
    void handleValidation_returns400ComCamposErros() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CAMPOS_INVALIDOS"))
            .andExpect(jsonPath("$.errors.nome").isNotEmpty())
            .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void handleDataAccess_ora02391_returns503ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/db-limit"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("LIMITE_CONEXOES_DB"))
            .andExpect(jsonPath("$.errorId").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Serviço temporariamente indisponível"));
    }

    @Test
    void handleDataAccess_outroErroDb_returns500ComErrorCode() throws Exception {
        mockMvc.perform(get("/test/db-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("ERRO_BANCO_DE_DADOS"))
            .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void handleUnexpected_returns500ComErrorIdNaoNulo() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("ERRO_INTERNO"))
            .andExpect(jsonPath("$.errorId").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Erro interno"))
            .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente em instantes."));
    }

    // ── Controlador auxiliar de teste ──────────────────────────────────────

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Recurso X", 99L);
        }

        @GetMapping("/test/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Abrigo atingiu capacidade máxima");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Registro duplicado");
        }

        @GetMapping("/test/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Sem permissão para este abrigo");
        }

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("Permissão insuficiente");
        }

        @GetMapping("/test/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("Senha inválida");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody TestRequest body) { }

        @GetMapping("/test/db-limit")
        void dbLimit() {
            throw new JpaSystemException(new RuntimeException(
                new SQLException("ORA-02391: exceeded simultaneous SESSIONS_PER_USER limit", "99999", 2391)));
        }

        @GetMapping("/test/db-error")
        void dbError() {
            throw new JpaSystemException(new RuntimeException(
                new SQLException("ORA-00942: table or view does not exist", "42000", 942)));
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new RuntimeException("Erro inesperado de teste");
        }

        record TestRequest(@NotBlank String nome) { }
    }
}
