package it.nttdata.gaikingCopilot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.ai.GenerateTAMavenSeleniumCucumberJunit;
import it.nttdata.gaikingCopilot.copilot.GitHubTokenSessionService;
import it.nttdata.gaikingCopilot.model.AutomationProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;


@Log4j2
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class NewProjectController {

    private final GenerateTAMavenSeleniumCucumberJunit generateTAMavenSeleniumCucumberJunit;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/newProject/gradle_selenium_junit5_cucumber")
    public Map<String, String> gradleSeleniumJunit5Cucumber(
        @RequestParam String model, 
        @RequestParam String projectName,
        @RequestParam String groupId,
        @RequestParam String javaVersion,
        @RequestParam String seleniumVersion,
        @RequestParam String junitVersion,
        @RequestParam String junitPlatformVersion,
        @RequestParam String cucumberVersion,
        @RequestParam String webdrivermanagerVersion,
        @RequestParam String surefireVersion,
        @RequestParam String compilerPluginVersion,
        WebSession session
    ) {
        if (session == null || session.isExpired()) {
            return Map.of("message", "Sessione scaduta. Effettua il login.");
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Token GitHub non disponibile. Effettua il login.");
        }

        return Map.of("message", "Progetto generato con successo! Controlla i log per i dettagli.");
    }
    


    @GetMapping("/newProject/mvn_selenium_junit5_cucumber")
    public Map<String, String> mavenSeleniumJunit5Cucumber(
        @RequestParam String model, 
        @RequestParam String projectName,
        @RequestParam String groupId,
        @RequestParam String javaVersion,
        @RequestParam String seleniumVersion,
        @RequestParam String junitVersion,
        @RequestParam String junitPlatformVersion,
        @RequestParam String cucumberVersion,
        @RequestParam String webdrivermanagerVersion,
        @RequestParam String surefireVersion,
        @RequestParam String compilerPluginVersion,
        WebSession session
    ) throws InterruptedException, ExecutionException {

        AutomationProjectRequest request = new AutomationProjectRequest(
                model,
                projectName,
                groupId,
                javaVersion,
                seleniumVersion,
                junitVersion,
                junitPlatformVersion,
                cucumberVersion,
                webdrivermanagerVersion,
                surefireVersion,
                compilerPluginVersion
        );

        String githubToken = gitHubTokenSessionService.getRequiredAccessToken(session);
        
        this.generateTAMavenSeleniumCucumberJunit.generateAutomationJavaSeleniumCucumberProject(request, githubToken);

        log.info("Project generation completed successfully for project: {}", projectName);

        return Map.of("message", "Progetto generato con successo! Controlla i log per i dettagli.");
    }
    

}
