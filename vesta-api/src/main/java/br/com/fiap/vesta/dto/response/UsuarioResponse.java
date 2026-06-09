package br.com.fiap.vesta.dto.response;

import java.time.LocalDateTime;

public record UsuarioResponse(
    Long idUsuario,
    String nmUsuario,
    String nrCpf,
    String nrTelefone,
    String dsEmail,
    String nmPerfil,
    Long idAbrigo,
    String nmAbrigo,
    String stAtivo,
    LocalDateTime dtCriacao
) {}
