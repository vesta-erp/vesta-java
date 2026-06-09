package br.com.fiap.vesta.service;

import br.com.fiap.vesta.client.CriticidadeClient;
import br.com.fiap.vesta.client.dto.CriticidadeResponse;
import br.com.fiap.vesta.domain.entity.Abrigo;
import br.com.fiap.vesta.domain.entity.Regiao;
import br.com.fiap.vesta.domain.enums.StatusAbrigo;
import br.com.fiap.vesta.domain.enums.StatusOcorrencia;
import br.com.fiap.vesta.dto.response.IndicadorAbrigoResponse;
import br.com.fiap.vesta.repository.AbrigoRepository;
import br.com.fiap.vesta.repository.EstoqueAbrigoRepository;
import br.com.fiap.vesta.repository.OcorrenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadorServiceTest {

    @Mock AbrigoRepository abrigoRepository;
    @Mock EstoqueAbrigoRepository estoqueRepository;
    @Mock OcorrenciaRepository ocorrenciaRepository;
    @Mock CriticidadeClient criticidadeClient;

    @InjectMocks IndicadorService indicadorService;

    private Abrigo abrigo;

    @BeforeEach
    void setup() {
        abrigo = new Abrigo();
        abrigo.setIdAbrigo(1L);
        abrigo.setNmAbrigo("Abrigo Teste");
        abrigo.setQtCapacidadeMaxima(100);
        abrigo.setQtOcupacaoAtual(90);
        abrigo.setStStatus(StatusAbrigo.ATIVO);
        Regiao regiao = new Regiao();
        regiao.setNmRegiao("Sul");
        abrigo.setRegiao(regiao);
    }

    private void mockRepositorios() {
        when(estoqueRepository.findByAbrigoIdAbrigo(1L)).thenReturn(List.of());
        when(estoqueRepository.findItensAbaixoMinimoPorAbrigo(1L)).thenReturn(List.of());
        when(ocorrenciaRepository.countByAbrigoIdAbrigoAndStStatusNot(1L, StatusOcorrencia.RESOLVIDA))
            .thenReturn(0L);
    }

    @Test
    void rankingCriticidade_comDotNetDisponivel_enriqueceCadaIndicadorComScoreNet() {
        mockRepositorios();
        when(abrigoRepository.findAll()).thenReturn(List.of(abrigo));
        when(criticidadeClient.listarCriticidade()).thenReturn(List.of(
            new CriticidadeResponse(1L, 85.0, "ALTO", "Alta ocupação", List.of("Transferir famílias"))
        ));

        List<IndicadorAbrigoResponse> ranking = indicadorService.rankingCriticidade();

        assertThat(ranking).hasSize(1);
        IndicadorAbrigoResponse ind = ranking.get(0);
        assertThat(ind.scoreNet()).isEqualTo(85.0);
        assertThat(ind.nivelNet()).isEqualTo("ALTO");
        assertThat(ind.justificativa()).isEqualTo("Alta ocupação");
        assertThat(ind.recomendacoes()).containsExactly("Transferir famílias");
    }

    @Test
    void rankingCriticidade_comDotNetIndisponivel_retornaIndicadoresComScoreNetNulo() {
        mockRepositorios();
        when(abrigoRepository.findAll()).thenReturn(List.of(abrigo));
        when(criticidadeClient.listarCriticidade()).thenReturn(Collections.emptyList());

        List<IndicadorAbrigoResponse> ranking = indicadorService.rankingCriticidade();

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).scoreNet()).isNull();
        assertThat(ranking.get(0).nivelCriticidade()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void indicadorPorAbrigo_comDotNetDisponivel_enriqueceIndicador() {
        mockRepositorios();
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(abrigo));
        when(criticidadeClient.buscarCriticidade(1L)).thenReturn(
            new CriticidadeResponse(1L, 72.0, "MEDIO", "Ocupação moderada", List.of())
        );

        IndicadorAbrigoResponse ind = indicadorService.indicadorPorAbrigo(1L);

        assertThat(ind.scoreNet()).isEqualTo(72.0);
        assertThat(ind.nivelNet()).isEqualTo("MEDIO");
        assertThat(ind.justificativa()).isEqualTo("Ocupação moderada");
    }

    @Test
    void indicadorPorAbrigo_comFallbackAtivado_scoreNetNuloECriticidadeLocalRetornada() {
        mockRepositorios();
        when(abrigoRepository.findById(1L)).thenReturn(Optional.of(abrigo));
        when(criticidadeClient.buscarCriticidade(1L)).thenReturn(
            new CriticidadeResponse(1L, 0.0, "INDISPONIVEL",
                "Serviço .NET temporariamente indisponível", Collections.emptyList())
        );

        IndicadorAbrigoResponse ind = indicadorService.indicadorPorAbrigo(1L);

        assertThat(ind.scoreNet()).isNull();
        assertThat(ind.nivelNet()).isNull();
        assertThat(ind.nivelCriticidade()).isGreaterThanOrEqualTo(0);
    }
}
