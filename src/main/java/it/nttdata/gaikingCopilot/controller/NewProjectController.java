package it.nttdata.gaikingCopilot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
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
    public Map<String, String> getMethodName() throws InterruptedException, ExecutionException {

        this.generateTAMavenSeleniumCucumberJunit.generateAutomationJavaSeleniumCucumberProject();

        return Map.of("message", "Progetto generato con successo! Controlla i log per i dettagli.");
    }
    

}
