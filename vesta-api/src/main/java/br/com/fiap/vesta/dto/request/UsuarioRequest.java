package br.com.fiap.vesta.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest(
    @NotBlank String nmUsuario,
    String nrCpf,
    String nrTelefone,
    @NotBlank @Email String dsEmail,
    String dsSenha,
    @NotBlank String nmPerfil,
    Long idAbrigo
) {}
