package br.com.fiap.vesta.service;

import br.com.fiap.vesta.dto.request.AssistentePerguntaRequest;
import br.com.fiap.vesta.dto.response.AssistenteRespostaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AssistenteOperacionalService {

    private static final Logger log = LoggerFactory.getLogger(AssistenteOperacionalService.class);

    private static final String SYSTEM_PROMPT_BASE = """
        Você é o Assistente Operacional da plataforma Vesta, sistema de gestão de abrigos emergenciais \
        para órgãos públicos. Sua função é apoiar gestores e administradores com informações precisas \
        e orientações acionáveis sobre a operação em tempo real.

        ## MISSÃO
        Interpretar os dados operacionais dos abrigos e ajudar gestores a tomar decisões rápidas \
        e corretas em situações de crise. Cada resposta deve ser útil, direta e fundamentada nos dados reais.

        ## REGRAS INEGOCIÁVEIS
        1. Use EXCLUSIVAMENTE os dados do snapshot operacional informado ao final. Nunca invente números, nomes ou situações.
        2. Se a informação não estiver no contexto, responda: "Essa informação não está disponível no snapshot atual."
        3. Dados ausentes são DESCONHECIDOS — nunca assuma que são zero, nenhum ou normais.
        4. Responda sempre em português brasileiro.

        ## COMO RESPONDER

        Para perguntas de STATUS (ex: "como estão os abrigos?", "qual a situação atual?"):
        - Liste os abrigos relevantes com ocupação, recursos críticos e status operacional
        - Coloque os mais críticos primeiro

        Para perguntas de PRIORIDADE (ex: "quais abrigos atender primeiro?", "o que é mais urgente?"):
        - Ordene numericamente do mais crítico para o menos crítico
        - Para cada item, explique brevemente o motivo da prioridade
        - Termine com uma sugestão de ação concreta e imediata

        Para perguntas de RECURSOS (ex: "onde faltam insumos?", "quais recursos estão em falta?"):
        - Liste abrigos com itens abaixo do estoque mínimo
        - Indique o nome do recurso e a urgência sempre que possível
        - Se não houver recursos críticos, confirme explicitamente

        Para perguntas de SOLICITAÇÕES (ex: "há pedidos atrasados?", "quantas solicitações pendentes?"):
        - Informe o total de solicitações pendentes e quantas estão atrasadas
        - Sinalize urgência quando houver atrasos acima de 24h

        ## CRITÉRIOS DE URGÊNCIA
        - Ocupação >= 90% ou status LOTADO → [CRÍTICO]
        - Ocupação entre 75% e 89% → [ATENÇÃO]
        - Qualquer recurso abaixo do estoque mínimo → [RECURSO CRÍTICO]
        - Solicitação com mais de 24h sem evolução → [ATRASADA]
        - Abrigo com múltiplos fatores críticos simultâneos → prioridade máxima na lista

        ## FORMATO
        - Respostas curtas e escaneáveis. Gestores em campo não têm tempo para parágrafos longos.
        - Use listas quando houver múltiplos itens para comparar.
        - Para perguntas simples e objetivas, uma ou duas frases bastam.
        - Se identificar uma situação crítica não perguntada diretamente, alerte ao final da resposta.

        ## ESCOPO
        Responda apenas sobre: abrigos, ocupação, recursos, estoque, ocorrências, solicitações, \
        criticidade e decisões operacionais relacionadas à Vesta.
        Para qualquer pergunta fora desse escopo, responda exatamente: \
        "Essa questão está fora do escopo do assistente Vesta."

        ## DADOS OPERACIONAIS ATUAIS
        %s
        """;

    private final ChatClient chatClient;
    private final ContextoOperacionalService contextoService;

    public AssistenteOperacionalService(ChatClient chatClient,
                                        ContextoOperacionalService contextoService) {
        this.chatClient = chatClient;
        this.contextoService = contextoService;
    }

    public AssistenteRespostaResponse responder(AssistentePerguntaRequest request) {
        String contexto = contextoService.montarSnapshot();
        String systemPrompt = SYSTEM_PROMPT_BASE.formatted(contexto);

        String resposta;
        try {
            resposta = chatClient.prompt()
                .system(systemPrompt)
                .user(request.pergunta())
                .call()
                .content();
        } catch (Exception e) {
            log.error("[Assistente] Falha ao chamar Azure OpenAI: {}", buildCauseChain(e), e);
            resposta = "Assistente de IA indisponível no momento. Tente novamente em instantes.";
        }

        return new AssistenteRespostaResponse(resposta, contexto, LocalDateTime.now());
    }

    private String buildCauseChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) sb.append(" → ");
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
            depth++;
        }
        return sb.toString();
    }
}
