package br.com.fiap.vesta.config;

import br.com.fiap.vesta.annotation.EndpointPublico;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RoleDocumentationCustomizer implements OperationCustomizer {

    private static final Pattern ROLE_PATTERN = Pattern.compile("'([A-Z]+)'");

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        EndpointPublico publico = handlerMethod.getMethodAnnotation(EndpointPublico.class);
        if (publico == null) {
            publico = handlerMethod.getBeanType().getAnnotation(EndpointPublico.class);
        }

        String rolesLine;

        if (publico != null) {
            rolesLine = "**Acesso:** Público (sem autenticação)";
        } else {
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
            }

            if (preAuthorize != null) {
                List<String> roles = extractRoles(preAuthorize.value());
                rolesLine = "**Perfis:** " + String.join(", ", roles);
            } else {
                rolesLine = "**Perfis:** Qualquer autenticado (ADMIN, GESTOR ou OPERADOR)";
            }
        }

        String existing = operation.getDescription();
        operation.setDescription(
            (existing != null && !existing.isBlank() ? existing + "\n\n" : "") + rolesLine
        );

        return operation;
    }

    private List<String> extractRoles(String expression) {
        List<String> roles = new ArrayList<>();
        Matcher matcher = ROLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            roles.add("`" + matcher.group(1) + "`");
        }
        return roles;
    }
}
