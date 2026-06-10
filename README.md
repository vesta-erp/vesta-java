# Vesta API — Java / Spring Boot

API REST principal da plataforma **Vesta**, sistema de gerenciamento de abrigos emergenciais para órgãos públicos (prefeituras, defesa civil, governos estaduais).

**FIAP — Global Solution 2026 — 2TDSA**

---

## Integrantes

| Nome | RM |
|---|---|
| Gabriel Cruz | 559613 |
| João Victor Madella | 561007 |
| Kauã Ferreira | 560992 |
| Nathália Mantovani | 99904 |
| Vinicius Bitú | 560227 |

---

## Tecnologias

| Camada | Stack |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Segurança | Spring Security 6 + JWT (jjwt 0.12.6) |
| Persistência | Spring Data JPA / Hibernate 6 + Oracle (ojdbc11 23.4) |
| Migrations | Flyway |
| Cache | Spring Cache + Caffeine |
| HATEOAS | Spring HATEOAS |
| Documentação | Springdoc OpenAPI 2.6.0 (Swagger UI) |
| Integração | Spring Cloud OpenFeign 2023.0.3 (serviço .NET) |
| IA | Spring AI 1.0.0 — Azure OpenAI (gpt-4o) |
| Testes | JUnit 5, Mockito, MockMvc (11 classes de teste) |

---

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Oracle Database (XEPDB1 local ou cloud)
- Variáveis de ambiente configuradas (ver abaixo)

---

## Variáveis de Ambiente

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_VESTA_URL` | JDBC URL do Oracle | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| `DB_VESTA_USER` | Usuário do banco | `vesta` |
| `DB_VESTA_PASSWORD` | Senha do banco | `vesta` |
| `Jwt__SecretKey` | Chave secreta JWT (mín. 256 bits) | valor padrão inseguro |
| `AZURE_OPENAI_API_KEY` | Chave da API Azure OpenAI | _(vazio — IA desativada)_ |
| `AZURE_OPENAI_ENDPOINT` | Endpoint do Azure OpenAI | _(vazio — IA desativada)_ |
| `AZURE_OPENAI_DEPLOYMENT` | Nome do deployment | `gpt-4o` |
| `DOTNET_URL` | URL do serviço .NET de criticidade | `http://localhost:5000` |

---

## Como Executar

```bash
# entrar no diretório
cd java/vesta-api

# compilar e rodar os testes
mvn verify

# subir a aplicação
mvn spring-boot:run \
  -DDB_VESTA_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 \
  -DDB_VESTA_USER=vesta \
  -DDB_VESTA_PASSWORD=vesta \
  -DJwt__SecretKey=sua-chave-secreta-256bits
```

- API disponível em: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Ambiente de Produção (Azure)

| | URL |
|---|---|
| **API** | `https://vesta-api-java-gwf7drgza3hjgfc6.brazilsouth-01.azurewebsites.net` |
| **Swagger UI** | `https://vesta-api-java-gwf7drgza3hjgfc6.brazilsouth-01.azurewebsites.net/swagger-ui.html` |

---

## Endpoints

Todos os endpoints (exceto `/api/auth/login`) exigem:

```
Authorization: Bearer <token>
```

### Autenticação

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Autenticar e obter token JWT | Público |

### Usuários

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/admin/usuarios` | Cadastrar usuário | ADMIN, GESTOR |
| `GET` | `/api/admin/usuarios` | Listar usuários | ADMIN, GESTOR |
| `GET` | `/api/admin/usuarios/{id}` | Buscar usuário por ID | ADMIN, GESTOR, OPERADOR |
| `PUT` | `/api/admin/usuarios/{id}` | Atualizar usuário | ADMIN, GESTOR, OPERADOR |
| `PATCH` | `/api/admin/usuarios/{id}/desativar` | Desativar usuário | ADMIN, GESTOR |

### Abrigos

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos?idRegiao=` | Listar abrigos (GESTOR filtrado por região automaticamente) | Todos |
| `GET` | `/api/abrigos/{id}` | Buscar abrigo por ID | Todos |
| `POST` | `/api/abrigos` | Criar abrigo | ADMIN, GESTOR |
| `PUT` | `/api/abrigos/{id}` | Atualizar abrigo | ADMIN, GESTOR |
| `PATCH` | `/api/abrigos/{id}/status` | Alterar status (`?status=ATIVO\|LOTADO\|INTERDITADO\|INATIVO`) | ADMIN, GESTOR |

