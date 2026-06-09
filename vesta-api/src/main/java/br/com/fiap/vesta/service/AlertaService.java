package br.com.fiap.vesta.service;

import br.com.fiap.vesta.domain.entity.Alerta;
import br.com.fiap.vesta.dto.response.AlertaResponse;
import br.com.fiap.vesta.exception.ResourceNotFoundException;
import br.com.fiap.vesta.repository.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final IsolamentoService isolamentoService;

    public AlertaService(AlertaRepository alertaRepository, IsolamentoService isolamentoService) {
        this.alertaRepository = alertaRepository;
        this.isolamentoService = isolamentoService;
    }

    public List<AlertaResponse> listarAtivos() {
        if (isolamentoService.isGestor()) {
            Long idRegiao = isolamentoService.getIdRegiaoGestor();
            if (idRegiao == null) return List.of();
            return alertaRepository.findByAbrigoRegiaoIdRegiaoAndStStatus(idRegiao, "ATIVO")
                .stream().map(this::toResponse).toList();
        }
        return alertaRepository.findByStStatus("ATIVO").stream().map(this::toResponse).toList();
    }

    public List<AlertaResponse> listarPorAbrigo(Long idAbrigo) {
        List<Alerta> alertas = alertaRepository.findByAbrigoIdAbrigoAndStStatus(idAbrigo, "ATIVO");
        if (isolamentoService.isGestor()) {
            Long idRegiao = isolamentoService.getIdRegiaoGestor();
            alertas = alertas.stream()
                .filter(a -> a.getAbrigo().getRegiao() != null
                    && a.getAbrigo().getRegiao().getIdRegiao().equals(idRegiao))
                .toList();
        }
        return alertas.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertaResponse resolver(Long id) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Alerta", id));
        alerta.setStStatus("RESOLVIDO");
        alerta.setDtResolucao(LocalDateTime.now());
        return toResponse(alertaRepository.save(alerta));
    }

    private AlertaResponse toResponse(Alerta a) {
        return new AlertaResponse(
            a.getIdAlerta(),
            a.getAbrigo().getIdAbrigo(), a.getAbrigo().getNmAbrigo(),
            a.getTpAlerta(), a.getDsMensagem(), a.getStStatus(),
            a.getDtGeracao(), a.getDtResolucao(),
            a.getRecurso() != null ? a.getRecurso().getIdRecurso() : null
        );
    }
}
