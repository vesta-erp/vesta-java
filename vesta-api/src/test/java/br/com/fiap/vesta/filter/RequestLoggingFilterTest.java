package br.com.fiap.vesta.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger filterLogger;

    @BeforeEach
    void setup() {
        filter = new RequestLoggingFilter();
        filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        filterLogger.addAppender(listAppender);
    }

    @AfterEach
    void teardown() {
        filterLogger.detachAppender(listAppender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveExecutarChainEEmitirLogRequest() throws IOException, ServletException {
        AtomicBoolean chainExecutada = new AtomicBoolean(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/abrigos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> chainExecutada.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainExecutada).isTrue();
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getMessage()).isEqualTo("REQUEST");
    }

    @Test
    void deveLerUsuarioAposExecucaoDaChain() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/abrigos/1/acolhimento");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            // simula JwtAuthenticationFilter populando SecurityContext durante a chain
            var auth = new UsernamePasswordAuthenticationToken("operador@vesta.com", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        };

        filter.doFilter(request, response, chain);

        ILoggingEvent evento = listAppender.list.get(0);
        String argumentos = Arrays.toString(evento.getArgumentArray());
        assertThat(argumentos).contains("operador@vesta.com");
    }

    @Test
    void deveLogarAnonimoCasoChainNaoAutentique() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { /* não popula SecurityContext */ };

        filter.doFilter(request, response, chain);

        String argumentos = Arrays.toString(listAppender.list.get(0).getArgumentArray());
        assertThat(argumentos).contains("anonimo");
    }

    @Test
    void deveCapturarStatusAposExecucaoDaChain() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("ok"); // status 200 padrão

        filter.doFilter(request, response, chain);

        String argumentos = Arrays.toString(listAppender.list.get(0).getArgumentArray());
        assertThat(argumentos).contains("200");
    }

    @Test
    void naoDeveInterceptarEndpointsDoActuator() {
        MockHttpServletRequest actuator = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletRequest swagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletRequest apiDocs = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/abrigos");

        assertThat(filter.shouldNotFilter(actuator)).isTrue();
        assertThat(filter.shouldNotFilter(swagger)).isTrue();
        assertThat(filter.shouldNotFilter(apiDocs)).isTrue();
        assertThat(filter.shouldNotFilter(api)).isFalse();
    }

    @Test
    void deveLogarMesmoQuandoChainLancaExcecao() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { throw new ServletException("erro simulado"); };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
            .isInstanceOf(ServletException.class);

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getMessage()).isEqualTo("REQUEST");
    }
}
