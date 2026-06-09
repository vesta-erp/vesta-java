package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.*;
import br.com.fiap.vesta.domain.enums.TipoAlerta;
import br.com.fiap.vesta.domain.enums.TipoMovimentacao;
import br.com.fiap.vesta.dto.request.MovimentacaoRequest;
import br.com.fiap.vesta.exception.BusinessRuleException;
import br.com.fiap.vesta.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock EstoqueAbrigoRepository estoqueRepository;
    @Mock MovimentacaoRecursoRepository movimentacaoRepository;
    @Mock RecursoRepository recursoRepository;
    @Mock AbrigoService abrigoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SolicitacaoRecursoRepository solicitacaoRepository;
    @Mock AlertaRepository alertaRepository;
    @Mock IsolamentoService isolamentoService;

    @InjectMocks EstoqueService estoqueService;

    @Test
    void saida_semEstoqueSuficiente_lancaException() {
        Abrigo abrigo = new Abrigo(); abrigo.setIdAbrigo(1L);
        Recurso recurso = new Recurso(); recurso.setIdRecurso(1L);
        EstoqueAbrigo estoque = new EstoqueAbrigo();
        estoque.setAbrigo(abrigo); estoque.setRecurso(recurso);
        estoque.setQtAtual(5); estoque.setQtMinima(0);
        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 1L))
            .thenReturn(Optional.of(estoque));

        var req = new MovimentacaoRequest(1L, TipoMovimentacao.SAIDA, 10, null, null);
        assertThatThrownBy(() -> estoqueService.registrarMovimentacao(1L, 1L, req))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("insuficiente");
    }

    @Test
    void doisRecursosCriticos_geramAlertasDistintos() {
        Abrigo abrigo = new Abrigo(); abrigo.setIdAbrigo(1L);
        Recurso r1 = new Recurso(); r1.setIdRecurso(1L); r1.setNmRecurso("Água");
        Recurso r2 = new Recurso(); r2.setIdRecurso(2L); r2.setNmRecurso("Cobertor");
        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        EstoqueAbrigo e1 = new EstoqueAbrigo();
        e1.setAbrigo(abrigo); e1.setRecurso(r1); e1.setQtAtual(0); e1.setQtMinima(10);

        EstoqueAbrigo e2 = new EstoqueAbrigo();
        e2.setAbrigo(abrigo); e2.setRecurso(r2); e2.setQtAtual(0); e2.setQtMinima(5);

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        when(recursoRepository.findById(1L)).thenReturn(Optional.of(r1));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 1L)).thenReturn(Optional.of(e1));
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 1L, "ATIVO")).thenReturn(Optional.empty());

        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(1L, TipoMovimentacao.SAIDA, 0, null, null));

        when(recursoRepository.findById(2L)).thenReturn(Optional.of(r2));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 2L)).thenReturn(Optional.of(e2));
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 2L, "ATIVO")).thenReturn(Optional.empty());

        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(2L, TipoMovimentacao.SAIDA, 0, null, null));

        verify(alertaRepository, times(2)).save(any(Alerta.class));
    }

    @Test
    void movimentacaoNormalizaEstoque_resolveAlertaEstoqueCritico() {
        Abrigo abrigo = new Abrigo(); abrigo.setIdAbrigo(1L);
        Recurso recurso = new Recurso(); recurso.setIdRecurso(1L); recurso.setNmRecurso("Água");
        EstoqueAbrigo estoque = new EstoqueAbrigo();
        estoque.setAbrigo(abrigo); estoque.setRecurso(recurso);
        estoque.setQtAtual(5); estoque.setQtMinima(10);
        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        Alerta alertaAtivo = new Alerta();
        alertaAtivo.setStStatus("ATIVO");

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 1L)).thenReturn(Optional.of(estoque));
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 1L, "ATIVO")).thenReturn(Optional.of(alertaAtivo));

        // 5 + 10 = 15 >= 10 (mínimo) → estoque normalizado
        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(1L, TipoMovimentacao.ENTRADA, 10, null, null));

        verify(alertaRepository).save(argThat(a -> "RESOLVIDO".equals(a.getStStatus())));
    }

    @Test
    void movimentacaoMantemEstoqueCritico_naoResolveAlerta() {
        Abrigo abrigo = new Abrigo(); abrigo.setIdAbrigo(1L);
        Recurso recurso = new Recurso(); recurso.setIdRecurso(1L); recurso.setNmRecurso("Água");
        EstoqueAbrigo estoque = new EstoqueAbrigo();
        estoque.setAbrigo(abrigo); estoque.setRecurso(recurso);
        estoque.setQtAtual(2); estoque.setQtMinima(10);
        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 1L)).thenReturn(Optional.of(estoque));
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 1L, "ATIVO")).thenReturn(Optional.empty());

        // 2 + 1 = 3 < 10 (mínimo) → ainda crítico
        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(1L, TipoMovimentacao.ENTRADA, 1, null, null));

        verify(alertaRepository, never()).save(argThat(a -> "RESOLVIDO".equals(a.getStStatus())));
    }

    @Test
    void mesmoRecursoCriticoDuasVezes_geraSomenteUmAlerta() {
        Abrigo abrigo = new Abrigo(); abrigo.setIdAbrigo(1L);
        Recurso recurso = new Recurso(); recurso.setIdRecurso(1L); recurso.setNmRecurso("Água");
        Usuario usuario = new Usuario(); usuario.setIdUsuario(1L);

        EstoqueAbrigo estoque = new EstoqueAbrigo();
        estoque.setAbrigo(abrigo); estoque.setRecurso(recurso); estoque.setQtAtual(0); estoque.setQtMinima(10);

        when(abrigoService.buscarPorId(1L)).thenReturn(abrigo);
        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estoqueRepository.findByAbrigoIdAbrigoAndRecursoIdRecurso(1L, 1L)).thenReturn(Optional.of(estoque));

        // Primeira movimentação: nenhum alerta ativo ainda
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 1L, "ATIVO")).thenReturn(Optional.empty());
        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(1L, TipoMovimentacao.SAIDA, 0, null, null));

        // Segunda movimentação: alerta já existe
        when(alertaRepository.findByAbrigoIdAbrigoAndTpAlertaAndRecursoIdRecursoAndStStatus(
                1L, TipoAlerta.ESTOQUE_CRITICO, 1L, "ATIVO")).thenReturn(Optional.of(new Alerta()));
        estoqueService.registrarMovimentacao(1L, 1L,
                new MovimentacaoRequest(1L, TipoMovimentacao.SAIDA, 0, null, null));

        verify(alertaRepository, times(1)).save(any(Alerta.class));
    }
}
