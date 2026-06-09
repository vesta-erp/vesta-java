package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.SeveridadeOcorrencia;
import br.com.fiap.vesta.domain.enums.TipoAlerta;
import br.com.fiap.vesta.dto.request.OcorrenciaRequest;
import br.com.fiap.vesta.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcorrenciaServiceTest {

    @Mock OcorrenciaRepository ocorrenciaRepository;
    @Mock AbrigoService abrigoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AlertaRepository alertaRepository;
    @Mock IsolamentoService isolamentoService;

    @InjectMocks OcorrenciaService ocorrenciaService;

    private Abrigo abrigo;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        abrigo = new Abrigo();
        abrigo.setIdAbrigo(1L);
        abrigo.setNmAbrigo("Abrigo Teste");

        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNmUsuario("Operador");

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ocorrenciaRepository.save(any())).thenAnswer(inv -> {
            Ocorrencia o = inv.getArgument(0);
            o.setIdOcorrencia(1L);
            return o;
        });
    }

    @Test
    void criar_ocorrenciaCritica_semAlertaAtivo_geraAlerta() {
        lenient().when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndStStatus(
                1L, TipoAlerta.OCORRENCIA_CRITICA, "ATIVO"))
            .thenReturn(Optional.empty());

        ocorrenciaService.criar(1L, 1L, new OcorrenciaRequest("Incêndio", "desc", SeveridadeOcorrencia.CRITICA));

        verify(alertaRepository).save(any(Alerta.class));
    }

    @Test
    void criar_ocorrenciaCritica_comAlertaJaAtivo_naoCriaDuplicata() {
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndStStatus(
                1L, TipoAlerta.OCORRENCIA_CRITICA, "ATIVO"))
            .thenReturn(Optional.of(new Alerta()));

        ocorrenciaService.criar(1L, 1L, new OcorrenciaRequest("Outro Incêndio", "desc", SeveridadeOcorrencia.CRITICA));

        verify(alertaRepository, never()).save(any(Alerta.class));
    }

    @Test
    void criar_ocorrenciaAlta_naoGeraAlerta() {
        ocorrenciaService.criar(1L, 1L, new OcorrenciaRequest("Problema", "desc", SeveridadeOcorrencia.ALTA));

        verify(alertaRepository, never()).save(any(Alerta.class));
    }

    @Test
    void criar_ocorrenciaCritica_comAlertaAnteriorResolvido_geraNovoAlerta() {
        lenient().when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndStStatus(
                1L, TipoAlerta.OCORRENCIA_CRITICA, "ATIVO"))
            .thenReturn(Optional.empty());

        ocorrenciaService.criar(1L, 1L, new OcorrenciaRequest("Novo Incêndio", "desc", SeveridadeOcorrencia.CRITICA));

        verify(alertaRepository).save(any(Alerta.class));
    }
}
