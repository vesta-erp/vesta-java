package br.com.fiap.vesta.service;

import br.com.fiap.vesta.exception.UnauthorizedException;
import br.com.fiap.vesta.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class IsolamentoService {

    private final UsuarioRepository usuarioRepository;

    public IsolamentoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void verificarAcessoAbrigo(Long idAbrigoAlvo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        boolean isOperador = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_OPERADOR"));
        if (!isOperador) return;

        String email = auth.getName();
        var usuario = usuarioRepository.findByDsEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado."));

        if (usuario.getAbrigo() == null) {
            throw new UnauthorizedException("Operador não está vinculado a nenhum abrigo.");
        }
        if (!usuario.getAbrigo().getIdAbrigo().equals(idAbrigoAlvo)) {
            throw new UnauthorizedException("Operador não tem acesso ao abrigo " + idAbrigoAlvo + ".");
        }
    }
}
