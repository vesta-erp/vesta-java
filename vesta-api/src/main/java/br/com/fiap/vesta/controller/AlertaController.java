package br.com.fiap.vesta.controller;

import br.com.fiap.vesta.dto.response.AlertaResponse;
import br.com.fiap.vesta.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@Tag(name = "Alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os alertas ativos")
    public ResponseEntity<CollectionModel<EntityModel<AlertaResponse>>> listarAtivos() {
        List<EntityModel<AlertaResponse>> modelos = alertaService.listarAtivos()
            .stream()
            .map(a -> EntityModel.of(a,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).resolver(a.idAlerta())).withRel("resolver")))
            .toList();
        return ResponseEntity.ok(CollectionModel.of(modelos,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).listarAtivos()).withSelfRel()));
    }

    @GetMapping("/abrigo/{idAbrigo}")
    @Operation(summary = "Listar alertas ativos de um abrigo")
    public ResponseEntity<CollectionModel<EntityModel<AlertaResponse>>> listarPorAbrigo(@PathVariable Long idAbrigo) {
        List<EntityModel<AlertaResponse>> modelos = alertaService.listarPorAbrigo(idAbrigo)
            .stream()
            .map(a -> EntityModel.of(a,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).resolver(a.idAlerta())).withRel("resolver")))
            .toList();
        return ResponseEntity.ok(CollectionModel.of(modelos,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).listarPorAbrigo(idAbrigo)).withSelfRel()));
    }

    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Marcar alerta como resolvido")
    public ResponseEntity<EntityModel<AlertaResponse>> resolver(@PathVariable Long id) {
        AlertaResponse resp = alertaService.resolver(id);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).resolver(id)).withSelfRel(),
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AlertaController.class).listarAtivos()).withRel("alertas")));
    }
}
