package br.com.fiap.vesta.dto.response;

import br.com.fiap.vesta.domain.enums.StatusAbrigo;
import java.util.List;

public record IndicadorAbrigoResponse(
    Long idAbrigo,
    String nmAbrigo,
    String nmRegiao,
    StatusAbrigo stStatus,
    Integer qtCapacidadeMaxima,
    Integer qtOcupacaoAtual,
    double taxaOcupacao,
    int qtItensAbaixoMinimo,
    long qtOcorrenciasAbertas,
    int nivelCriticidade,
    String descricaoCriticidade,
    Double scoreNet,
    String nivelNet,
    String justificativa,
    List<String> recomendacoes
) {}
