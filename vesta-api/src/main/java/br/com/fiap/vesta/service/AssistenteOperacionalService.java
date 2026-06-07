package br.com.fiap.vesta.service;

import br.com.fiap.vesta.dto.request.AssistentePerguntaRequest;
import br.com.fiap.vesta.dto.response.AssistenteRespostaResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AssistenteOperacionalService {

    private static final String SYSTEM_PROMPT_BASE = """
        Você é o assistente operacional da plataforma Vesta,
        especializado em gestão de abrigos em situações de desastre.

        Responda sempre de forma objetiva, clara e em português.
        Baseie suas respostas SOMENTE nas informações do contexto abaixo.
        Se não houver dados suficientes, diga isso explicitamente.
        Nunca invente dados ou situações.

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
            resposta = "Assistente de IA indisponível no momento. " +
                "Configure as variáveis AZURE_OPENAI_API_KEY e AZURE_OPENAI_ENDPOINT para habilitar este recurso.\n\n" +
                "Contexto operacional coletado:\n" + contexto;
        }

        return new AssistenteRespostaResponse(resposta, contexto, LocalDateTime.now());
    }
}
