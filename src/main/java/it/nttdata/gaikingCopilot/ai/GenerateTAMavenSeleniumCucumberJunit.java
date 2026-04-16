package it.nttdata.gaikingCopilot.ai;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import it.nttdata.gaikingCopilot.copilot.CopilotService;
import it.nttdata.gaikingCopilot.utility.ProjectFileWriterTool;
import it.nttdata.gaikingCopilot.utility.ReadAndWriteJson;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class GenerateTAMavenSeleniumCucumberJunit {

    private final ReadAndWriteJson readAndWriteJson;

    private final ProjectFileWriterTool fileWriterTool;

    private final CopilotService copilotService;

    @Setter
    private String modelName = "gpt-5.3-codex";

    @Setter
    private String projectName = "automation-generated";

    @Setter
    private String javaVersion;
    @Setter
    private String seleniumVersion;
    @Setter
    private String junitVersion;
    @Setter
    private String junitPlatformVersion;
    @Setter
    private String cucumberVersion;
    @Setter
    private String webdrivermanagerVersion;
    @Setter
    private String surefireVersion;
    @Setter
    private String compilerPluginVersion;





    private String validateAndCleanJson(String response) {
        log.info("Raw LLM output:\n" + response);
        boolean isJson = this.readAndWriteJson.isValidJson(response);
        log.info("Is valid JSON? " + isJson);
        if (!isJson) {
            log.warn("Invalid JSON detected, attempting to clean...");
            String responseCleaned = this.readAndWriteJson.normalizeJsonEscapes(response);
            isJson = this.readAndWriteJson.isValidJson(responseCleaned);
            log.info("After normalization, is valid JSON? " + isJson);
            return responseCleaned;
        }else {
            return response;
        }
    } 


   public void generateAutomationJavaSeleniumCucumberProject() throws InterruptedException, ExecutionException {
        log.info("Generating Java Selenium Cucumber Project...");
        String pomContent = buildPOM();
        log.info("pom Content\n" + pomContent);
        String basePageContent = buildBasePage();
        log.info("BasePage Content\n" + basePageContent);
        String loginPageContent = buildLoginPage();
        log.info("LoginPage Content\n" + loginPageContent);
        String driverFactoryContent = buildDriverFactory();
        log.info("DriverFactory Content\n" + driverFactoryContent);
        String hooksContent = buildHooks();
        log.info("Hooks Content\n" + hooksContent);
        String testRunnerContent = buildRunCucumberTest();
        log.info("TestRunner Content\n" + testRunnerContent);
        String loginStepsContent = buildLoginSteps();
        log.info("LoginSteps Content\n" + loginStepsContent);
        String loginFeatureContent = buildLoginFeature();
        log.info("LoginFeature Content\n" + loginFeatureContent);

        // Combina tutti i file in un unico JSON
        String projectJson = "{ \"files\": ["
            + pomContent + ","
            + basePageContent + ","
            + loginPageContent + ","
            + driverFactoryContent + ","
            + hooksContent + ","
            + testRunnerContent + ","
            + loginStepsContent + ","
            + loginFeatureContent
            + "] }";
        log.info("Generated Project JSON:\n" + projectJson);
        fileWriterTool.writeProjectFiles(projectName, projectJson);
   }

   private String buildPOM() throws InterruptedException, ExecutionException{
        log.info("Building POM file...");           
        
        String systemPrompt = """
        Generate pom.xml for a Maven project.
        Rules:
        - Respond only with one single valid JSON object.
        - JSON format:
        {{"path":"pom.xml","content":"<full pom.xml (XML) with \\n for newlines and \\\" for quotes>"}}
        - The value of "content" must be only valid XML, not JSON.
        - Every newline as \\n, quotes as \\\".
        - Single line JSON.
        - No extra text or explanations.
        """;

        // String userPrompt = """
        // Generate pom.xml for a Maven project (Java 25).
        // Dependencies:
        // - org.seleniumhq.selenium:selenium-java:4.35.0
        // - org.junit.jupiter:junit-jupiter:5.13.4
        // - org.junit.platform:junit-platform-suite-api:1.13.4
        // - io.cucumber:cucumber-java:7.27.2
        // - io.cucumber:cucumber-junit-platform-engine:7.27.2
        // - io.github.bonigarcia:webdrivermanager:6.2.0
        // Plugins:
        // - maven-surefire-plugin:3.2.5
        // - maven-compiler-plugin:3.13.0 with <release>25</release>
        // GroupId = it.nttdata
        // ArtifactId = automation-generated
        // """;

        String userPrompt = String.format("""
            Generate pom.xml for a Maven project (Java %1$s).
            Dependencies:
            - org.seleniumhq.selenium:selenium-java:%2$s
            - org.junit.jupiter:junit-jupiter:%3$s
            - org.junit.platform:junit-platform-suite-api:%4$s
            - io.cucumber:cucumber-java:%5$s
            - io.cucumber:cucumber-junit-platform-engine:%5$s
            - io.github.bonigarcia:webdrivermanager:%6$s
            Plugins:
            - maven-surefire-plugin:%7$s
            - maven-compiler-plugin:%8$s with <release>%1$s</release>
            GroupId = it.nttdata
            ArtifactId = %9$s
            """,
            javaVersion,
            seleniumVersion,
            junitVersion,
            junitPlatformVersion,
            cucumberVersion,
            webdrivermanagerVersion,
            surefireVersion,
            compilerPluginVersion,
            projectName
        );

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, systemPrompt + "\n" + userPrompt);

        log.info("Risposta of the {} model to build the pom.xml: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString);
   }

   public String buildBasePage() throws InterruptedException, ExecutionException{
        log.info("Building BasePage class...");

        String basePagePrompt = """
            Generate BasePage.java with:
            - Package: pages
            - Abstract class: BasePage
            - Constructor that accepts WebDriver
            - Use PageFactory.initElements
            - Add methods: open, waitForElement, clickElement, typeText, getElementText

            Return ONLY this JSON on ONE line, nothing else:
            {{"path":"src/main/java/pages/BasePage.java","content":"<complete Java code with all braces and semicolons, newlines as \\n>"}}

            IMPORTANT:
            - Include all opening and closing braces {{ }}
            - Include all semicolons
            - Make sure the class is complete
            - Newlines as \\n
            - Quotes as \\"
            - Single line JSON
            """;

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, basePagePrompt);

        log.info("Risposta of the {} model to build the base page: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString);
    }

    private String buildLoginPage() throws InterruptedException, ExecutionException{
        log.info("Building LoginPage class...");
        String loginPagePrompt = """
            Generate LoginPage.java:
            - Extends BasePage
            - Constructor that receives WebDriver and passes it to BasePage
            - Fields: usernameInput, passwordInput, loginButton, errorBox
            - Methods: enterUsername, enterPassword, submit, getErrorMessage
            Rules:
            - Respond only with a single valid JSON object.
            - JSON format:
                {{
                    "path": "src/main/java/pages/LoginPage.java",
                    "content": "<full file content with \\n for each newline and all internal quotes escaped>"
                }}
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.            
        """;

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginPagePrompt);

        log.info("Risposta of the {} model to build the login page: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString);
    }

    private String buildDriverFactory() throws InterruptedException, ExecutionException{
        log.info("Building DriverFactory class...");
        String driverFactoryPrompt = """
            Generate the file DriverFactory.java inside package driver.

            Requirements:
            - Create a class named DriverFactory
            - Provide two static methods: createDriver and destroyDriver
            - In createDriver:
                - Initialize Chrome WebDriver using WebDriverManager
                - Configure ChromeOptions with common capabilities (e.g. start-maximized, disable notifications)
                - Return the WebDriver instance
            - In destroyDriver:
                - Properly quit the WebDriver instance if not null
            - Ensure thread safety if possible (e.g. ThreadLocal WebDriver)

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {{
                "path": "src/test/java/driver/DriverFactory.java",
                "content": "<full file content with \\n and \\\" escape>"
            }}
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """;

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, driverFactoryPrompt);

        log.info("Risposta of the {} model to build the DriverFactory: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString);
    }

    private String buildHooks() throws InterruptedException, ExecutionException{
        log.info("Building Hooks class...");
        String hooksPrompt = """
            Generate the file Hooks.java inside package hooks.
            Requirements:
            - Use @Before and @After annotations
            - Initialize and quit Chrome WebDriver using WebDriverManager
            - Share WebDriver with step classes

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {{
                "path": "src/test/java/hooks/Hooks.java",
                "content": "<full file content with \\n and \\\" escape>"
                }}
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
          """;

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, hooksPrompt);

        log.info("Risposta of the {} model to build the hooks: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss);
    }

    private String buildRunCucumberTest() throws InterruptedException, ExecutionException{
        log.info("Building TestRunner class...");

        String testRunnerPrompt = """
            Generate RunCucumberTest.java file.

            Requirements:
            - Package: runner
            - Class name: RunCucumberTest
            - Add these annotations on the class:
            @Suite
            @IncludeEngines("cucumber")
            @SelectClasspathResource("features")
            @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps,hooks")
            @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html, json:target/cucumber.json")
            - No methods, only annotations
            - MAKE SURE THE CLASS ENDS WITH A CLOSING BRACE }

            Return ONLY JSON on ONE line:
            {{"path":"src/test/java/runner/RunCucumberTest.java","content":"<complete Java code with closing brace, newlines as \\n>"}}

            IMPORTANT:
            - Include all opening and closing braces {{ }}
            - Include all semicolons
            - Make sure the class is complete
            - Newlines as \\n
            - Quotes as \\"
            - Single line JSON
        """;

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, testRunnerPrompt);

        log.info("Risposta of the {} model to build the RunCucumberTest: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss);
    }

    private String buildLoginSteps() throws InterruptedException, ExecutionException{
        log.info("Building LoginSteps class...");

        String loginStepsPrompt = """
            Generate LoginSteps.java file.
            Package steps. Imports: io.cucumber.java.en.*; pages.LoginPage; hooks.Hooks; org.junit.jupiter.api.Assertions.*;
            Class LoginSteps with private field LoginPage loginPage.
            5 Cucumber step methods:
            1. @Given with method openLoginPage(String url) - creates LoginPage with Hooks.getDriver()
            2. @When with method enterUsername(String username) - calls loginPage.enterUsername
            3. @And with method enterPassword(String password) - calls loginPage.enterPassword
            4. @And with method clickLoginButton - calls loginPage.submit
            5. @Then with method verifyErrorMessage(String expected) - calls assertTrue
            
            Return ONLY JSON on ONE line:
            {{"path":"src/test/java/steps/LoginSteps.java","content":"<complete Java code with braces, newlines as \\n>"}}
        """;

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginStepsPrompt);

        log.info("Risposta of the {} model to build the LoginSteps: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss);
    }

    private String buildLoginFeature() throws InterruptedException, ExecutionException{
        log.info("Building login.feature file...");
        String loginFeaturePrompt = """
            Generate login.feature:
            - Feature: Login
            - Scenario: Failed login with incorrect credentials
            Given I open the login page "https://example.com/login"
            When I enter username "wronguser"
            And I enter password "wrongpass"
            And I click the login button
            Then I verify an error message containing "Invalid credentials"
            Rules:
            - Respond only with a single valid JSON object.
            - JSON format:
                {{
                    "path": "src/test/resources/features/login.feature",
                    "content": "<full file content with \\n for newlines and all internal quotes escaped>"
                }}
            - Every newline inside "content" must be encoded as \\n (two backslashes + n).
            - Every double quote inside "content" must be escaped as \\\".
            - The JSON must be on one single line (no pretty-printing, no indentation).
            - Do not add any explanations, comments, or text before or after the JSON.
            - Output must be strictly valid JSON that can be parsed by com.fasterxml.jackson.databind.ObjectMapper.readTree().          
          """;

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginFeaturePrompt);

        log.info("Risposta of the {} model to build the login.feature: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss);
    }

}
