package br.com.fiap.vesta.controller;

import br.com.fiap.vesta.dto.request.TransferenciaRequest;
import br.com.fiap.vesta.dto.response.TransferenciaResponse;
import br.com.fiap.vesta.repository.UsuarioRepository;
import br.com.fiap.vesta.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transferencias")
@Tag(name = "Transferências")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;
    private final UsuarioRepository usuarioRepository;

    public TransferenciaController(TransferenciaService transferenciaService,
                                    UsuarioRepository usuarioRepository) {
        this.transferenciaService = transferenciaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/abrigo/{idAbrigo}")
    @Operation(summary = "Listar transferências de um abrigo (origem ou destino)")
    public ResponseEntity<CollectionModel<EntityModel<TransferenciaResponse>>> listar(@PathVariable Long idAbrigo) {
        List<EntityModel<TransferenciaResponse>> modelos = transferenciaService.listarPorAbrigo(idAbrigo)
            .stream()
            .map(t -> EntityModel.of(t,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).aprovar(t.idTransferencia())).withRel("aprovar"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).concluir(t.idTransferencia())).withRel("concluir")))
            .toList();
        return ResponseEntity.ok(CollectionModel.of(modelos,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).listar(idAbrigo)).withSelfRel()));
    }

    @PostMapping("/abrigo/{idAbrigo}")
    @Operation(summary = "Solicitar transferência de família")
    public ResponseEntity<EntityModel<TransferenciaResponse>> solicitar(@PathVariable Long idAbrigo,
                                                                        @Valid @RequestBody TransferenciaRequest req,
                                                                        @AuthenticationPrincipal UserDetails user) {
        Long idUsuario = usuarioRepository.findByDsEmail(user.getUsername())
            .map(u -> u.getIdUsuario()).orElseThrow();
        TransferenciaResponse resp = transferenciaService.solicitar(idAbrigo, idUsuario, req);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).listar(idAbrigo)).withRel("transferencias"),
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).aprovar(resp.idTransferencia())).withRel("aprovar")));
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Aprovar transferência pendente")
    public ResponseEntity<EntityModel<TransferenciaResponse>> aprovar(@PathVariable Long id) {
        TransferenciaResponse resp = transferenciaService.aprovar(id);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).aprovar(id)).withSelfRel(),
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).concluir(id)).withRel("concluir")));
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Concluir transferência aprovada (movimenta família e ajusta ocupação)")
    public ResponseEntity<EntityModel<TransferenciaResponse>> concluir(@PathVariable Long id) {
        TransferenciaResponse resp = transferenciaService.concluir(id);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TransferenciaController.class).concluir(id)).withSelfRel()));
    }
}
