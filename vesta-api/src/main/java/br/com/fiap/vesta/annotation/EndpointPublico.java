package br.com.fiap.vesta.annotation;

import java.lang.annotation.*;

/**
 * Marca endpoints que não exigem autenticação (permitAll no SecurityConfig).
 * Usado pelo RoleDocumentationCustomizer para exibir "Público" no Swagger.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EndpointPublico {
}
