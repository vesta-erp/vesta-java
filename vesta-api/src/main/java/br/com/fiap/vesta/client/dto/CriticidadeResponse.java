package br.com.fiap.vesta.client.dto;

import java.util.List;

public record CriticidadeResponse(
    Long idAbrigo,
    double score,
    String nivel,
    String justificativa,
    List<String> recomendacoes
) {}
