package it.nttdata.gaikingCopilot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nttdata.gaikingCopilot.ai.GenerateTAMavenSeleniumCucumberJunit;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;


@Log4j2
@RestController
@RequestMapping("/")
public class NewProjectController {

    private final GenerateTAMavenSeleniumCucumberJunit generateTAMavenSeleniumCucumberJunit;

    public NewProjectController(GenerateTAMavenSeleniumCucumberJunit generateTAMavenSeleniumCucumberJunit) {
        this.generateTAMavenSeleniumCucumberJunit = generateTAMavenSeleniumCucumberJunit;
    }

    @GetMapping("/newProject/mvn_selenium_junit5_cucumber")
    public Map<String, String> getMethodName(
        @RequestParam String model, 
        @RequestParam String projectName,
        @RequestParam String javaVersion,
        @RequestParam String seleniumVersion,
        @RequestParam String junitVersion,
        @RequestParam String junitPlatformVersion,
        @RequestParam String cucumberVersion,
        @RequestParam String webdrivermanagerVersion,
        @RequestParam String surefireVersion,
        @RequestParam String compilerPluginVersion
    ) throws InterruptedException, ExecutionException {

        this.generateTAMavenSeleniumCucumberJunit.setModelName(model);
        this.generateTAMavenSeleniumCucumberJunit.setProjectName(projectName);
        this.generateTAMavenSeleniumCucumberJunit.setJavaVersion(javaVersion);
        this.generateTAMavenSeleniumCucumberJunit.setSeleniumVersion(seleniumVersion);
        this.generateTAMavenSeleniumCucumberJunit.setJunitVersion(junitVersion);
        this.generateTAMavenSeleniumCucumberJunit.setJunitPlatformVersion(junitPlatformVersion);
        this.generateTAMavenSeleniumCucumberJunit.setCucumberVersion(cucumberVersion);
        this.generateTAMavenSeleniumCucumberJunit.setWebdrivermanagerVersion(webdrivermanagerVersion);
        this.generateTAMavenSeleniumCucumberJunit.setSurefireVersion(surefireVersion);
        this.generateTAMavenSeleniumCucumberJunit.setCompilerPluginVersion(compilerPluginVersion);
        this.generateTAMavenSeleniumCucumberJunit.generateAutomationJavaSeleniumCucumberProject();

        log.info("Project generation completed successfully for project: {}", projectName);

        return Map.of("message", "Progetto generato con successo! Controlla i log per i dettagli.");
    }
    

}
