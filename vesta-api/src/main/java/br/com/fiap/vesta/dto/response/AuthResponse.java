package br.com.fiap.vesta.dto.response;

public record AuthResponse(String token, String tipo, String email, String perfil, String nmUsuario) {
    public static AuthResponse of(String token, String email, String perfil, String nmUsuario) {
        return new AuthResponse(token, "Bearer", email, perfil, nmUsuario);
    }
}
