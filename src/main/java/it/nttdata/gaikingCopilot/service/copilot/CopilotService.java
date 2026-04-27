package it.nttdata.gaikingCopilot.service.copilot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.events.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionErrorEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

import it.nttdata.gaikingCopilot.exception.CustomException;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class CopilotService {




    public List<ModelInfo> getCopilotModel(String githubToken) throws InterruptedException {
        log.info("getCopilotModel");

        try (CopilotClient client = createClient(githubToken)) {
            log.info("CopilotClient created successfully.");
            log.info("Retrieving Copilot models...");

            List<ModelInfo> models = client.listModels().get();

            log.info("Retrieved {} models.", models.size());
            return models;

        } catch (ExecutionException e) {
            Throwable cause = rootCause(e);

            log.error("Failed to retrieve Copilot models. causeType={}, cause={}",
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause);

            throw buildCopilotException("Errore durante il recupero dei modelli Copilot", cause);
        }
    }

    public String getResponseCopilotWhitOutStreaming(String githubToken, String model, String reasoningEffort, String prompt) throws InterruptedException {
        log.info("getResponseCopilotWhitOutStreaming");

        validateGitHubToken(githubToken);
        validateInput(model, prompt);

        try (CopilotClient client = createClient(githubToken)) {
            client.start().get();
                log.info("CopilotClient started successfully. model={}, reasoningEffort={}, promptLength={}", model, reasoningEffort, prompt.length());

                SessionConfig sessionConfig = new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel(model)
                    .setStreaming(false);

                if (reasoningEffort != null && !reasoningEffort.equalsIgnoreCase("NA")) {
                    sessionConfig.setReasoningEffort(reasoningEffort);
                }

                try (CopilotSession session = client.createSession(sessionConfig).get()) {

                    var responseEvent = session.sendAndWait(
                            new MessageOptions().setPrompt(prompt),
                            5 * 60 * 1000L
                    ).get();

                    String response = responseEvent != null
                            ? responseEvent.getData().content()
                            : null;

                    log.info("Received non-streaming response. responseLength={}",
                            response != null ? response.length() : 0);

                    return response;
                }

        } catch (ExecutionException e) {
            Throwable cause = rootCause(e);

            log.error("Copilot non-streaming request failed. model={}, promptLength={}, causeType={}, cause={}",
                    model,
                    prompt != null ? prompt.length() : 0,
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause);

            throw buildCopilotException("Errore durante la chiamata non-streaming a Copilot", cause);
        }
    }

    public String getResponseCopilotWithStreaming(String githubToken, String model, String reasoningEffort, String prompt) throws InterruptedException {
        log.info("getResponseCopilotWithStreaming");

        validateGitHubToken(githubToken);
        validateInput(model, prompt);
       

        try (CopilotClient client = createClient(githubToken)) {
            client.start().get();
            log.info("CopilotClient started successfully. model={}, reasoningEffort={}, promptLength={}", model, reasoningEffort, prompt.length());

            SessionConfig sessionConfig = new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel(model)
                    .setStreaming(true);

            if (reasoningEffort != null && !reasoningEffort.equalsIgnoreCase("NA")) {
                sessionConfig.setReasoningEffort(reasoningEffort);
            }

            try (CopilotSession copilotSession = client.createSession(sessionConfig).get()) {

                CompletableFuture<Void> done = new CompletableFuture<>();
                CompletableFuture<String> errorFuture = new CompletableFuture<>();
                StringBuilder responseBuilder = new StringBuilder();

                copilotSession.on(AssistantMessageDeltaEvent.class, delta -> {
                    String chunk = delta.getData().deltaContent();
                    if (chunk != null) {
                        responseBuilder.append(chunk);
                        log.debug("Received chunk. chunkLength={}", chunk.length());
                    }
                });

                copilotSession.on(AssistantMessageEvent.class, msg -> {
                    String content = msg.getData().content();
                    log.info("Final response event received. contentLength={}",
                            content != null ? content.length() : 0);

                    if (responseBuilder.isEmpty() && content != null) {
                        responseBuilder.append(content);
                    }
                });

                copilotSession.on(SessionErrorEvent.class, err -> {
                    String errorMessage = err.getData().message();
                    log.error("Streaming session error: {}", errorMessage);

                    errorFuture.complete(errorMessage);
                    done.completeExceptionally(new RuntimeException(errorMessage));
                });

                copilotSession.on(SessionIdleEvent.class, idle -> {
                    log.info("Streaming session became idle.");
                    done.complete(null);
                });

                String messageId = copilotSession.send(
                        new MessageOptions().setPrompt(prompt)
                ).get();

                log.info("Message sent. ID={}", messageId);

                done.get();

                if (errorFuture.isDone()) {
                    String errorMessage = errorFuture.get();
                    throw new CustomException(
                            "Errore durante la sessione streaming verso Copilot",
                            500,
                            new RuntimeException(errorMessage)
                    );
                }

                String response = responseBuilder.toString();

                log.info("Streaming response completed. responseLength={}", response.length());

                return response;
            }

        } catch (ExecutionException e) {
            Throwable cause = rootCause(e);

            log.error("Copilot streaming request failed. model={}, promptLength={}, causeType={}, cause={}",
                    model,
                    prompt != null ? prompt.length() : 0,
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause);

            throw buildCopilotException("Errore durante la chiamata streaming a Copilot", cause);
        }
    }

    private CopilotClient createClient(String githubToken) {
        CopilotClientOptions options = new CopilotClientOptions()
                .setGitHubToken(githubToken)
                .setUseLoggedInUser(false);

        return new CopilotClient(options);
    }

    private void validateGitHubToken(String githubToken) {
        if (githubToken == null || githubToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token obbligatorio.");
        }
    }

    private void validateInput(String model, String prompt) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Il parametro 'model' è obbligatorio.");
        }

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Il parametro 'prompt' è obbligatorio.");
        }
    }

    private Throwable rootCause(Throwable ex) {
        Throwable result = ex;

        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }

        log.debug("Root cause identified. causeType={}, causeMessage={}",
                result.getClass().getName(),
                result.getMessage());

        return result;
    }

    private CustomException buildCopilotException(String defaultMessage, Throwable cause) {
        String causeMessage = cause.getMessage() != null ? cause.getMessage() : "Errore sconosciuto";
        log.error("{}: causeType={}, causeMessage={}", defaultMessage, cause.getClass().getName(), causeMessage, cause);

        if (cause instanceof TimeoutException) {
            return new CustomException(
                    "Timeout della richiesta verso Copilot",
                    504,
                    cause
            );
        }

        if (causeMessage.contains("422")) {
            return new CustomException(
                    "Richiesta non valida verso Copilot: controlla model, prompt o payload",
                    422,
                    cause
            );
        }

        if (causeMessage.contains("401")) {
            return new CustomException(
                    "Non autorizzato verso Copilot",
                    401,
                    cause
            );
        }

        if (causeMessage.contains("403")) {
            return new CustomException(
                    "Accesso negato verso Copilot",
                    403,
                    cause
            );
        }

        if (causeMessage.contains("404")) {
            return new CustomException(
                    "Risorsa Copilot non trovata",
                    404,
                    cause
            );
        }

        if (causeMessage.contains("429")) {
            return new CustomException(
                    "Troppe richieste verso Copilot",
                    429,
                    cause
            );
        }

        return new CustomException(defaultMessage + ": " + causeMessage, 500, cause);
    }

}