### Famílias e Acolhimento

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos/{idAbrigo}/familias` | Listar famílias presentes no abrigo | Todos |
| `GET` | `/api/abrigos/{idAbrigo}/familias/{idFamilia}` | Buscar família por ID | Todos |
| `GET` | `/api/abrigos/{idAbrigo}/familias/{idFamilia}/pessoas` | Listar pessoas da família | Todos |
| `POST` | `/api/abrigos/{idAbrigo}/familias/acolhimento` | Registrar acolhimento (família + pessoas) | Todos |
| `POST` | `/api/abrigos/{idAbrigo}/familias/{idFamilia}/saida` | Registrar saída de família | Todos |

### Estoque

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos/{idAbrigo}/estoque` | Listar estoque do abrigo | Todos |
| `GET` | `/api/abrigos/{idAbrigo}/estoque/criticos` | Listar itens abaixo do mínimo | Todos |
| `POST` | `/api/abrigos/{idAbrigo}/estoque/movimentacao` | Registrar entrada, saída ou ajuste | Todos |
| `PATCH` | `/api/abrigos/{idAbrigo}/estoque/minimo` | Definir quantidade mínima de um recurso | Todos |

### Ocorrências

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos/{idAbrigo}/ocorrencias` | Listar ocorrências do abrigo | Todos |
| `GET` | `/api/abrigos/{idAbrigo}/ocorrencias/{id}` | Buscar ocorrência por ID | Todos |
| `POST` | `/api/abrigos/{idAbrigo}/ocorrencias` | Registrar ocorrência | Todos |
| `PATCH` | `/api/abrigos/{idAbrigo}/ocorrencias/{id}/status` | Atualizar status (`?status=ABERTA\|EM_ANDAMENTO\|RESOLVIDA`) | Todos |

### Solicitações de Recursos

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/abrigos/{idAbrigo}/solicitacoes` | Listar solicitações do abrigo | Todos |
| `GET` | `/api/solicitacoes` | Listar todas as solicitações abertas | Todos |
| `GET` | `/api/solicitacoes/{id}` | Buscar solicitação por ID | Todos |
| `POST` | `/api/abrigos/{idAbrigo}/solicitacoes` | Abrir solicitação de recurso | Todos |
| `PATCH` | `/api/solicitacoes/{id}/status` | Avançar status da solicitação (body) | ADMIN, GESTOR |

### Transferências

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/transferencias/abrigo/{idAbrigo}` | Listar transferências de um abrigo | Todos |
| `POST` | `/api/transferencias/abrigo/{idAbrigo}` | Solicitar transferência de família | Todos |
| `PATCH` | `/api/transferencias/{id}/aprovar` | Aprovar transferência pendente | ADMIN, GESTOR |
| `PATCH` | `/api/transferencias/{id}/concluir` | Concluir transferência (movimenta família e ajusta ocupação) | ADMIN, GESTOR |

### Alertas

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/alertas` | Listar todos os alertas ativos | Todos |
| `GET` | `/api/alertas/abrigo/{idAbrigo}` | Listar alertas de um abrigo | Todos |
| `PATCH` | `/api/alertas/{id}/resolver` | Resolver alerta manualmente | ADMIN, GESTOR |

### Indicadores e Criticidade

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/indicadores/ranking` | Ranking de abrigos por criticidade (maior primeiro) | ADMIN, GESTOR |
| `GET` | `/api/indicadores/abrigo/{id}` | Indicador de criticidade de um abrigo | ADMIN, GESTOR |

### Assistente Operacional IA

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/assistente` | Fazer pergunta ao assistente IA | ADMIN, GESTOR |
| `GET` | `/api/assistente/health` | Verificar disponibilidade do assistente | ADMIN, GESTOR |

