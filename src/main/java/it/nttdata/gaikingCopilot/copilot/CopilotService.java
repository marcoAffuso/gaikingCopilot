package it.nttdata.gaikingCopilot.copilot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.events.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionErrorEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class CopilotService {

    public List<ModelInfo> getCopilotModel() throws InterruptedException, ExecutionException {
        log.info("getCopilotModel");
        try (CopilotClient client = new CopilotClient()) {
            log.info("CopilotClient created successfully.");
            log.info("Retrieving Copilot models...");
            List<ModelInfo> models = client.listModels().get();
            log.info("Retrieved {} models.", models.size());
            return models;
        }
    }

    public String getResponseCopilotWhitOutStreaming(String model, String prompt) throws InterruptedException, ExecutionException{
        log.info("getResponseCopilotWhitOutStreaming");

        try (CopilotClient client = new CopilotClient()) {
            client.start().get();
            log.info("CopilotClient started successfully.");

            try (CopilotSession session = client.createSession(
                    new SessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setModel(model)
                            .setStreaming(false)
                            .setReasoningEffort("high")
            ).get()) {

                var responseEvent = session.sendAndWait(
                        new MessageOptions().setPrompt(prompt)
                ).get();

                String response = responseEvent != null
                        ? responseEvent.getData().content()
                        : null;

                log.info("Received response: {}", response);
                return response;
            }
        }
    }

    public String getResponseCopilotWithStreaming(String model, String prompt) throws InterruptedException, ExecutionException {
        log.info("getResponseCopilotWithStreaming");

        try (CopilotClient client = new CopilotClient()) {
            client.start().get();
            log.info("CopilotClient started successfully.");

            try (CopilotSession copilotSession = client.createSession(
                    new SessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setModel(model)
                            .setStreaming(true)
                            .setReasoningEffort("high")
            ).get()) {

                CompletableFuture<Void> done = new CompletableFuture<>();
                CompletableFuture<String> errorFuture = new CompletableFuture<>();
                StringBuilder responseBuilder = new StringBuilder();

                copilotSession.on(AssistantMessageDeltaEvent.class, delta -> {
                    String chunk = delta.getData().deltaContent();
                    if (chunk != null) {
                        responseBuilder.append(chunk);
                        log.info("Chunk: {}", chunk);
                    }
                });

                copilotSession.on(AssistantMessageEvent.class, msg -> {
                    String content = msg.getData().content();
                    log.info("Final response event: {}", content);

                    // Fallback: se per qualche motivo i delta non sono arrivati,
                    // uso il contenuto completo del messaggio finale.
                    if (responseBuilder.isEmpty() && content != null) {
                        responseBuilder.append(content);
                    }
                });

                copilotSession.on(SessionErrorEvent.class, err -> {
                    String errorMessage = err.getData().message();
                    log.error("Error: {}", errorMessage);
                    errorFuture.complete(errorMessage);
                    done.completeExceptionally(new RuntimeException(errorMessage));
                });

                copilotSession.on(SessionIdleEvent.class, idle -> done.complete(null));


                String messageId = copilotSession.send(
                        new MessageOptions().setPrompt(prompt)
                ).get();

                log.info("Message sent. ID: {}", messageId);

                done.get();

                if (errorFuture.isDone()) {
                    throw new ExecutionException(new RuntimeException(errorFuture.get()));
                }

                String response = responseBuilder.toString();
                log.info("Received response: {}", response);
                return response;
            }
        }
    }

}
