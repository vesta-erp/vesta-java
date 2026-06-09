package br.com.fiap.vesta.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record AcolhimentoRequest(
    @NotBlank String nmResponsavel,
    @Pattern(regexp = "^(\\d{11})?$", message = "CPF deve conter exatamente 11 dígitos numéricos, sem máscara")
    String nrCpfResponsavel,
    String nrTelefone,
    @NotNull @Size(min = 1) List<MembroRequest> membros
) {
    public record MembroRequest(
        @NotBlank String nmPessoa,
        LocalDate dtNascimento,
        String tpDocumento,
        String nrDocumento
    ) {}
}
