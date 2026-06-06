# Vesta API — Java / Spring Boot

API REST principal da plataforma **Vesta**, sistema de gerenciamento de abrigos de emergência para órgãos públicos (prefeituras, defesa civil, governos estaduais). Projeto acadêmico FIAP Global Solution 2026 — 2TDSA.

---

## Tecnologias

| Camada | Stack |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Segurança | Spring Security 6 + JWT (jjwt 0.12.6) |
| Persistência | Spring Data JPA / Hibernate 6 + Oracle (ojdbc11 23.4) |
| Cache | Spring Cache + Caffeine |
| Documentação | Springdoc OpenAPI 2.6.0 (Swagger UI) |
| Integração | Spring Cloud OpenFeign (serviço .NET) |
| IA | Spring AI 1.0.0 — Anthropic Claude |
| Testes | JUnit 5, Mockito, MockMvc, H2 (modo Oracle) |

---

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Oracle Database (XEPDB1 ou cloud)
- Variáveis de ambiente configuradas (ver seção abaixo)

---

## Variáveis de Ambiente

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `ORACLE_URL` | JDBC URL do Oracle | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| `ORACLE_USER` | Usuário do banco | `vesta` |
| `ORACLE_PASS` | Senha do banco | `vesta` |
| `JWT_SECRET` | Chave secreta JWT (mín. 256 bits) | valor padrão inseguro |
| `ANTHROPIC_API_KEY` | Chave da API Anthropic (resumo IA) | _(vazio — IA desativada)_ |
| `DOTNET_URL` | URL do serviço .NET de criticidade | `http://localhost:5000` |

---

## Como executar

```bash
# clonar e entrar no diretório
cd java/vesta-api

# compilar e rodar testes
mvn verify

# subir a aplicação
mvn spring-boot:run \
  -DORACLE_URL=jdbc:oracle:thin:@... \
  -DORACLE_USER=vesta \
  -DORACLE_PASS=vesta \
  -DJWT_SECRET=sua-chave-secreta-256bits
```

A API sobe em `http://localhost:8080`.  
Swagger UI disponível em `http://localhost:8080/swagger-ui.html`.

---

## Endpoints

Todos os endpoints (exceto `/api/auth/login`) exigem o header:

```
Authorization: Bearer <token>
```

### Autenticação

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/auth/login` | Obter token JWT | Público |

### Abrigos

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos` | Listar abrigos (filtro opcional `?idRegiao=`) | Todos |
| `GET` | `/api/abrigos/{id}` | Buscar abrigo por ID | Todos |
| `POST` | `/api/abrigos` | Criar abrigo | ADMIN, GESTOR |
| `PUT` | `/api/abrigos/{id}` | Atualizar abrigo | ADMIN, GESTOR |
| `PATCH` | `/api/abrigos/{id}/status` | Atualizar status (`?status=ATIVO\|LOTADO\|INTERDITADO\|INATIVO`) | ADMIN, GESTOR |

### Famílias e Acolhimento

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/familias` | Listar famílias (filtro `?idAbrigo=`) | Todos |
| `GET` | `/api/familias/{id}` | Buscar família | Todos |
| `POST` | `/api/familias/acolhimento` | Registrar acolhimento de família | ADMIN, GESTOR, OPERADOR |
| `POST` | `/api/familias/{id}/saida` | Registrar saída de família | ADMIN, GESTOR, OPERADOR |

### Estoque

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/estoques/abrigo/{idAbrigo}` | Listar estoque do abrigo | Todos |
| `POST` | `/api/estoques/movimentacao` | Registrar movimentação de recurso | ADMIN, GESTOR, OPERADOR |
| `PATCH` | `/api/estoques/{idEstoque}/minimo` | Atualizar quantidade mínima | ADMIN, GESTOR |

### Ocorrências

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/ocorrencias/abrigo/{idAbrigo}` | Listar ocorrências do abrigo | Todos |
| `POST` | `/api/ocorrencias` | Registrar ocorrência | ADMIN, GESTOR, OPERADOR |
| `PATCH` | `/api/ocorrencias/{id}/status` | Atualizar status da ocorrência | ADMIN, GESTOR, OPERADOR |

### Solicitações de Recursos

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/solicitacoes` | Listar solicitações (filtro `?idAbrigo=`) | Todos |
| `POST` | `/api/solicitacoes` | Abrir solicitação | ADMIN, GESTOR, OPERADOR |
| `PATCH` | `/api/solicitacoes/{id}/status` | Avançar status da solicitação | ADMIN, GESTOR |

### Transferências

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/transferencias` | Listar transferências | Todos |
| `POST` | `/api/transferencias` | Solicitar transferência de família | ADMIN, GESTOR |
| `PATCH` | `/api/transferencias/{id}/aprovar` | Aprovar e executar transferência | ADMIN, GESTOR |

### Alertas

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/alertas` | Listar alertas ativos | Todos |
| `GET` | `/api/alertas/abrigo/{idAbrigo}` | Alertas de um abrigo | Todos |
| `PATCH` | `/api/alertas/{id}/resolver` | Resolver alerta | ADMIN, GESTOR, OPERADOR |

### Indicadores e IA

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/indicadores/ranking` | Ranking de abrigos por criticidade | ADMIN, GESTOR |
| `GET` | `/api/indicadores/abrigo/{id}` | Indicadores de criticidade de um abrigo | Todos |
| `GET` | `/api/indicadores/abrigo/{id}/resumo` | Resumo operacional gerado por IA | Todos |

---

## Regras de Negócio

- Abrigo não aceita novos ocupantes quando está no status `LOTADO` ou `INTERDITADO`
- Status dos abrigos: `ATIVO` → `LOTADO` → `INTERDITADO` → `INATIVO`
- Estoque com quantidade abaixo do mínimo gera `Alerta` automaticamente
- Fluxo de solicitação: `ABERTA` → `EM_ANALISE` → `EM_ATENDIMENTO` → `CONCLUIDA`
- Transferência só é permitida se o abrigo destino possui vagas disponíveis
- Operadores só podem atualizar o abrigo ao qual estão vinculados
- Gestores só visualizam e operam dentro de sua região

---

## Estrutura do Projeto

```
src/main/java/br/com/fiap/vesta/
├── config/          # SecurityConfig, CacheConfig, CorsConfig, OpenApiConfig, FeignConfig
├── security/        # JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
├── domain/
│   ├── entity/      # 14 entidades JPA (Abrigo, Familia, Recurso, etc.)
│   └── enums/       # 10 enums do domínio
├── repository/      # 14 interfaces Spring Data JPA
├── dto/
│   ├── request/     # DTOs de entrada com validação Bean Validation
│   └── response/    # DTOs de saída (records)
├── service/         # Lógica de negócio
├── controller/      # Controllers REST com HATEOAS
├── client/          # Feign client para o serviço .NET
└── exception/       # GlobalExceptionHandler e exceções customizadas
```

---

## Testes

```bash
# rodar todos os testes
mvn test

# rodar apenas testes de um módulo
mvn test -Dtest=AbrigoServiceTest
```

Os testes usam H2 em memória (modo Oracle). O perfil `test` é ativado automaticamente via `application-test.yml`.

---

## Integração com o Serviço .NET

A API consome o serviço `.NET` de criticidade via OpenFeign. O cliente está em `CriticidadeClient` e usa fallback (`CriticidadeClientFallback`) quando o serviço estiver indisponível. Configure a URL via `DOTNET_URL`.

---

## Integrantes

| Nome | RM |
|---|---|
| Gabriel | _(preencher)_ |

**FIAP — 2TDSA — Global Solution 2026**
