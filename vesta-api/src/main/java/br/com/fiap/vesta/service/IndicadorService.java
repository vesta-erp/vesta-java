package br.com.fiap.vesta.service;

import br.com.fiap.vesta.client.CriticidadeClient;
import br.com.fiap.vesta.client.dto.CriticidadeResponse;
import br.com.fiap.vesta.domain.entity.Abrigo;
import br.com.fiap.vesta.domain.enums.StatusOcorrencia;
import br.com.fiap.vesta.dto.response.IndicadorAbrigoResponse;
import br.com.fiap.vesta.exception.ResourceNotFoundException;
import br.com.fiap.vesta.repository.AbrigoRepository;
import br.com.fiap.vesta.repository.EstoqueAbrigoRepository;
import br.com.fiap.vesta.repository.OcorrenciaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IndicadorService {

    private final AbrigoRepository abrigoRepository;
    private final EstoqueAbrigoRepository estoqueRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final CriticidadeClient criticidadeClient;

    public IndicadorService(AbrigoRepository abrigoRepository,
                            EstoqueAbrigoRepository estoqueRepository,
                            OcorrenciaRepository ocorrenciaRepository,
                            CriticidadeClient criticidadeClient) {
        this.abrigoRepository = abrigoRepository;
        this.estoqueRepository = estoqueRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.criticidadeClient = criticidadeClient;
    }

    @Cacheable("indicadores")
    public List<IndicadorAbrigoResponse> rankingCriticidade() {
        List<IndicadorAbrigoResponse> locais = abrigoRepository.findAll().stream()
            .map(this::calcularIndicador)
            .toList();

        Map<Long, CriticidadeResponse> scoresNet = criticidadeClient.listarCriticidade()
            .stream()
            .collect(Collectors.toMap(CriticidadeResponse::idAbrigo, Function.identity()));

        return locais.stream()
            .map(ind -> enriquecer(ind, scoresNet.get(ind.idAbrigo())))
            .sorted(Comparator.comparingDouble((IndicadorAbrigoResponse r) ->
                r.scoreNet() != null ? r.scoreNet() : (double) r.nivelCriticidade()
            ).reversed())
            .toList();
    }

    public IndicadorAbrigoResponse indicadorPorAbrigo(Long idAbrigo) {
        Abrigo abrigo = abrigoRepository.findById(idAbrigo)
            .orElseThrow(() -> new ResourceNotFoundException("Abrigo", idAbrigo));
        IndicadorAbrigoResponse local = calcularIndicador(abrigo);
        CriticidadeResponse scoreNet = criticidadeClient.buscarCriticidade(idAbrigo);
        return enriquecer(local, scoreNet);
    }

    private IndicadorAbrigoResponse enriquecer(IndicadorAbrigoResponse local, CriticidadeResponse score) {
        if (score == null) return local;
        return new IndicadorAbrigoResponse(
            local.idAbrigo(), local.nmAbrigo(), local.nmRegiao(), local.stStatus(),
            local.qtCapacidadeMaxima(), local.qtOcupacaoAtual(), local.taxaOcupacao(),
            local.qtItensAbaixoMinimo(), local.qtOcorrenciasAbertas(),
            local.nivelCriticidade(), local.descricaoCriticidade(),
            score.score(), score.nivel(), score.justificativa(), score.recomendacoes()
        );
    }

    private IndicadorAbrigoResponse calcularIndicador(Abrigo a) {
        double taxaOcupacao = a.getQtCapacidadeMaxima() > 0
            ? (a.getQtOcupacaoAtual() * 100.0 / a.getQtCapacidadeMaxima())
            : 0;

        List<?> todosItens = estoqueRepository.findByAbrigoIdAbrigo(a.getIdAbrigo());
        int totalItens = todosItens.size();
        int itensAbaixo = estoqueRepository.findItensAbaixoMinimoPorAbrigo(a.getIdAbrigo()).size();
        double scoreEstoque = totalItens > 0 ? (itensAbaixo * 100.0 / totalItens) : 0;

        long ocorrenciasAbertas = ocorrenciaRepository
            .countByAbrigoIdAbrigoAndStStatusNot(a.getIdAbrigo(), StatusOcorrencia.RESOLVIDA);
        double scoreOcorrencias = Math.min(ocorrenciasAbertas * 20.0, 100);

        int nivelCriticidade = (int) ((taxaOcupacao + scoreEstoque + scoreOcorrencias) / 3);

        String descricao = switch (nivelCriticidade / 25) {
            case 0 -> "BAIXO";
            case 1 -> "MEDIO";
            case 2 -> "ALTO";
            default -> "CRITICO";
        };

        return new IndicadorAbrigoResponse(
            a.getIdAbrigo(), a.getNmAbrigo(),
            a.getRegiao() != null ? a.getRegiao().getNmRegiao() : "-",
            a.getStStatus(),
            a.getQtCapacidadeMaxima(), a.getQtOcupacaoAtual(),
            taxaOcupacao, itensAbaixo, ocorrenciasAbertas,
            nivelCriticidade, descricao,
            null, null, null, null
        );
    }
}
