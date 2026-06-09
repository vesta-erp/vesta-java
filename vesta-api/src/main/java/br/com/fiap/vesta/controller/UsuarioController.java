package br.com.fiap.vesta.controller;

import br.com.fiap.vesta.dto.request.UsuarioRequest;
import br.com.fiap.vesta.dto.response.UsuarioResponse;
import br.com.fiap.vesta.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Cadastrar novo usuário")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request,
                                                  @AuthenticationPrincipal UserDetails currentUser) {
        UsuarioResponse resp = usuarioService.criar(request, currentUser);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(resp.idUsuario()).toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Listar usuários")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar usuário")
    public ResponseEntity<Void> desativar(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        usuarioService.desativar(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