---

## Regras de Negócio Implementadas

### Capacidade e Acolhimento
- Abrigo `INTERDITADO` ou `INATIVO` bloqueia qualquer acolhimento
- Acolhimento bloqueado quando `qtOcupacaoAtual >= qtCapacidadeMaxima`
- Ao atingir capacidade máxima → status `LOTADO` + alerta `LOTACAO` (deduplicado)
- Ao registrar saída e liberar vagas → status volta para `ATIVO` + alerta `LOTACAO` resolvido automaticamente

### Estoque e Recursos
- Movimentação com `qtAtual < qtMinima` → gera alerta `ESTOQUE_CRITICO` com deduplicação por `(abrigo, recurso)`
- Normalização do estoque → alerta `ESTOQUE_CRITICO` resolvido automaticamente
- `qtMinima = 0` nunca gera alerta

### Alertas Automáticos
| Tipo | Gerado quando | Resolvido quando |
|---|---|---|
| `ESTOQUE_CRITICO` | `qtAtual < qtMinima` após movimentação | `qtAtual >= qtMinima` (automático) |
| `LOTACAO` | Ocupação atinge capacidade (acolhimento ou transferência) | Ocupação cai abaixo da capacidade (automático) |
| `OCORRENCIA_CRITICA` | Ocorrência com severidade `CRITICA` é registrada | Manual (`PATCH /alertas/{id}/resolver`) |

### Transferências
- Destino não pode ser `INTERDITADO` ou `INATIVO`
- Destino deve ter vagas no momento da aprovação
- Ao concluir: ocupação de origem e destino ajustada; status dos dois abrigos recalculado; alerta `LOTACAO` gerado se destino ficar cheio

### Solicitações
- Fluxo de status: `ABERTA` → `EM_ANALISE` → `EM_ATENDIMENTO` → `CONCLUIDA`
- `CANCELADA` é estado terminal válido
- Retrocesso de status não é permitido

### Isolamento de Acesso
- **OPERADOR**: acessa apenas o abrigo ao qual está vinculado (`IsolamentoService.verificarAcessoAbrigo()`)
- **GESTOR**: acessa apenas abrigos da sua região (`IsolamentoService.verificarAcessoRegiao()`); listagem de abrigos filtrada automaticamente por região
- **ADMIN**: acesso irrestrito

### Indicadores de Criticidade
Score local calculado por `IndicadorService` como média de três componentes (0–100 cada):
- Taxa de ocupação
- Percentual de itens abaixo do mínimo
- Score de ocorrências abertas (5+ = 100)

Quando o serviço .NET está disponível, o score e a classificação do .NET substituem o cálculo local (ver Integração com .NET).

---

## Operações Não-CRUD

| Operação | Endpoint | Descrição |
|---|---|---|
| `registrarAcolhimento` | `POST /api/abrigos/{id}/familias/acolhimento` | Salva família + pessoas + atualiza ocupação + gera alertas |
| `registrarSaida` | `POST /api/abrigos/{id}/familias/{id}/saida` | Saída + decrementa ocupação + resolve alerta LOTACAO |
| `concluirTransferencia` | `PATCH /api/transferencias/{id}/concluir` | Move família entre abrigos + ajusta ocupação + alertas |
| `abrirSolicitacao` | `POST /api/abrigos/{id}/solicitacoes` | Inicia workflow com status ABERTA |
| `atualizarStatusSolicitacao` | `PATCH /api/solicitacoes/{id}/status` | Transição controlada de estados |
| `rankingCriticidade` | `GET /api/indicadores/ranking` | Score composto local + enriquecimento .NET, ordenado |
| `resumoOperacional` | `POST /api/assistente` | Resposta em linguagem natural via Azure OpenAI (gpt-4o) |

---

## Estrutura do Projeto

