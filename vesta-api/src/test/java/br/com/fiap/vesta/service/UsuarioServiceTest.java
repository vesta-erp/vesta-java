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

        usuarioService.atualizar(1L, request);

        verify(passwordEncoder, never()).encode(any());
        assertThat(existente.getDsSenhaHash()).isEqualTo("$2a$hash-existente");
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
