package it.nttdata.gaikingCopilot.controller;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.ai.GenerateTAMavenSeleniumCucumberJunit;
import it.nttdata.gaikingCopilot.copilot.GitHubTokenSessionService;
import it.nttdata.gaikingCopilot.model.AutomationProjectRequest;
import it.nttdata.gaikingCopilot.utility.OperationOnFileSystem;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;


import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;


@Log4j2
@RestController
@Validated
@RequestMapping("/")
@RequiredArgsConstructor
public class NewProjectController {

    private static final String MESSAGE_KEY = "message";

    private final GenerateTAMavenSeleniumCucumberJunit generateTAMavenSeleniumCucumberJunit;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    private static final String PROJECT_BASE_PATH_GRADLE = "newProject/selenium/java/gradle/junit/";
    private static final String PROJECT_BASE_PATH_MAVEN = "newProject/selenium/java/maven/junit/";

    @GetMapping("/newProject/gradle_selenium_junit5_cucumber")
    public Map<String, String> gradleSeleniumJunit5Cucumber(
        @RequestParam @NotBlank String model, 
        @RequestParam @NotBlank String projectName,
        @RequestParam @NotBlank String groupId,
        @RequestParam @NotBlank String javaVersion,
        @RequestParam @NotBlank String seleniumVersion,
        @RequestParam @NotBlank String junitVersion,
        @RequestParam @NotBlank String junitPlatformVersion,
        @RequestParam @NotBlank String cucumberVersion,
        @RequestParam @NotBlank String webdrivermanagerVersion,
        @RequestParam @NotBlank String surefireVersion,
        @RequestParam @NotBlank String compilerPluginVersion,
        WebSession session
    ) {
        if (session == null || session.isExpired()) {
            return Map.of(MESSAGE_KEY, "Sessione scaduta. Effettua il login.");
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return Map.of(MESSAGE_KEY, "Token GitHub non disponibile. Effettua il login.");
        }

        return Map.of(
            MESSAGE_KEY, "Project generated successfully.",
            "projectName", PROJECT_BASE_PATH_GRADLE + projectName
        );
    }
    


    @GetMapping("/newProject/mvn_selenium_junit5_cucumber")
    public Map<String, String> mavenSeleniumJunit5Cucumber(
        @RequestParam @NotBlank String model, 
        @RequestParam @NotBlank String projectName,
        @RequestParam @NotBlank String groupId,
        @RequestParam @NotBlank String javaVersion,
        @RequestParam @NotBlank String seleniumVersion,
        @RequestParam @NotBlank String junitVersion,
        @RequestParam @NotBlank String junitPlatformVersion,
        @RequestParam @NotBlank String cucumberVersion,
        @RequestParam @NotBlank String webdrivermanagerVersion,
        @RequestParam @NotBlank String surefireVersion,
        @RequestParam @NotBlank String compilerPluginVersion,
        WebSession session
    ) throws InterruptedException, ExecutionException {

        AutomationProjectRequest request = new AutomationProjectRequest(
                model,
                PROJECT_BASE_PATH_MAVEN + projectName,
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

        return Map.of(
            MESSAGE_KEY, "Project generated successfully.",
            "projectName", PROJECT_BASE_PATH_MAVEN + projectName
        );
    }

    @GetMapping("/newProject/download")
    public ResponseEntity<ByteArrayResource> downloadProject(
        @RequestParam String projectName,
        WebSession session
    ) throws IOException {
        if (session == null || session.isExpired()) {
            return ResponseEntity.status(401).build();
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        Path projectPath = Path.of(projectName);
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return ResponseEntity.notFound().build();
        }

        OperationOnFileSystem operationOnFileSystem = new OperationOnFileSystem();

        byte[] zipContent = operationOnFileSystem.zipProjectDirectory(projectPath);
        ByteArrayResource resource = new ByteArrayResource(zipContent);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + projectName + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipContent.length)
            .body(resource);
    }

    @DeleteMapping("/newProject/delete")
    public ResponseEntity<Map<String, String>> deleteProject(
        @RequestParam String projectName,
        WebSession session
    ) throws IOException {
        if (session == null || session.isExpired()) {
            return ResponseEntity.status(401).build();
        }       

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        Path projectPath = Path.of(projectName);
        if (!Files.exists(projectPath)) {
            return ResponseEntity.notFound().build();
        }

        OperationOnFileSystem operationOnFileSystem = new OperationOnFileSystem();
        operationOnFileSystem.deleteProjectDirectory(projectPath);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Project deleted successfully."));
    }

}