```
src/main/java/br/com/fiap/vesta/
├── config/           # SecurityConfig, CacheConfig, CorsConfig, OpenApiConfig, FeignConfig, SpringAiConfig
├── security/         # JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
├── annotation/       # @EndpointPublico (marca rotas públicas)
├── domain/
│   ├── entity/       # 14 entidades JPA (Abrigo, Familia, PessoaAbrigada, Recurso, EstoqueAbrigo, ...)
│   └── enums/        # 10 enums (StatusAbrigo, StatusSolicitacao, TipoAlerta, ...)
├── repository/       # 14 interfaces Spring Data JPA
├── dto/
│   ├── request/      # DTOs de entrada com Bean Validation
│   └── response/     # DTOs de saída (Java records)
├── service/          # Lógica de negócio (13 services, incluindo IsolamentoService)
├── controller/       # 11 controllers REST com HATEOAS (EntityModel / CollectionModel)
├── client/           # CriticidadeClient (Feign) + CriticidadeClientFallback
└── exception/        # GlobalExceptionHandler (@RestControllerAdvice) + exceções customizadas

src/main/resources/
├── application.yml
├── application-test.yml
└── db/migration/
    ├── V6__create_tables.sql
    ├── V7__seed_data.sql
    ├── V8__fix_password_hashes.sql
    ├── V9__alerta_add_id_recurso.sql
    ├── V10__usuario_add_cpf_telefone.sql
    ├── V11__usuario_add_id_regiao.sql
    └── V12__fix_invalid_password_hashes.sql
```

---

## Testes

```bash
# rodar todos os testes
mvn test

# rodar um módulo específico
mvn test -Dtest=FamiliaServiceTest
```

11 classes de teste com Mockito (sem banco de dados real). O perfil `test` é ativado automaticamente via `application-test.yml`, desabilitando o Flyway e configurando JWT para testes.

| Classe | Cobertura |
|---|---|
| `AbrigoServiceTest` | CRUD, status, alertas de lotação |
| `FamiliaServiceTest` | Acolhimento, saída, validação de capacidade, alertas |
| `EstoqueServiceTest` | Movimentação, alerta ESTOQUE_CRITICO, resolução automática |
| `OcorrenciaServiceTest` | Registro, status, alerta OCORRENCIA_CRITICA |
| `SolicitacaoServiceTest` | Workflow de status, transições inválidas |
| `TransferenciaServiceTest` | Aprovação, conclusão, validação de capacidade, alertas |
| `AlertaService` | (coberto via services acima) |
| `IsolamentoServiceTest` | Isolamento por abrigo (OPERADOR) e por região (GESTOR) |
| `IndicadorServiceTest` | Score composto, enriquecimento .NET, fallback |
| `UsuarioServiceTest` | Cadastro, atualização, desativação |
| `AuthControllerTest` | Login, token JWT, MockMvc |
| `JwtTokenProviderTest` | Geração e validação de tokens |

---

## Integração com o Serviço .NET

A API consome o serviço .NET de criticidade via **Spring Cloud OpenFeign**.

- **Client**: `CriticidadeClient` — chama `GET /api/criticidade/abrigos` e `GET /api/criticidade/abrigos/{id}`
- **Fallback**: `CriticidadeClientFallback` — retorna `"INDISPONIVEL"` quando o .NET está offline; a API Java opera de forma autônoma
- **Configuração**: variável `DOTNET_URL` (padrão: `http://localhost:5000`)
- **Resposta enriquecida**: `IndicadorAbrigoResponse` contém campos locais (`nivelCriticidade`, `descricaoCriticidade`) e campos do .NET (`scoreNet`, `nivelNet`, `justificativa`, `recomendacoes`)

O .NET, por sua vez, busca dados de abrigos na Java API via HTTP para calcular seu próprio score de criticidade.

---

## Assistente Operacional IA

Powered by **Azure OpenAI (gpt-4o)** via Spring AI. O assistente recebe uma pergunta em linguagem natural e responde com base no contexto operacional atual dos abrigos (ocupação, estoque crítico, ocorrências abertas, alertas ativos).

Quando as variáveis `AZURE_OPENAI_*` não estão configuradas, o endpoint retorna uma resposta de indisponibilidade sem lançar erro.
