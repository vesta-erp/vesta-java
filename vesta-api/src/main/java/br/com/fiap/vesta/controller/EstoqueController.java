package br.com.fiap.vesta.controller;

import br.com.fiap.vesta.dto.request.EstoqueMinRequest;
import br.com.fiap.vesta.dto.request.MovimentacaoRequest;
import br.com.fiap.vesta.dto.response.EstoqueResponse;
import br.com.fiap.vesta.dto.response.MovimentacaoResponse;
import br.com.fiap.vesta.repository.UsuarioRepository;
import br.com.fiap.vesta.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/abrigos/{idAbrigo}/estoque")
@Tag(name = "Estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final UsuarioRepository usuarioRepository;

    public EstoqueController(EstoqueService estoqueService,
                              UsuarioRepository usuarioRepository) {
        this.estoqueService = estoqueService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Operation(summary = "Listar estoque do abrigo")
    public ResponseEntity<CollectionModel<EntityModel<EstoqueResponse>>> listar(@PathVariable Long idAbrigo) {
        List<EntityModel<EstoqueResponse>> modelos = estoqueService.listarPorAbrigo(idAbrigo)
            .stream()
            .map(e -> EntityModel.of(e,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).listar(idAbrigo)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).criticos(idAbrigo)).withRel("criticos")))
            .toList();
        return ResponseEntity.ok(CollectionModel.of(modelos,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).listar(idAbrigo)).withSelfRel()));
    }

    @GetMapping("/criticos")
    @Operation(summary = "Listar itens abaixo do estoque mínimo")
    public ResponseEntity<CollectionModel<EntityModel<EstoqueResponse>>> criticos(@PathVariable Long idAbrigo) {
        List<EntityModel<EstoqueResponse>> modelos = estoqueService.listarAbaixoMinimo(idAbrigo)
            .stream()
            .map(e -> EntityModel.of(e,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).listar(idAbrigo)).withRel("estoque")))
            .toList();
        return ResponseEntity.ok(CollectionModel.of(modelos,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).criticos(idAbrigo)).withSelfRel()));
    }

    @PostMapping("/movimentacao")
    @Operation(summary = "Registrar entrada, saída ou ajuste de estoque")
    public ResponseEntity<EntityModel<MovimentacaoResponse>> movimentar(@PathVariable Long idAbrigo,
                                                                        @Valid @RequestBody MovimentacaoRequest request,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        Long idUsuario = usuarioRepository.findByDsEmail(userDetails.getUsername())
            .map(u -> u.getIdUsuario())
            .orElseThrow();
        MovimentacaoResponse resp = estoqueService.registrarMovimentacao(idAbrigo, idUsuario, request);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).listar(idAbrigo)).withRel("estoque")));
    }

    @PatchMapping("/minimo")
    @Operation(summary = "Definir quantidade mínima de um recurso")
    public ResponseEntity<EntityModel<EstoqueResponse>> definirMinimo(@PathVariable Long idAbrigo,
                                                                      @Valid @RequestBody EstoqueMinRequest request) {
        EstoqueResponse resp = estoqueService.definirMinimo(idAbrigo, request);
        return ResponseEntity.ok(EntityModel.of(resp,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EstoqueController.class).listar(idAbrigo)).withRel("estoque")));
    }
}
