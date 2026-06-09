package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.exception.UnauthorizedException;
import br.com.fiap.vesta.repository.UsuarioRepository;
import br.com.fiap.vesta.domain.entity.Regiao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsolamentoServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks IsolamentoService isolamentoService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private Usuario operadorComAbrigo(Long idAbrigo) {
        Abrigo abrigo = new Abrigo();
        abrigo.setIdAbrigo(idAbrigo);
        Usuario u = new Usuario();
        u.setAbrigo(abrigo);
        return u;
    }

    private Usuario gestorComRegiao(Long idRegiao) {
        Regiao regiao = new Regiao();
        regiao.setIdRegiao(idRegiao);
        Usuario u = new Usuario();
        u.setRegiao(regiao);
        return u;
    }

    @Test
    void operador_acessaSeuAbrigo_naoLanca() {
        autenticarComo("op@vesta.com", "ROLE_OPERADOR");
        when(usuarioRepository.findByDsEmail("op@vesta.com"))
            .thenReturn(Optional.of(operadorComAbrigo(1L)));

        assertThatNoException().isThrownBy(() -> isolamentoService.verificarAcessoAbrigo(1L));
    }

    @Test
    void operador_acessaAbrigoErrado_lancaUnauthorized() {
        autenticarComo("op@vesta.com", "ROLE_OPERADOR");
        when(usuarioRepository.findByDsEmail("op@vesta.com"))
            .thenReturn(Optional.of(operadorComAbrigo(1L)));

        assertThatThrownBy(() -> isolamentoService.verificarAcessoAbrigo(2L))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void operador_semAbrigoVinculado_lancaUnauthorized() {
        autenticarComo("op@vesta.com", "ROLE_OPERADOR");
        Usuario u = new Usuario(); // abrigo = null
        when(usuarioRepository.findByDsEmail("op@vesta.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> isolamentoService.verificarAcessoAbrigo(1L))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void gestor_acessaQualquerAbrigo_naoLanca() {
        autenticarComo("gestor@vesta.com", "ROLE_GESTOR");

        assertThatNoException().isThrownBy(() -> isolamentoService.verificarAcessoAbrigo(99L));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void admin_acessaQualquerAbrigo_naoLanca() {
        autenticarComo("admin@vesta.com", "ROLE_ADMIN");

        assertThatNoException().isThrownBy(() -> isolamentoService.verificarAcessoAbrigo(99L));
        verifyNoInteractions(usuarioRepository);
    }

    // ── verificarAcessoRegiao ─────────────────────────────────────────────────

    @Test
    void gestor_acessaSuaRegiao_naoLanca() {
        autenticarComo("gestor@vesta.com", "ROLE_GESTOR");
        when(usuarioRepository.findByDsEmail("gestor@vesta.com"))
            .thenReturn(Optional.of(gestorComRegiao(1L)));

        assertThatNoException().isThrownBy(() -> isolamentoService.verificarAcessoRegiao(1L));
    }

    @Test
    void gestor_acessaRegiaoErrada_lancaUnauthorized() {
        autenticarComo("gestor@vesta.com", "ROLE_GESTOR");
        when(usuarioRepository.findByDsEmail("gestor@vesta.com"))
            .thenReturn(Optional.of(gestorComRegiao(1L)));

        assertThatThrownBy(() -> isolamentoService.verificarAcessoRegiao(2L))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void gestor_semRegiao_lancaUnauthorized() {
        autenticarComo("gestor@vesta.com", "ROLE_GESTOR");
        Usuario u = new Usuario(); // regiao = null
        when(usuarioRepository.findByDsEmail("gestor@vesta.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> isolamentoService.verificarAcessoRegiao(1L))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void admin_acessaQualquerRegiao_naoLanca() {
        autenticarComo("admin@vesta.com", "ROLE_ADMIN");

        assertThatNoException().isThrownBy(() -> isolamentoService.verificarAcessoRegiao(99L));
        verifyNoInteractions(usuarioRepository);
    }
}
