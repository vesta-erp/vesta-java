package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.NomePerfil;
import br.com.fiap.vesta.dto.request.UsuarioRequest;
import br.com.fiap.vesta.dto.response.UsuarioResponse;
import br.com.fiap.vesta.exception.BusinessRuleException;
import br.com.fiap.vesta.exception.ConflictException;
import br.com.fiap.vesta.exception.ResourceNotFoundException;
import br.com.fiap.vesta.exception.UnauthorizedException;
import br.com.fiap.vesta.repository.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final AbrigoRepository abrigoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PerfilAcessoRepository perfilRepository,
                          AbrigoRepository abrigoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.abrigoRepository = abrigoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, UserDetails caller) {
        NomePerfil perfilSolicitado = resolverNomePerfil(request.nmPerfil());

        boolean callerIsGestor = caller.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));
        if (callerIsGestor && perfilSolicitado != NomePerfil.OPERADOR) {
            throw new UnauthorizedException("Gestor só pode criar usuários com perfil OPERADOR.");
        }

        if (usuarioRepository.existsByDsEmail(request.dsEmail())) {
            throw new ConflictException("Email já cadastrado: " + request.dsEmail());
        }
        String cpf = normalizarCpf(request.nrCpf());
        if (cpf != null && usuarioRepository.existsByNrCpf(cpf)) {
            throw new ConflictException("CPF já cadastrado.");
        }
        if (request.dsSenha() == null || request.dsSenha().isBlank()) {
            throw new BusinessRuleException("Senha é obrigatória no cadastro.");
        }
        if (perfilSolicitado == NomePerfil.OPERADOR && request.idAbrigo() == null) {
            throw new BusinessRuleException("Perfil OPERADOR requer um abrigo vinculado.");
        }

        PerfilAcesso perfil = perfilRepository.findByNmPerfil(perfilSolicitado)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil", 0L));

        Usuario usuario = new Usuario();
        usuario.setNmUsuario(request.nmUsuario());
        usuario.setNrCpf(cpf);
        usuario.setNrTelefone(request.nrTelefone());
        usuario.setDsEmail(request.dsEmail());
        usuario.setDsSenhaHash(passwordEncoder.encode(request.dsSenha()));
        usuario.setPerfil(perfil);
        if (request.idAbrigo() != null) {
            Abrigo abrigo = abrigoRepository.findById(request.idAbrigo())
                .orElseThrow(() -> new ResourceNotFoundException("Abrigo", request.idAbrigo()));
            usuario.setAbrigo(abrigo);
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UsuarioResponse buscarPorId(Long id, UserDetails caller) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        verificarAcessoAoUsuario(usuario, caller);
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request, UserDetails caller) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        verificarAcessoAoUsuario(usuario, caller);

        if (!usuario.getDsEmail().equals(request.dsEmail())
                && usuarioRepository.existsByDsEmail(request.dsEmail())) {
            throw new ConflictException("Email já cadastrado: " + request.dsEmail());
        }
        String cpf = normalizarCpf(request.nrCpf());
        if (cpf != null && !cpf.equals(usuario.getNrCpf())
                && usuarioRepository.existsByNrCpf(cpf)) {
            throw new ConflictException("CPF já cadastrado.");
        }

        NomePerfil perfilSolicitado = resolverNomePerfil(request.nmPerfil());
        if (perfilSolicitado == NomePerfil.OPERADOR && request.idAbrigo() == null) {
            throw new BusinessRuleException("Perfil OPERADOR requer um abrigo vinculado.");
        }

        PerfilAcesso perfil = perfilRepository.findByNmPerfil(perfilSolicitado)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil", 0L));

        usuario.setNmUsuario(request.nmUsuario());
        usuario.setNrCpf(cpf);
        usuario.setNrTelefone(request.nrTelefone());
        usuario.setDsEmail(request.dsEmail());
        usuario.setPerfil(perfil);
        if (request.dsSenha() != null && !request.dsSenha().isBlank()) {
            usuario.setDsSenhaHash(passwordEncoder.encode(request.dsSenha()));
        }
        if (request.idAbrigo() != null) {
            Abrigo abrigo = abrigoRepository.findById(request.idAbrigo())
                .orElseThrow(() -> new ResourceNotFoundException("Abrigo", request.idAbrigo()));
            usuario.setAbrigo(abrigo);
        } else {
            usuario.setAbrigo(null);
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desativar(Long id, UserDetails caller) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (usuario.getDsEmail().equals(caller.getUsername())
                && usuario.getPerfil().getNmPerfil() == NomePerfil.ADMIN) {
            throw new BusinessRuleException("Admin não pode desativar a si mesmo.");
        }
        verificarAcessoAoUsuario(usuario, caller);
        usuario.setStAtivo("N");
        usuarioRepository.save(usuario);
    }

    private void verificarAcessoAoUsuario(Usuario alvo, UserDetails caller) {
        boolean isAdmin = caller.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isGestor = caller.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));
        if (isGestor) {
            boolean isSelf = alvo.getDsEmail().equals(caller.getUsername());
            boolean targetIsOperador = alvo.getPerfil().getNmPerfil() == NomePerfil.OPERADOR;
            if (!isSelf && !targetIsOperador) {
                throw new UnauthorizedException("Gestor não tem acesso a este usuário.");
            }
            return;
        }

        if (!alvo.getDsEmail().equals(caller.getUsername())) {
            throw new UnauthorizedException("Operador só pode acessar o próprio usuário.");
        }
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        return cpf.replaceAll("[^0-9]", "");
    }

    private NomePerfil resolverNomePerfil(String nmPerfil) {
        try {
            return NomePerfil.valueOf(nmPerfil.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Perfil inválido: " + nmPerfil);
        }
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
            u.getIdUsuario(),
            u.getNmUsuario(),
            u.getNrCpf(),
            u.getNrTelefone(),
            u.getDsEmail(),
            u.getPerfil().getNmPerfil().name(),
            u.getAbrigo() != null ? u.getAbrigo().getIdAbrigo() : null,
            u.getAbrigo() != null ? u.getAbrigo().getNmAbrigo() : null,
            u.getStAtivo(),
            u.getDtCriacao()
        );
    }
}
