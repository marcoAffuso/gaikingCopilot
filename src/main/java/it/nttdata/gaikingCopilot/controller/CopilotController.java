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
import it.nttdata.gaikingCopilot.copilot.CopilotService;
import it.nttdata.gaikingCopilot.copilot.GitHubTokenSessionService;
import it.nttdata.gaikingCopilot.model.ModelCopilot;
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
        if (session == null || session.isExpired()) {
            return ResponseEntity.status(401).build();
        }

        String githubToken = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (githubToken == null || githubToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        List<ModelInfo> models = copilotService.getCopilotModel(githubToken);
        log.info("models {} ", models);
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
        log.info("Received request with param: {}", modelName);

        String githubToken = gitHubTokenSessionService.getRequiredAccessToken(session);

        return switch (streaming.toLowerCase()) {
            case "true" -> copilotService.getResponseCopilotWithStreaming(githubToken, modelName, reasoningEffort, prompt);
            case "false" -> copilotService.getResponseCopilotWhitOutStreaming(githubToken, modelName, reasoningEffort, prompt);
            default -> throw new IllegalArgumentException("Invalid value for 'streaming' parameter. Expected 'true' or 'false'.");
        };
    }    

}
