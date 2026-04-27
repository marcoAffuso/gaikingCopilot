package it.nttdata.gaikingCopilot.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import com.github.copilot.sdk.json.ModelInfo;

import it.nttdata.gaikingCopilot.model.ModelCopilot;
import it.nttdata.gaikingCopilot.service.copilot.CopilotService;
import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;

import org.springframework.web.bind.annotation.RequestParam;



@Log4j2
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/getCopilotModels")
    public ResponseEntity<List<ModelCopilot>> getCopilotModels(WebSession session) throws InterruptedException, ExecutionException {
        log.info("Handling Copilot models request. sessionId={}", sessionId(session));

        if (session == null || session.isExpired()) {
            log.info("Rejecting Copilot models request because session is missing or expired. sessionId={}", sessionId(session));
            return ResponseEntity.status(401).build();
        }

        String githubToken = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (githubToken == null || githubToken.isBlank()) {
            log.info("Rejecting Copilot models request because GitHub token is missing in session. sessionId={}", session.getId());
            return ResponseEntity.status(401).build();
        }

        List<ModelInfo> models = copilotService.getCopilotModel(githubToken);
        List<ModelCopilot> response = models.stream()
            .map(model -> new ModelCopilot(
                model.getId(),
                model.getName(),
                model.getSupportedReasoningEfforts(),
                model.getCapabilities() != null
                    && model.getCapabilities().getSupports() != null
                    && model.getCapabilities().getSupports().isReasoningEffort()
            ))
                .toList();

                log.info("Returning Copilot models response. sessionId={}, modelCount={}", session.getId(), response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getTestCopilot")
    public String getTestCopilot(
        @RequestParam String modelName, 
	    @RequestParam(defaultValue = "NA") String reasoningEffort,
        @RequestParam String prompt, 
        @RequestParam String streaming,
        WebSession session
    ) throws InterruptedException, ExecutionException{
        log.info(
                "Handling Copilot test request. sessionId={}, modelName={}, reasoningEffort={}, streaming={}, promptLength={}",
                sessionId(session),
                modelName,
                reasoningEffort,
                streaming,
                prompt != null ? prompt.length() : 0
        );

        String githubToken = gitHubTokenSessionService.getRequiredAccessToken(session);

        String response = switch (streaming.toLowerCase()) {
            case "true" -> copilotService.getResponseCopilotWithStreaming(githubToken, modelName, reasoningEffort, prompt);
            case "false" -> copilotService.getResponseCopilotWhitOutStreaming(githubToken, modelName, reasoningEffort, prompt);
            default -> throw new IllegalArgumentException("Invalid value for 'streaming' parameter. Expected 'true' or 'false'.");
        };

        log.info("Returning Copilot test response. sessionId={}, modelName={}, responseLength={}", sessionId(session), modelName, response.length());

        return response;
    }

    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }


}
