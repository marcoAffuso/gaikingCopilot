package it.nttdata.gaikingCopilot.controller;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.ai.TestCaseCreationFromConfluenceService;
import it.nttdata.gaikingCopilot.model.ConfluenceRequestParameters;
import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;


@Log4j2
@RestController
@Validated
@RequestMapping("/")
@RequiredArgsConstructor
public class TestCaseGeneratorController {

    private static final String MESSAGE_KEY = "message";
    private static final String TEST_CASE_PATH_CONFLUENCE = "confluence/testsCases";

    public final GitHubTokenSessionService gitHubTokenSessionService;
    public final TestCaseCreationFromConfluenceService testCaseCreationFromConfluenceService;


    @GetMapping("/generateTestCase/Confluence")
    public Mono<Map<String, String>> generateTestCaseConfluence(
        @RequestParam @NotBlank String model,
	    @RequestParam @NotBlank String reasoningEffort,
        @RequestParam @NotBlank String confluenceUrl,
        @RequestParam @NotBlank String spaceKey,
        @RequestParam @NotBlank String pageTitle,
        @RequestParam(defaultValue = TEST_CASE_PATH_CONFLUENCE) String outputDir,
        WebSession session
    ) {
        
        log.info("Returning Confluence test case generation response. sessionId={}, projectPath={}", session.getId(), TEST_CASE_PATH_CONFLUENCE);

        if (session == null || session.isExpired()) {
            log.info("Rejecting Generate Test Case from Confluence request because session is missing or expired. sessionId={}", sessionId(session));
            return Mono.just(Map.of(MESSAGE_KEY, "Sessione scaduta. Effettua il login."));
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Rejecting Generate Test Case from Confluence request because GitHub token is missing in session. sessionId={}", session.getId());
            return Mono.just(Map.of(MESSAGE_KEY, "Token GitHub non disponibile. Effettua il login."));
        }

        ConfluenceRequestParameters confluenceRequestParameters = new ConfluenceRequestParameters(
            model,
            reasoningEffort,
            confluenceUrl,
            spaceKey,
            pageTitle,
            outputDir
        );

        return this.testCaseCreationFromConfluenceService
            .fromConflunceToStepByStepTestCase(confluenceRequestParameters, token)
            .thenReturn(Map.of(
                MESSAGE_KEY, "Test Case from Confluence generated successfully.",
                "ConfluenceTestCasePath", TEST_CASE_PATH_CONFLUENCE
            ));
    }


    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }

}
