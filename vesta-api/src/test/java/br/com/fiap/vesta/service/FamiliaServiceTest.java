package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.StatusAbrigo;
import br.com.fiap.vesta.domain.enums.TipoAlerta;
import java.util.List;
import br.com.fiap.vesta.exception.BusinessRuleException;
import br.com.fiap.vesta.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamiliaServiceTest {

    @Mock FamiliaRepository familiaRepository;
    @Mock PessoaAbrigadaRepository pessoaRepository;
    @Mock AbrigoRepository abrigoRepository;
    @Mock AbrigoService abrigoService;
    @Mock AlertaRepository alertaRepository;

    @InjectMocks FamiliaService familiaService;

    private Abrigo abrigo;

    @BeforeEach
    void setup() {
        abrigo = new Abrigo();
        abrigo.setIdAbrigo(1L);
        abrigo.setQtCapacidadeMaxima(100);
        abrigo.setQtOcupacaoAtual(50);
        abrigo.setStStatus(StatusAbrigo.ATIVO);
    }

    @Test
    void acolhimento_abrigoLotado_lancaException() {
        abrigo.setQtOcupacaoAtual(100);
        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);

        var request = criarAcolhimentoRequest(1);
        assertThatThrownBy(() -> familiaService.registrarAcolhimento(1L, request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("capacidade");
    }

    @Test
    void acolhimento_abrigoComVagas_salvaFamiliaEPessoas() {
        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(familiaRepository.save(any())).thenAnswer(inv -> {
            Familia f = inv.getArgument(0);
            f.setIdFamilia(1L);
            return f;
        });
        when(pessoaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(abrigoRepository.save(any())).thenReturn(abrigo);

        var request = criarAcolhimentoRequest(2);
        familiaService.registrarAcolhimento(1L, request);

        verify(familiaRepository).save(any());
        verify(pessoaRepository, times(2)).save(any());
        verify(abrigoRepository).save(argThat(a -> a.getQtOcupacaoAtual() == 52));
    }

    @Test
    void acolhimento_queAtingeCapacidade_chamaGerarAlertaLotacao() {
        abrigo.setQtOcupacaoAtual(99);
        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(familiaRepository.save(any())).thenAnswer(inv -> {
            Familia f = inv.getArgument(0); f.setIdFamilia(1L); return f;
        });
        when(pessoaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(abrigoRepository.save(any())).thenReturn(abrigo);

        familiaService.registrarAcolhimento(1L, criarAcolhimentoRequest(1));

        verify(abrigoService).gerarAlertaLotacao(abrigo);
    }

    @Test
    void acolhimento_queNaoAtingeCapacidade_naoGeraAlerta() {
        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(familiaRepository.save(any())).thenAnswer(inv -> {
            Familia f = inv.getArgument(0); f.setIdFamilia(1L); return f;
        });
        when(pessoaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(abrigoRepository.save(any())).thenReturn(abrigo);

        familiaService.registrarAcolhimento(1L, criarAcolhimentoRequest(1));

        verify(abrigoService, never()).gerarAlertaLotacao(any());
    }

    @Test
    void saida_desocupaAbrigoLotado_resolveAlertaLotacao() {
        abrigo.setQtCapacidadeMaxima(10);
        abrigo.setQtOcupacaoAtual(10);
        abrigo.setStStatus(StatusAbrigo.LOTADO);

        Familia familia = new Familia();
        familia.setIdFamilia(1L);
        familia.setAbrigo(abrigo);

        PessoaAbrigada pessoa = new PessoaAbrigada(); // stPresente = "S" por padrão

        Alerta alertaAtivo = new Alerta();
        alertaAtivo.setStStatus("ATIVO");

        when(familiaRepository.findById(1L)).thenReturn(Optional.of(familia));
        when(pessoaRepository.findByFamiliaIdFamilia(1L)).thenReturn(List.of(pessoa));
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndStStatus(
                1L, TipoAlerta.LOTACAO, "ATIVO")).thenReturn(Optional.of(alertaAtivo));

        familiaService.registrarSaida(1L);

        verify(alertaRepository).save(argThat(a -> "RESOLVIDO".equals(a.getStStatus())));
    }

    @Test
    void saida_abrigoNaoLotado_naoTocaAlertas() {
        // abrigo ATIVO com espaço disponível — saída não muda status
        Familia familia = new Familia();
        familia.setIdFamilia(1L);
        familia.setAbrigo(abrigo); // status ATIVO, 50/100

        PessoaAbrigada pessoa = new PessoaAbrigada();

        when(familiaRepository.findById(1L)).thenReturn(Optional.of(familia));
        when(pessoaRepository.findByFamiliaIdFamilia(1L)).thenReturn(List.of(pessoa));

        familiaService.registrarSaida(1L);

        verify(alertaRepository, never()).findByAbrigoIdAbrigoAndTpAlertaAndStStatus(any(), any(), any());
        verify(alertaRepository, never()).save(any());
    }

    private br.com.fiap.vesta.dto.request.AcolhimentoRequest criarAcolhimentoRequest(int membros) {
        var lista = java.util.List.of(
            new br.com.fiap.vesta.dto.request.AcolhimentoRequest.MembroRequest("Pessoa 1", null, null, null),
            new br.com.fiap.vesta.dto.request.AcolhimentoRequest.MembroRequest("Pessoa 2", null, null, null)
        ).subList(0, membros);
        return new br.com.fiap.vesta.dto.request.AcolhimentoRequest("Responsável", null, null, lista);
    }
}
