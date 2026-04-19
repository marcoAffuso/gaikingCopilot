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
    private String groupId = "it.nttdata";

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

    private static final int MAX_JSON_REPAIR_RETRIES = 1;

    private String validateAndCleanJson(String response, String originalPrompt)
            throws InterruptedException, ExecutionException {

        log.info("Raw LLM output:\n{}", response);

        String candidate = sanitizeJsonCandidate(response);
        if (this.readAndWriteJson.isValidJson(candidate)) {
            log.info("Response is valid JSON after initial sanitization.");
            return candidate;
        }

        String parserError = this.readAndWriteJson.getJsonValidationError(candidate);
        log.warn("Invalid JSON detected. Parser error: {}", parserError);

        String cleanedCandidate = tryLocalJsonRepairs(candidate);
        if (this.readAndWriteJson.isValidJson(cleanedCandidate)) {
            log.info("Local cleanup produced valid JSON.");
            return cleanedCandidate;
        }

        String cleanedParserError = this.readAndWriteJson.getJsonValidationError(cleanedCandidate);
        log.warn("JSON still invalid after local cleanup. Parser error: {}", cleanedParserError);

        if (originalPrompt == null || originalPrompt.isBlank()) {
            throw new IllegalStateException(
                    "Invalid JSON and no original prompt available for retry. Parser error: "
                            + cleanedParserError
                            + "\nLast payload:\n"
                            + cleanedCandidate
            );
        }

        String latestCandidate = cleanedCandidate;
        String latestParserError = cleanedParserError;

        for (int attempt = 1; attempt <= MAX_JSON_REPAIR_RETRIES; attempt++) {
            log.warn(
                    "JSON still invalid. Requesting controlled repair from model, attempt {}/{}. Parser error: {}",
                    attempt,
                    MAX_JSON_REPAIR_RETRIES,
                    latestParserError
            );

            String repairPrompt = buildJsonRepairPrompt(originalPrompt, latestCandidate, latestParserError);
            String retriedResponse = copilotService.getResponseCopilotWhitOutStreaming(modelName, repairPrompt);

            log.info("Repair attempt {} raw output:\n{}", attempt, retriedResponse);

            latestCandidate = sanitizeJsonCandidate(retriedResponse);
            if (this.readAndWriteJson.isValidJson(latestCandidate)) {
                log.info("Repair attempt {} produced valid JSON.", attempt);
                return latestCandidate;
            }

            latestParserError = this.readAndWriteJson.getJsonValidationError(latestCandidate);
            log.warn("Repair attempt {} still invalid before local cleanup. Parser error: {}", attempt, latestParserError);

            latestCandidate = tryLocalJsonRepairs(latestCandidate);
            if (this.readAndWriteJson.isValidJson(latestCandidate)) {
                log.info("Repair attempt {} produced valid JSON after local cleanup.", attempt);
                return latestCandidate;
            }

            latestParserError = this.readAndWriteJson.getJsonValidationError(latestCandidate);
            log.warn("Repair attempt {} still invalid after local cleanup. Parser error: {}", attempt, latestParserError);
        }

        throw new IllegalStateException(
                "Unable to obtain valid JSON after local cleanup and retry. Parser error: "
                        + latestParserError
                        + "\nLast payload:\n"
                        + latestCandidate
        );
    }

    private String sanitizeJsonCandidate(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = this.readAndWriteJson.cleanJson(raw).trim();
        return unwrapOuterDoubleBraces(cleaned);
    }

    private String unwrapOuterDoubleBraces(String value) {
        if (value.startsWith("{{") && value.endsWith("}}") && value.length() >= 4) {
            log.warn("Detected outer double braces. Converting '{{...}}' to '{...}'.");
            return value.substring(1, value.length() - 1).trim();
        }

        return value;
    }

    private String tryLocalJsonRepairs(String raw) {
        String candidate = raw;

        try {
            candidate = this.readAndWriteJson.normalizeJsonEscapes(candidate);
        } catch (Exception ex) {
            log.warn("normalizeJsonEscapes failed: {}", ex.getMessage());
        }

        return sanitizeJsonCandidate(candidate);
    }

    private String buildJsonRepairPrompt(String originalPrompt, String invalidJsonResponse, String parserError) {
        return """
            The previous answer for the prompt below is not valid JSON.

            Repair it and return ONLY one valid JSON object on a single line.

            Parser error:
            """ + parserError + """

            Rules:
            - Return only one valid JSON object
            - Do not add explanations
            - Do not add markdown fences
            - Do not add comments
            - Do not wrap the whole JSON with double outer braces like {{...}}
            - Use standard JSON with one outer object: {...}
            - Preserve the intended path and content from the invalid response
            - Escape all internal newlines as \\n
            - Escape all internal double quotes as \\"
            - The output must be parseable by Jackson ObjectMapper.readTree()

            Original generation prompt:
            """ + originalPrompt + """

            Invalid response to repair:
            """ + invalidJsonResponse;
    }


   public void generateAutomationJavaSeleniumCucumberProject() throws InterruptedException, ExecutionException {
        log.info("Generating Java Selenium Cucumber Project...");
        String pomContent = buildPOM();
        log.info("pom Content\n" + pomContent);
        String basePageContent = buildBasePage();
        log.info("BasePage Content\n" + basePageContent);
        String loginPageContent = buildLoginPage();
        log.info("LoginPage Content\n" + loginPageContent);
        String pageObjectManagerContent = buildPageObjectManager();
        log.info("PageObjectManager Content\n" + pageObjectManagerContent);
        String driverFactoryContent = buildDriverFactory();
        log.info("DriverFactory Content\n" + driverFactoryContent);
        String hooksContent = buildHooks();
        log.info("Hooks Content\n" + hooksContent);
        String hooksInterfaceContent = buildHooksInterface();
        log.info("HooksInterface Content\n" + hooksInterfaceContent);
        String testRunnerContent = buildRunCucumberTest();
        log.info("TestRunner Content\n" + testRunnerContent);
        String testContextContent = buildTestContext();
        log.info("TestContext Content\n" + testContextContent);
        String loginStepsContent = buildLoginSteps();
        log.info("LoginSteps Content\n" + loginStepsContent);
        String loginFeatureContent = buildLoginFeature();
        log.info("LoginFeature Content\n" + loginFeatureContent);

        // Combina tutti i file in un unico JSON
        String projectJson = "{ \"files\": ["
            + pomContent + ","
            + basePageContent + ","
            + loginPageContent + ","
            + pageObjectManagerContent + ","
            + driverFactoryContent + ","
            + hooksContent + ","
            + hooksInterfaceContent + ","
            + testRunnerContent + ","
            + testContextContent + ","
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
            {"path":"pom.xml","content":"<full pom.xml (XML) with \\n for newlines and \\\" for quotes>"}
            - The value of "content" must be only valid XML, not JSON.
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
            """;

        String userPrompt = String.format("""
            Generate pom.xml for a Maven project (Java %1$s).

            Project structure requirements:
            - Classes pages.BasePage and pages.LoginPage are under src/main/java
            - Therefore Selenium classes used by src/main/java must be available in main compile scope
            - Test-related classes such as Hooks, DriverFactory, LoginSteps, RunCucumberTest are under src/test/java

            Dependencies:
            - org.seleniumhq.selenium:selenium-java:%2$s
            - org.junit.jupiter:junit-jupiter:%3$s
            - org.junit.platform:junit-platform-suite-api:%4$s
            - io.cucumber:cucumber-java:%5$s
            - io.cucumber:cucumber-junit-platform-engine:%5$s
            - io.cucumber:cucumber-picocontainer:%5$s
            - io.github.bonigarcia:webdrivermanager:%6$s

            Dependency scope rules:
            - selenium-java MUST NOT use <scope>test</scope>
            - selenium-java must be available to src/main/java classes, so leave it in the default compile scope
            - junit-jupiter should use <scope>test</scope>
            - junit-platform-suite-api should use <scope>test</scope>
            - cucumber-java should use <scope>test</scope>
            - cucumber-junit-platform-engine should use <scope>test</scope>
            - cucumber-picocontainer should use <scope>test</scope>
            - webdrivermanager should use <scope>test</scope>

            Plugins:
            - maven-surefire-plugin:%7$s
            - maven-compiler-plugin:%8$s with <release>%1$s</release>

            GroupId = %9$s
            ArtifactId = %10$s

            Additional requirements:
            - Include cucumber-picocontainer with the same version as cucumber-java
            - The pom.xml must support Cucumber constructor injection for step classes
            - The generated project must be compatible with:
                - hooks.Hooks implementing HooksInterface
                - step classes receiving Hooks through constructor injection
                - pages.BasePage and pages.LoginPage under src/main/java using Selenium imports
            - Use standard Maven structure
            - Ensure all required dependencies are included and valid
            - Do NOT assign test scope to dependencies required by src/main/java classes
            """,
            javaVersion,
            seleniumVersion,
            junitVersion,
            junitPlatformVersion,
            cucumberVersion,
            webdrivermanagerVersion,
            surefireVersion,
            compilerPluginVersion,
            groupId,
            projectName
        );

        String responseCopilString = copilotService.getResponseCopilotWithStreaming(modelName, systemPrompt + "\n" + userPrompt);

        log.info("Risposta of the {} model to build the pom.xml: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString, systemPrompt + "\n" + userPrompt);
   }

   public String buildBasePage() throws InterruptedException, ExecutionException{
        log.info("Building BasePage class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String basePagePrompt = String.format("""
            Generate the file BasePage.java inside package pages.

            Requirements:
            - Package: %1$s.%2$s.pages
            - Create an abstract generic class with this signature:
              public abstract class BasePage<T extends BasePage<T>> extends LoadableComponent<T>
            - Import java.time.Duration
            - Import org.openqa.selenium.By
            - Import org.openqa.selenium.WebDriver
            - Import org.openqa.selenium.WebElement
            - Import org.openqa.selenium.support.PageFactory
            - Import org.openqa.selenium.support.ui.ExpectedConditions
            - Import org.openqa.selenium.support.ui.LoadableComponent
            - Import org.openqa.selenium.support.ui.WebDriverWait
            - Declare protected fields named driver and wait
            - In the constructor:
                - Accept a WebDriver parameter
                - Assign it to the driver field
                - Initialize wait with new WebDriverWait(driver, Duration.ofSeconds(10))
                - Call PageFactory.initElements(driver, this)
            - Add method open(String url) that calls driver.get(url)
            - Add method protected WebElement waitForElement(By locator) that returns wait.until(ExpectedConditions.visibilityOfElementLocated(locator))
            - Add method clickElement(By locator) that clicks the element returned by waitForElement(locator)
            - Add method typeText(By locator, String text) that clears the element and sends the provided text
            - Add method getElementText(By locator) that returns the text of the element returned by waitForElement(locator)
            - The code must be complete, valid, and compilable Java

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {
                "path": "src/main/java/%1$s/%2$s/pages/BasePage.java",
                "content": "<full file content with \\n and \\\" escape>"
            }
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, basePagePrompt);

        log.info("Risposta of the {} model to build the base page: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString, basePagePrompt);
    }

    private String buildLoginPage() throws InterruptedException, ExecutionException{
        log.info("Building LoginPage class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String loginPagePrompt = String.format("""
            Generate the file LoginPage.java inside package pages.

            Requirements:
            - Package: %1$s.%2$s.pages
            - Create a class named LoginPage
            - The class must extend BasePage<LoginPage>
            - Import org.openqa.selenium.By
            - Import org.openqa.selenium.WebDriver
            - Import org.openqa.selenium.support.ui.ExpectedConditions
            - Declare these private final By fields with exactly these names and locators:
                - usernameInput = By.id("username")
                - passwordInput = By.id("password")
                - loginButton = By.cssSelector("button[type='submit']")
                - errorBox = By.cssSelector(".error, .alert-danger, [role='alert']")
            - Add a constructor that receives a WebDriver and calls super(driver)
            - Do NOT declare another WebDriver field in this class, because the inherited driver field from BasePage must be reused
            - Override the method load() with an empty implementation
            - Override the method isLoaded() throws Error
            - In isLoaded():
                - Use wait.until(ExpectedConditions.visibilityOfElementLocated(usernameInput))

            Page behavior methods:
            - Add method enterUsername(String username)
                - Use the inherited helper method typeText(usernameInput, username)
            - Add method enterPassword(String password)
                - Use the inherited helper method typeText(passwordInput, password)
            - Add method submit()
                - Use the inherited helper method clickElement(loginButton)
            - Add method getErrorMessage()
                - Use the inherited helper method getElementText(errorBox)

            Design constraints:
            - Reuse BasePage behavior instead of duplicating direct driver.findElement(...) logic where possible
            - Keep the same functional behavior as a standard login page object
            - The code must be complete, valid, and compilable Java
            - The class must remain compatible with BasePage<LoginPage> and its inherited protected fields and helper methods

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {
                "path": "src/main/java/%1$s/%2$s/pages/LoginPage.java",
                "content": "<full file content with \\n and \\\" escape>"
            }
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginPagePrompt);

        log.info("Risposta of the {} model to build the login page: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString, loginPagePrompt);
    }

    private String buildPageObjectManager() throws InterruptedException, ExecutionException{
        log.info("Building PageObjectManager class...");

        String[] partsOfGroupId = groupId.split("\\.");

        String pageObjectManagerPrompt = String.format("""
            Generate the file PageObjectManager.java inside package %1$s.%2$s.pages.

            Goal:
            - Generate exactly one Java class named PageObjectManager
            - The generated class must match the required structure precisely
            - The generated code must be complete, valid, and compilable Java

            Package and target:
            - Package: %1$s.%2$s.pages
            - The only file to generate is PageObjectManager.java in package %1$s.%2$s.pages

            Required imports:
            - org.openqa.selenium.WebDriver

            Forbidden imports:
            - %1$s.%2$s.hooks.Hooks
            - %1$s.%2$s.hooks.HooksInterface
            - %1$s.%2$s.driver.DriverFactory
            - org.openqa.selenium.By
            - org.openqa.selenium.support.ui.ExpectedConditions

            Class structure rules:
            - Create exactly one public class named PageObjectManager
            - Declare exactly these two fields:
            - private final WebDriver webDriver;
            - private LoginPage loginPage;
            - Do not declare any other fields

            Constructor rules:
            - Add exactly one constructor
            - Constructor signature must be exactly:
            public PageObjectManager(WebDriver webDriver)
            - In the constructor:
            - assign the received webDriver to this.webDriver;
            - Do not add any other constructor

            Required methods:
            - Add exactly one public method besides the constructor
            - Method signature must be exactly:
            public LoginPage getLoginPage()
            - In getLoginPage():
            - if loginPage is null, initialize it with new LoginPage(webDriver);
            - return loginPage;
            - Do not add any other methods
            - Do not add getters for webDriver
            - Do not add setters

            Behavioral constraints:
            - The class must reuse the same WebDriver instance received in the constructor
            - The class must create LoginPage only through getLoginPage()
            - The class must reuse the same LoginPage instance once created
            - The class must not manage WebDriver lifecycle
            - The class must not create or destroy drivers
            - The class must not instantiate DriverFactory
            - Keep the implementation simple and focused on page object creation and reuse

            Exactness constraints:
            - Do not add extra fields
            - Do not add helper methods
            - Do not add comments
            - Do not add annotations
            - Do not add Lombok annotations
            - Do not add unused imports
            - Do not add example code outside the class

            The generated class must follow this exact shape:
            - public class PageObjectManager
            - two fields:
            - webDriver
            - loginPage
            - one constructor:
            - PageObjectManager(WebDriver webDriver)
            - one method:
            - getLoginPage

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Return exactly one JSON object
            - Return the JSON on a single line
            - Do not add markdown fences
            - Do not add explanations
            - Do not add comments
            - Use this exact format:
            {
                "path": "src/main/java/%1$s/%2$s/pages/PageObjectManager.java",
                "content": "<full Java file content with \\n for newlines and \\\" for quotes>"
            }
            - The value of "content" must contain only the complete Java source code
            - Every newline in "content" must be escaped as \\n
            - Every internal double quote in "content" must be escaped as \\\"
            - The output must be parseable by Jackson ObjectMapper.readTree()
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, pageObjectManagerPrompt);

        log.info("Risposta of the {} model to build the PageObjectManager: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString, pageObjectManagerPrompt);
    }

    private String buildDriverFactory() throws InterruptedException, ExecutionException{
        log.info("Building DriverFactory class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String driverFactoryPrompt = String.format("""
            Generate the file DriverFactory.java inside package %1$s.%2$s.driver.

            Requirements:
            - Create a class named DriverFactory inside package driver
            - Do NOT use static methods for driver lifecycle management
            - Provide exactly two public instance methods named createDriver and destroyDriver
            - Declare a private ThreadLocal<WebDriver> field to store the driver managed by this DriverFactory instance
            - In createDriver:
                - If the current thread already has a WebDriver instance, return that existing instance
                - Otherwise initialize Chrome WebDriver using WebDriverManager
                - Configure ChromeOptions with common capabilities such as start-maximized and disable-notifications
                - Create the ChromeDriver instance
                - Store the created WebDriver inside the ThreadLocal field
                - Return the stored WebDriver instance
            - In destroyDriver:
                - Read the current thread WebDriver from the ThreadLocal field
                - If the driver is not null, quit it properly
                - Remove the value from the ThreadLocal field after quitting
            - DriverFactory must encapsulate all browser creation and destruction logic
            - The code must be complete, valid, and compilable Java

            Alignment with Hooks:
            - This class will be used by Hooks through a DriverFactory instance created as new DriverFactory()
            - Hooks will call driverFactory.createDriver() inside the @Before method
            - Hooks will call driverFactory.destroyDriver() inside the @After method
            - Therefore createDriver and destroyDriver MUST be instance methods and MUST NOT be static

            Strict constraints:
            - DO NOT declare createDriver as static
            - DO NOT declare destroyDriver as static
            - DO NOT expose extra public lifecycle methods besides createDriver and destroyDriver
            - DO NOT put Cucumber annotations in this class
            - DO NOT move lifecycle responsibilities out of DriverFactory
            - DO NOT omit imports required for WebDriver, ChromeDriver, ChromeOptions, and WebDriverManager

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {
                "path": "src/test/java/%1$s/%2$s/driver/DriverFactory.java",
                "content": "<full file content with \\n and \\\" escape>"
            }
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseCopilString = copilotService.getResponseCopilotWhitOutStreaming(modelName, driverFactoryPrompt);

        log.info("Risposta of the {} model to build the DriverFactory: {}", modelName, responseCopilString);
        return validateAndCleanJson(responseCopilString, driverFactoryPrompt);
    }

    private String buildHooks() throws InterruptedException, ExecutionException{
        log.info("Building Hooks class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String hooksPrompt = String.format("""
            Generate the file Hooks.java inside package %1$s.%2$s.hooks.

            Requirements:
            - Create a class named Hooks inside package %1$s.%2$s.hooks
            - The class must implement HooksInterface
            - Import %1$s.%2$s.driver.DriverFactory
            - Import io.cucumber.java.Before
            - Import io.cucumber.java.After
            - Import org.openqa.selenium.WebDriver
            - Declare a private field named driverFactory of type DriverFactory
            - Declare a private field named driver of type WebDriver
            - Implement the method getDriver() declared in HooksInterface
            - Add the annotation @Override immediately above the getDriver() method
            - Implement the method exactly with this signature:
            public WebDriver getDriver()
            - The getDriver() method must return the field driver
            - Add a method annotated with @Before named beforeScenario
            - In beforeScenario:
                - Instantiate DriverFactory with new DriverFactory()
                - Assign it to the field driverFactory
                - Initialize the field driver by calling driverFactory.createDriver()
            - Add a method annotated with @After named afterScenario
            - In afterScenario:
                - Call driverFactory.destroyDriver()
                - Set driver to null
            - Do not use static fields
            - Do not use static methods
            - Do not create ChromeDriver directly in this class
            - Do not use WebDriverManager directly in this class
            - Do not configure ChromeOptions in this class
            - The class must be complete, valid, and compilable Java
            - Match this structure exactly:
                - class Hooks implements HooksInterface
                - fields: driverFactory, driver
                - methods: getDriver, beforeScenario, afterScenario
                - getDriver must be annotated with @Override

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {
                "path": "src/test/java/%1$s/%2$s/hooks/Hooks.java",
                "content": "<full file content with \\n and \\\" escape>"
            }
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, hooksPrompt);

        log.info("Risposta of the {} model to build the hooks: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, hooksPrompt);
    }

    private String buildHooksInterface() throws InterruptedException, ExecutionException{
        log.info("Building HooksInterface...");

        String [] partsOfGroupId = groupId.split("\\.");

        String hooksInterfacePrompt = String.format("""
            Generate the file HooksInterface.java inside package %1$s.%2$s.hooks.

            Requirements:
            - Create an interface named HooksInterface inside package %1$s.%2$s.hooks
            - Import org.openqa.selenium.WebDriver
            - Declare exactly one method:
            WebDriver getDriver();
            - Do not add any other methods
            - Do not add fields
            - The code must be complete, valid, and compilable Java
            - The generated file must be compatible with a Hooks class that implements HooksInterface and returns a Selenium WebDriver

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Format:
            {
                "path": "src/test/java/%1$s/%2$s/hooks/HooksInterface.java",
                "content": "<full file content with \\n and \\\" escape>"
            }
            - Every newline as \\n, quotes as \\\".
            - Single line JSON.
            - No extra text or explanations.
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, hooksInterfacePrompt);

        log.info("Risposta of the {} model to build the HooksInterface: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, hooksInterfacePrompt);
    }

    private String buildRunCucumberTest() throws InterruptedException, ExecutionException{
        log.info("Building TestRunner class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String testRunnerPrompt = String.format("""
            Generate RunCucumberTest.java file.

            Requirements:
            - Package: %1$s.%2$s.runner
            - Class name: RunCucumberTest
            - Add these annotations on the class:
            @Suite
            @IncludeEngines("cucumber")
            @SelectClasspathResource("features")
            @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "%1$s.%2$s.steps,%1$s.%2$s.hooks")
            @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html, json:target/cucumber.json")
            - No methods, only annotations
            - MAKE SURE THE CLASS ENDS WITH A CLOSING BRACE }

            Return ONLY JSON on ONE line:
            {"path":"src/test/java/%1$s/%2$s/runner/RunCucumberTest.java","content":"<complete Java code with closing brace, newlines as \\n>"}

            IMPORTANT:
            - Include all opening and closing braces { }
            - Include all semicolons
            - Make sure the class is complete
            - Newlines as \\n
            - Quotes as \\\"
            - Single line JSON
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, testRunnerPrompt);

        log.info("Risposta of the {} model to build the RunCucumberTest: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, testRunnerPrompt);
    }

    private String buildTestContext() throws InterruptedException, ExecutionException{
        log.info("Building TestContext class...");
        String [] partsOfGroupId = groupId.split("\\.");

        String testContextPrompt = String.format("""
            Generate the file TestContext.java inside package %1$s.%2$s.utility.

            Goal:
            - Generate exactly one Java class named TestContext
            - The generated class must match the required structure precisely
            - The generated code must be complete, valid, and compilable Java

            Package and file path:
            - Package: %1$s.%2$s.utility

            Required imports:
            - org.openqa.selenium.WebDriver
            - %1$s.%2$s.hooks.HooksInterface
            - %1$s.%2$s.pages.PageObjectManager

            Forbidden imports:
            - %1$s.%2$s.hooks.Hooks
            - %1$s.%2$s.driver.DriverFactory
            - %1$s.%2$s.pages.LoginPage

            Class structure rules:
            - Create exactly one public class named TestContext
            - Declare exactly these three fields:
            - private WebDriver driver;
            - private HooksInterface hooks;
            - private PageObjectManager pageObjectManager;
            - Do not declare any other fields

            Constructor rules:
            - Add exactly one constructor
            - Constructor signature must be exactly:
            public TestContext(HooksInterface hooks)
            - In the constructor:
            - assign the received hooks to this.hooks;
            - initialize this.driver with hooks.getDriver();
            - Do not add any other constructor

            Required methods:
            - Add exactly one public method besides the constructor
            - Method signature must be exactly:
            public PageObjectManager getPageObjectManager()
            - In getPageObjectManager():
            - if this.pageObjectManager is null, initialize it with new PageObjectManager(this.driver);
            - return this.pageObjectManager;
            - Do not add any other methods
            - Do not add getters for driver
            - Do not add getters for hooks
            - Do not add setters

            Behavioral constraints:
            - The class must reuse the WebDriver obtained from HooksInterface
            - The class must create PageObjectManager only through getPageObjectManager()
            - The class must reuse the same PageObjectManager instance once created
            - The class must not manage WebDriver lifecycle
            - The class must not create or destroy drivers
            - The class must not instantiate DriverFactory
            - Keep the implementation simple and focused on shared test context management

            Exactness constraints:
            - Do not add extra fields
            - Do not add helper methods
            - Do not add comments
            - Do not add annotations
            - Do not add Lombok annotations
            - Do not add unused imports
            - Do not instantiate LoginPage directly
            - Do not add example code outside the class

            The generated class must follow this exact shape:
            - public class TestContext
            - three fields:
            - driver
            - hooks
            - pageObjectManager
            - one constructor:
            - TestContext(HooksInterface hooks)
            - one method:
            - getPageObjectManager

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Return exactly one JSON object
            - Return the JSON on a single line
            - Do not add markdown fences
            - Do not add explanations
            - Do not add comments
            - Use this exact format:
            {
                "path": "src/test/java/%1$s/%2$s/utility/TestContext.java",
                "content": "<full Java file content with \\n for newlines and \\\" for quotes>"
            }
            - The value of "content" must contain only the complete Java source code
            - Every newline in "content" must be escaped as \\n
            - Every internal double quote in "content" must be escaped as \\\"
            - The output must be parseable by Jackson ObjectMapper.readTree()
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, testContextPrompt);

        log.info("Risposta of the {} model to build the TestContext: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, testContextPrompt);
    }

    private String buildLoginSteps() throws InterruptedException, ExecutionException{
        log.info("Building LoginSteps class...");

        String [] partsOfGroupId = groupId.split("\\.");

        String loginStepsPrompt = String.format("""
            Generate the file LoginSteps.java inside package %1$s.%2$s.steps.

            Goal:
            - Generate exactly one Java class named LoginSteps
            - The generated class must match the required structure precisely
            - The generated code must be complete, valid, and compilable Java

            Package and file path:
            - Package: %1$s.%2$s.steps

            Required imports:
            - io.cucumber.java.en.Given
            - io.cucumber.java.en.When
            - io.cucumber.java.en.And
            - io.cucumber.java.en.Then
            - %1$s.%2$s.pages.LoginPage
            - %1$s.%2$s.utility.TestContext
            - static org.junit.jupiter.api.Assertions.assertTrue

            Forbidden imports:
            - %1$s.%2$s.hooks.Hooks
            - %1$s.%2$s.hooks.HooksInterface
            - any DriverFactory import
            - any WebDriver import

            Class structure rules:
            - Create exactly one public class named LoginSteps
            - Declare exactly one field:
            private LoginPage loginPage;
            - Do not declare any other fields
            - Do not declare a hooks field
            - Do not declare a driver field
            - Do not declare a testContext field

            Constructor rules:
            - Add exactly one constructor
            - Constructor signature must be exactly:
            public LoginSteps(TestContext testContext)
            - In the constructor:
            - initialize loginPage with testContext.getPageObjectManager().getLoginPage();
            - Do not add any other constructor
            - Do not instantiate LoginPage directly with new LoginPage(...)
            - Do not use Hooks or HooksInterface in the constructor

            Required step methods:
            - Add exactly these five public methods and no others

            - Method 1:
            - Annotation: @Given("I open the login page {string}")
            - Signature: public void openLoginPage(String url)
            - Body:
                - call loginPage.open(url);
                - call loginPage.get();

            - Method 2:
            - Annotation: @When("I enter username {string}")
            - Signature: public void enterUsername(String username)
            - Body:
                - call loginPage.enterUsername(username);

            - Method 3:
            - Annotation: @And("I enter password {string}")
            - Signature: public void enterPassword(String password)
            - Body:
                - call loginPage.enterPassword(password);

            - Method 4:
            - Annotation: @And("I click the login button")
            - Signature: public void clickLoginButton()
            - Body:
                - call loginPage.submit();

            - Method 5:
            - Annotation: @Then("I verify an error message containing {string}")
            - Signature: public void verifyErrorMessage(String expected)
            - Body:
                - use assertTrue(loginPage.getErrorMessage().contains(expected));

            Behavioral constraints:
            - The class must obtain LoginPage only through TestContext and PageObjectManager
            - The class must not manage WebDriver lifecycle
            - The class must not create or destroy drivers
            - The class must not create DriverFactory
            - The class must not use Hooks directly
            - Keep step definitions thin and delegate behavior to LoginPage
            - Keep the class focused only on step orchestration and assertion

            Exactness constraints:
            - Do not add extra methods
            - Do not add helper methods
            - Do not add comments
            - Do not add annotations other than the five Cucumber step annotations
            - Do not add Lombok annotations
            - Do not add unused imports
            - Do not add blank placeholder methods
            - Do not add example code outside the class

            The generated class must follow this exact shape:
            - public class LoginSteps
            - one field: loginPage
            - one constructor: LoginSteps(TestContext testContext)
            - five methods:
            - openLoginPage
            - enterUsername
            - enterPassword
            - clickLoginButton
            - verifyErrorMessage

            Respond only with a single valid JSON object.

            Strict JSON requirements:
            - Return exactly one JSON object
            - Return the JSON on a single line
            - Do not add markdown fences
            - Do not add explanations
            - Do not add comments
            - Use this exact format:
            {
                "path": "src/test/java/%1$s/%2$s/steps/LoginSteps.java",
                "content": "<full Java file content with \\n for newlines and \\\" for quotes>"
            }
            - The value of "content" must contain only the complete Java source code
            - Every newline in "content" must be escaped as \\n
            - Every internal double quote in "content" must be escaped as \\\"
            - The output must be parseable by Jackson ObjectMapper.readTree()
        """, partsOfGroupId[0], partsOfGroupId[1]);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginStepsPrompt);

        log.info("Risposta of the {} model to build the LoginSteps: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, loginStepsPrompt);
    }

    private String buildLoginFeature() throws InterruptedException, ExecutionException{
        log.info("Building login.feature file...");

        String loginFeaturePrompt = String.format("""
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
                {
                    "path": "src/test/resources/features/login.feature",
                    "content": "<full file content with \\n for newlines and all internal quotes escaped>"
                }
            - Every newline inside "content" must be encoded as \\n (two backslashes + n).
            - Every double quote inside "content" must be escaped as \\\".
            - The JSON must be on one single line (no pretty-printing, no indentation).
            - Do not add any explanations, comments, or text before or after the JSON.
            - Output must be strictly valid JSON that can be parsed by com.fasterxml.jackson.databind.ObjectMapper.readTree().
        """);

        String responseAddGptOss = copilotService.getResponseCopilotWhitOutStreaming(modelName, loginFeaturePrompt);

        log.info("Risposta of the {} model to build the login.feature: {}", modelName, responseAddGptOss);
        return validateAndCleanJson(responseAddGptOss, loginFeaturePrompt);
    }

}
