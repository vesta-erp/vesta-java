package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.StatusAbrigo;
import br.com.fiap.vesta.domain.enums.StatusTransferencia;
import br.com.fiap.vesta.exception.BusinessRuleException;
import br.com.fiap.vesta.dto.request.TransferenciaRequest;
import br.com.fiap.vesta.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock TransferenciaAbrigoRepository transferenciaRepository;
    @Mock FamiliaRepository familiaRepository;
    @Mock PessoaAbrigadaRepository pessoaRepository;
    @Mock AbrigoRepository abrigoRepository;
    @Mock AbrigoService abrigoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock IsolamentoService isolamentoService;

    @InjectMocks TransferenciaService transferenciaService;

    @Test
    void solicitarTransferencia_mesmoAbrigo_lancaException() {
        assertThatThrownBy(() ->
            transferenciaService.solicitar(1L, 1L, new TransferenciaRequest(1L, 1L, "motivo")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("mesmo abrigo");
    }

    // --- concluir() ---

    private TransferenciaAbrigo montarTransferenciaAprovada(Abrigo origem, Abrigo destino,
                                                             Familia familia, int idTransferencia) {
        TransferenciaAbrigo t = new TransferenciaAbrigo();
        t.setIdTransferencia((long) idTransferencia);
        t.setAbrigoOrigem(origem);
        t.setAbrigoDestino(destino);
        t.setFamilia(familia);
        t.setStStatus(StatusTransferencia.APROVADA);
        return t;
    }

    private PessoaAbrigada pessoaPresente(Abrigo abrigo) {
        PessoaAbrigada p = new PessoaAbrigada();
        p.setIdPessoa(1L);
        p.setStPresente("S");
        p.setAbrigo(abrigo);
        return p;
    }

    @Test
    void concluir_queLotaDestino_geraAlertaLotacao() {
        Abrigo origem = new Abrigo();
        origem.setIdAbrigo(1L); origem.setNmAbrigo("Origem");
        origem.setQtCapacidadeMaxima(100); origem.setQtOcupacaoAtual(5);
        origem.setStStatus(StatusAbrigo.ATIVO);

        Abrigo destino = new Abrigo();
        destino.setIdAbrigo(2L); destino.setNmAbrigo("Destino");
        destino.setQtCapacidadeMaxima(100); destino.setQtOcupacaoAtual(99);
        destino.setStStatus(StatusAbrigo.ATIVO);

        Familia familia = new Familia();
        familia.setIdFamilia(1L); familia.setNmResponsavel("Resp"); familia.setAbrigo(origem);

        TransferenciaAbrigo t = montarTransferenciaAprovada(origem, destino, familia, 10);

        when(transferenciaRepository.findById(10L)).thenReturn(Optional.of(t));
        when(pessoaRepository.findByFamiliaIdFamilia(1L)).thenReturn(List.of(pessoaPresente(origem)));
        when(transferenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transferenciaService.concluir(10L);

        verify(abrigoService).gerarAlertaLotacao(destino);
    }

    @Test
    void concluir_queNaoLotaDestino_naoGeraAlerta() {
        Abrigo origem = new Abrigo();
        origem.setIdAbrigo(1L); origem.setNmAbrigo("Origem");
        origem.setQtCapacidadeMaxima(100); origem.setQtOcupacaoAtual(5);
        origem.setStStatus(StatusAbrigo.ATIVO);

        Abrigo destino = new Abrigo();
        destino.setIdAbrigo(2L); destino.setNmAbrigo("Destino");
        destino.setQtCapacidadeMaxima(100); destino.setQtOcupacaoAtual(50);
        destino.setStStatus(StatusAbrigo.ATIVO);

        Familia familia = new Familia();
        familia.setIdFamilia(1L); familia.setNmResponsavel("Resp"); familia.setAbrigo(origem);

        TransferenciaAbrigo t = montarTransferenciaAprovada(origem, destino, familia, 10);

        when(transferenciaRepository.findById(10L)).thenReturn(Optional.of(t));
        when(pessoaRepository.findByFamiliaIdFamilia(1L)).thenReturn(List.of(pessoaPresente(origem)));
        when(transferenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transferenciaService.concluir(10L);

        verify(abrigoService, never()).gerarAlertaLotacao(any());
    }

    @Test
    void concluir_destinoJaLotadoComAlertaAtivo_delegaDedupParaGerarAlerta() {
        Abrigo origem = new Abrigo();
        origem.setIdAbrigo(1L); origem.setNmAbrigo("Origem");
        origem.setQtCapacidadeMaxima(100); origem.setQtOcupacaoAtual(5);
        origem.setStStatus(StatusAbrigo.ATIVO);

        Abrigo destino = new Abrigo();
        destino.setIdAbrigo(2L); destino.setNmAbrigo("Destino");
        destino.setQtCapacidadeMaxima(100); destino.setQtOcupacaoAtual(100);
        destino.setStStatus(StatusAbrigo.LOTADO);

        Familia familia = new Familia();
        familia.setIdFamilia(1L); familia.setNmResponsavel("Resp"); familia.setAbrigo(origem);

        TransferenciaAbrigo t = montarTransferenciaAprovada(origem, destino, familia, 10);

        when(transferenciaRepository.findById(10L)).thenReturn(Optional.of(t));
        when(pessoaRepository.findByFamiliaIdFamilia(1L)).thenReturn(List.of(pessoaPresente(origem)));
        when(transferenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transferenciaService.concluir(10L);

        verify(abrigoService).gerarAlertaLotacao(destino);
    }

    @Test
    void solicitarTransferencia_destinoSemVagas_lancaException() {
        Abrigo origem = new Abrigo();
        origem.setIdAbrigo(1L); origem.setQtCapacidadeMaxima(100); origem.setQtOcupacaoAtual(50);
        origem.setStStatus(StatusAbrigo.ATIVO);

        Abrigo destino = new Abrigo();
        destino.setIdAbrigo(2L); destino.setQtCapacidadeMaxima(10); destino.setQtOcupacaoAtual(10);
        destino.setStStatus(StatusAbrigo.LOTADO);

        Familia familia = new Familia(); familia.setIdFamilia(1L); familia.setAbrigo(origem);

        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        when(abrigoService.buscarPorId(1L)).thenReturn(origem);
        when(abrigoService.buscarPorId(2L)).thenReturn(destino);
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));
        when(usuarioRepository.findById(1L)).thenReturn(java.util.Optional.of(usuario));
        when(pessoaRepository.countByAbrigoIdAbrigoAndStPresente(1L, "S")).thenReturn(3L);

        assertThatThrownBy(() ->
            transferenciaService.solicitar(1L, 1L, new TransferenciaRequest(1L, 2L, "motivo")))
            .isInstanceOf(BusinessRuleException.class);
    }
}
