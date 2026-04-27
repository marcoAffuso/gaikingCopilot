package it.nttdata.gaikingCopilot.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.ai.GenerateTAMavenSeleniumCucumberJunit;
import it.nttdata.gaikingCopilot.model.AutomationProjectRequest;
import it.nttdata.gaikingCopilot.model.CreateProjectGitRequest;
import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;
import it.nttdata.gaikingCopilot.service.git.GitRepositoryService;
import it.nttdata.gaikingCopilot.utility.OperationOnFileSystem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;


import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    private final GitRepositoryService gitRepositoryService;

    private static final String PROJECT_BASE_PATH_GRADLE = "newProject/selenium/java/gradle/junit/";
    private static final String PROJECT_BASE_PATH_MAVEN = "newProject/selenium/java/maven/junit/";

    @GetMapping("/newProject/gradle_selenium_junit5_cucumber")
    public Map<String, String> gradleSeleniumJunit5Cucumber(
        @RequestParam @NotBlank String model,
	    @RequestParam @NotBlank String reasoningEffort,
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
        log.info("Handling Gradle project generation request. sessionId={}, projectName={}, model={}, javaVersion={}", sessionId(session), projectName, model, javaVersion);

        if (session == null || session.isExpired()) {
            log.info("Rejecting Gradle project generation request because session is missing or expired. sessionId={}", sessionId(session));
            return Map.of(MESSAGE_KEY, "Sessione scaduta. Effettua il login.");
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Rejecting Gradle project generation request because GitHub token is missing in session. sessionId={}", session.getId());
            return Map.of(MESSAGE_KEY, "Token GitHub non disponibile. Effettua il login.");
        }

        log.info("Returning Gradle project generation response. sessionId={}, projectPath={}", session.getId(), PROJECT_BASE_PATH_GRADLE + projectName);

        return Map.of(
            MESSAGE_KEY, "Project generated successfully.",
            "projectName", PROJECT_BASE_PATH_GRADLE + projectName
        );
    }
    


    @GetMapping("/newProject/mvn_selenium_junit5_cucumber")
    public Map<String, String> mavenSeleniumJunit5Cucumber(
        @RequestParam @NotBlank String model,
	    @RequestParam @NotBlank String reasoningEffort, 
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
        log.info("Handling Maven project generation request. sessionId={}, projectName={}, model={}, javaVersion={}", sessionId(session), projectName, model, javaVersion);

        AutomationProjectRequest request = new AutomationProjectRequest(
                model,
                reasoningEffort,
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

        log.info("Maven project generation completed. sessionId={}, projectPath={}", sessionId(session), PROJECT_BASE_PATH_MAVEN + projectName);

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
        log.info("Handling project download request. sessionId={}, projectName={}", sessionId(session), projectName);

        if (session == null || session.isExpired()) {
            log.info("Rejecting project download request because session is missing or expired. sessionId={}", sessionId(session));
            return ResponseEntity.status(401).build();
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Rejecting project download request because GitHub token is missing in session. sessionId={}", session.getId());
            return ResponseEntity.status(401).build();
        }

        Path projectPath = Path.of(projectName);
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.info("Project download request returned not found. sessionId={}, projectName={}", session.getId(), projectName);
            return ResponseEntity.notFound().build();
        }

        OperationOnFileSystem operationOnFileSystem = new OperationOnFileSystem();

        byte[] zipContent = operationOnFileSystem.zipProjectDirectory(projectPath);
        ByteArrayResource resource = new ByteArrayResource(zipContent);

        log.info("Returning project download response. sessionId={}, projectName={}, contentLength={}", session.getId(), projectName, zipContent.length);

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
        log.info("Handling project delete request. sessionId={}, projectName={}", sessionId(session), projectName);

        if (session == null || session.isExpired()) {
            log.info("Rejecting project delete request because session is missing or expired. sessionId={}", sessionId(session));
            return ResponseEntity.status(401).build();
        }       

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Rejecting project delete request because GitHub token is missing in session. sessionId={}", session.getId());
            return ResponseEntity.status(401).build();
        }

        Path projectPath = Path.of(projectName);
        if (!Files.exists(projectPath)) {
            log.info("Project delete request returned not found. sessionId={}, projectName={}", session.getId(), projectName);
            return ResponseEntity.notFound().build();
        }

        OperationOnFileSystem operationOnFileSystem = new OperationOnFileSystem();
        operationOnFileSystem.deleteProjectDirectory(projectPath);
        log.info("Project delete completed. sessionId={}, projectName={}", session.getId(), projectName);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Project deleted successfully."));
    }

    @PostMapping("/newProject/createProjectGit")
    public ResponseEntity<Map<String, String>> createProjectGit(
        @Valid @RequestBody CreateProjectGitRequest request,
        WebSession session
    ) throws IOException, GitAPIException, URISyntaxException {
        log.info("Handling Git project creation request. sessionId={}, projectName={}, repositoryName={}", sessionId(session), request.getProjectName(), request.getRepositoryName());

        if (session == null || session.isExpired()) {
            log.info("Rejecting Git project creation request because session is missing or expired. sessionId={}", sessionId(session));
            return ResponseEntity.status(401).body(Map.of(MESSAGE_KEY, "Sessione scaduta. Effettua il login."));
        }       

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Rejecting Git project creation request because GitHub token is missing in session. sessionId={}", session.getId());
            return ResponseEntity.status(401).build();
        }

        Path projectPath = Path.of(request.getProjectName());
        if (!Files.exists(projectPath)) {
            log.info("Git project creation request returned not found. sessionId={}, projectName={}", session.getId(), request.getProjectName());
            return ResponseEntity.notFound().build();
        }

        String branchName = "main";   // oppure "master"
        gitRepositoryService.initializeRepository(projectPath);
        gitRepositoryService.addFiles(projectPath);
        gitRepositoryService.commit(projectPath, "Initial commit");
        gitRepositoryService.addRemote(projectPath, request.getRepositoryName());
        gitRepositoryService.checkoutBranch(projectPath, branchName);
        gitRepositoryService.push(projectPath, branchName, request.getUserGit(), request.getTokenGit());

        log.info("Git project creation completed. sessionId={}, projectName={}, branchName={}", session.getId(), request.getProjectName(), branchName);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Project Git created successfully."));
    }

    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }




}
