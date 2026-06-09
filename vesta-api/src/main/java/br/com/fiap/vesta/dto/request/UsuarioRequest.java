package br.com.fiap.vesta.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsuarioRequest(
    @NotBlank String nmUsuario,
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter exatamente 11 dígitos numéricos, sem máscara")
    String nrCpf,
    String nrTelefone,
    @NotBlank @Email String dsEmail,
    String dsSenha,
    @NotBlank String nmPerfil,
    Long idAbrigo
) {}
