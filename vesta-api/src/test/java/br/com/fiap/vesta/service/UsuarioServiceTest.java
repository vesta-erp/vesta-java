package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.NomePerfil;
import br.com.fiap.vesta.dto.request.UsuarioRequest;
import br.com.fiap.vesta.exception.*;
import br.com.fiap.vesta.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PerfilAcessoRepository perfilRepository;
    @Mock AbrigoRepository abrigoRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UsuarioService usuarioService;

    private UserDetails adminUser() {
        return new User("admin@vesta.com", "hash",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UserDetails gestorUser() {
        return new User("gestor@vesta.com", "hash",
            List.of(new SimpleGrantedAuthority("ROLE_GESTOR")));
    }

    private UserDetails operadorUser() {
        return new User("operador@vesta.com", "hash",
            List.of(new SimpleGrantedAuthority("ROLE_OPERADOR")));
    }

    private PerfilAcesso perfil(NomePerfil nome) {
        PerfilAcesso p = new PerfilAcesso();
        p.setNmPerfil(nome);
        return p;
    }

    @Test
    void criar_valido_hasheaSenhaESalva() {
        var request = new UsuarioRequest("João", null, null, "joao@vesta.com", "senha123", "OPERADOR", 1L);
        when(usuarioRepository.existsByDsEmail("joao@vesta.com")).thenReturn(false);
        when(perfilRepository.findByNmPerfil(NomePerfil.OPERADOR)).thenReturn(Optional.of(perfil(NomePerfil.OPERADOR)));
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(new Abrigo()));
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1L);
            u.setPerfil(perfil(NomePerfil.OPERADOR));
            return u;
        });

        var resp = usuarioService.criar(request, adminUser());

        verify(passwordEncoder).encode("senha123");
        assertThat(resp.dsEmail()).isEqualTo("joao@vesta.com");
        assertThat(resp.nmPerfil()).isEqualTo("OPERADOR");
    }

    @Test
    void criar_emailDuplicado_lancaConflictException() {
        var request = new UsuarioRequest("João", null, null, "joao@vesta.com", "senha123", "ADMIN", null);
        when(usuarioRepository.existsByDsEmail("joao@vesta.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(request, adminUser()))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void criar_operadorSemAbrigo_lancaBusinessRuleException() {
        var request = new UsuarioRequest("João", null, null, "joao@vesta.com", "senha123", "OPERADOR", null);
        when(usuarioRepository.existsByDsEmail("joao@vesta.com")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.criar(request, adminUser()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("abrigo");
    }

    @Test
    void criar_gestorCriandoOperador_ePermitido() {
        var request = new UsuarioRequest("João", null, null, "joao@vesta.com", "senha123", "OPERADOR", 1L);
        when(usuarioRepository.existsByDsEmail("joao@vesta.com")).thenReturn(false);
        when(perfilRepository.findByNmPerfil(NomePerfil.OPERADOR)).thenReturn(Optional.of(perfil(NomePerfil.OPERADOR)));
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(new Abrigo()));
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1L);
            u.setPerfil(perfil(NomePerfil.OPERADOR));
            return u;
        });

        assertThatNoException().isThrownBy(() -> usuarioService.criar(request, gestorUser()));
    }

    @Test
    void criar_gestorCriandoGestor_lancaUnauthorizedException() {
        var request = new UsuarioRequest("João", null, null, "joao@vesta.com", "senha123", "GESTOR", null);

        assertThatThrownBy(() -> usuarioService.criar(request, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void atualizar_semSenha_mantemHashAtual() {
        Usuario existente = new Usuario();
        existente.setIdUsuario(1L);
        existente.setDsEmail("joao@vesta.com");
        existente.setDsSenhaHash("$2a$hash-existente");
        existente.setPerfil(perfil(NomePerfil.OPERADOR));

        var request = new UsuarioRequest("João Atualizado", null, null, "joao@vesta.com", null, "OPERADOR", 1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.findByNmPerfil(NomePerfil.OPERADOR)).thenReturn(Optional.of(perfil(NomePerfil.OPERADOR)));
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(new Abrigo()));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.atualizar(1L, request, adminUser());

        verify(passwordEncoder, never()).encode(any());
        assertThat(existente.getDsSenhaHash()).isEqualTo("$2a$hash-existente");
    }

    // ── T9: buscarPorId — controle de acesso ──────────────────────────────────

    @Test
    void buscarPorId_gestorBuscandoOProprio_retornaDados() {
        Usuario gestor = new Usuario(); gestor.setDsEmail("gestor@vesta.com");
        gestor.setPerfil(perfil(NomePerfil.GESTOR));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gestor));

        assertThatNoException().isThrownBy(() -> usuarioService.buscarPorId(1L, gestorUser()));
    }

    @Test
    void buscarPorId_gestorBuscandoOperador_retornaDados() {
        Usuario operador = new Usuario(); operador.setDsEmail("op@vesta.com");
        operador.setPerfil(perfil(NomePerfil.OPERADOR));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(operador));

        assertThatNoException().isThrownBy(() -> usuarioService.buscarPorId(2L, gestorUser()));
    }

    @Test
    void buscarPorId_gestorBuscandoOutroGestor_lancaUnauthorized() {
        Usuario outro = new Usuario(); outro.setDsEmail("outro@vesta.com");
        outro.setPerfil(perfil(NomePerfil.GESTOR));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> usuarioService.buscarPorId(3L, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void buscarPorId_gestorBuscandoAdmin_lancaUnauthorized() {
        Usuario admin = new Usuario(); admin.setDsEmail("admin2@vesta.com");
        admin.setPerfil(perfil(NomePerfil.ADMIN));
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.buscarPorId(4L, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void buscarPorId_operadorBuscandoOProprio_retornaDados() {
        Usuario operador = new Usuario(); operador.setDsEmail("operador@vesta.com");
        operador.setPerfil(perfil(NomePerfil.OPERADOR));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operador));

        assertThatNoException().isThrownBy(() -> usuarioService.buscarPorId(5L, operadorUser()));
    }

    @Test
    void buscarPorId_operadorBuscandoOutro_lancaUnauthorized() {
        Usuario outro = new Usuario(); outro.setDsEmail("outro@vesta.com");
        outro.setPerfil(perfil(NomePerfil.OPERADOR));
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> usuarioService.buscarPorId(6L, operadorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    // ── T10: atualizar — controle de acesso ───────────────────────────────────

    @Test
    void atualizar_gestorAtualizandoOProprio_ePermitido() {
        Usuario gestor = new Usuario(); gestor.setIdUsuario(1L);
        gestor.setDsEmail("gestor@vesta.com"); gestor.setDsSenhaHash("hash");
        gestor.setPerfil(perfil(NomePerfil.GESTOR));
        var req = new UsuarioRequest("Gestor", null, null, "gestor@vesta.com", null, "GESTOR", null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gestor));
        when(perfilRepository.findByNmPerfil(NomePerfil.GESTOR)).thenReturn(Optional.of(perfil(NomePerfil.GESTOR)));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> usuarioService.atualizar(1L, req, gestorUser()));
    }

    @Test
    void atualizar_gestorAtualizandoOperador_ePermitido() {
        Usuario operador = new Usuario(); operador.setIdUsuario(2L);
        operador.setDsEmail("op@vesta.com"); operador.setDsSenhaHash("hash");
        operador.setPerfil(perfil(NomePerfil.OPERADOR));
        var req = new UsuarioRequest("Op", null, null, "op@vesta.com", null, "OPERADOR", 1L);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(operador));
        when(perfilRepository.findByNmPerfil(NomePerfil.OPERADOR)).thenReturn(Optional.of(perfil(NomePerfil.OPERADOR)));
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(new Abrigo()));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> usuarioService.atualizar(2L, req, gestorUser()));
    }

    @Test
    void atualizar_gestorAtualizandoOutroGestor_lancaUnauthorized() {
        Usuario outro = new Usuario(); outro.setDsEmail("outro@vesta.com");
        outro.setPerfil(perfil(NomePerfil.GESTOR));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(outro));

        var req = new UsuarioRequest("Outro", null, null, "outro@vesta.com", null, "GESTOR", null);
        assertThatThrownBy(() -> usuarioService.atualizar(3L, req, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void atualizar_operadorAtualizandoOProprio_ePermitido() {
        Usuario operador = new Usuario(); operador.setIdUsuario(4L);
        operador.setDsEmail("operador@vesta.com"); operador.setDsSenhaHash("hash");
        operador.setPerfil(perfil(NomePerfil.OPERADOR));
        var req = new UsuarioRequest("Op", null, null, "operador@vesta.com", null, "OPERADOR", 1L);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(operador));
        when(perfilRepository.findByNmPerfil(NomePerfil.OPERADOR)).thenReturn(Optional.of(perfil(NomePerfil.OPERADOR)));
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(new Abrigo()));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> usuarioService.atualizar(4L, req, operadorUser()));
    }

    @Test
    void atualizar_operadorAtualizandoOutro_lancaUnauthorized() {
        Usuario outro = new Usuario(); outro.setDsEmail("outro@vesta.com");
        outro.setPerfil(perfil(NomePerfil.OPERADOR));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(outro));

        var req = new UsuarioRequest("Outro", null, null, "outro@vesta.com", null, "OPERADOR", null);
        assertThatThrownBy(() -> usuarioService.atualizar(5L, req, operadorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    // ── T11: desativar — controle de acesso do GESTOR ─────────────────────────

    @Test
    void desativar_gestorDesativandoOperador_ePermitido() {
        Usuario operador = new Usuario(); operador.setIdUsuario(1L);
        operador.setDsEmail("op@vesta.com");
        operador.setPerfil(perfil(NomePerfil.OPERADOR));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));

        assertThatNoException().isThrownBy(() -> usuarioService.desativar(1L, gestorUser()));
        verify(usuarioRepository).save(argThat(u -> "N".equals(u.getStAtivo())));
    }

    @Test
    void desativar_gestorDesativandoOutroGestor_lancaUnauthorized() {
        Usuario outroGestor = new Usuario(); outroGestor.setDsEmail("outro@vesta.com");
        outroGestor.setPerfil(perfil(NomePerfil.GESTOR));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outroGestor));

        assertThatThrownBy(() -> usuarioService.desativar(2L, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void desativar_gestorDesativandoAdmin_lancaUnauthorized() {
        Usuario admin = new Usuario(); admin.setDsEmail("admin@vesta.com");
        admin.setPerfil(perfil(NomePerfil.ADMIN));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.desativar(3L, gestorUser()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void desativar_adminDesativandoSiMesmo_lancaBusinessRuleException() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setDsEmail("admin@vesta.com");
        usuario.setPerfil(perfil(NomePerfil.ADMIN));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.desativar(1L, adminUser()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("si mesmo");
    }
}
