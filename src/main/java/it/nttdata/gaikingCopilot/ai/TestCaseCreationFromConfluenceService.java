package it.nttdata.gaikingCopilot.ai;

import org.springframework.stereotype.Service;

import it.nttdata.gaikingCopilot.model.ConfluenceRequestParameters;
import it.nttdata.gaikingCopilot.service.confluence.RequirementFromConfluence;
import it.nttdata.gaikingCopilot.service.copilot.CopilotService;
import it.nttdata.gaikingCopilot.utility.JsonToExcelConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Service
@RequiredArgsConstructor
public class TestCaseCreationFromConfluenceService {

    private final RequirementFromConfluence requirementFromConfluence;
    private final CopilotService copilotService;
    private final JsonToExcelConverter jsonToExcelConverter;


    public Mono<Void> fromConflunceToStepByStepTestCase(ConfluenceRequestParameters confluenceRequestParameters, String githubToken) {
        log.info("Generating test case from Confluence content.");

        String promptGenerateTestCase = """
            You are a senior software tester with expertise in generating test cases from requirements.
        """;

        return requirementFromConfluence.getRequirementFromConfluence(
                confluenceRequestParameters.confluenceUrl(),
                confluenceRequestParameters.spaceKey(),
                confluenceRequestParameters.pageTitle()
            )
            .doOnNext(requirements -> log.info("Requirements extracted from Confluence: {}", requirements))
            .flatMap(requirements -> Mono.fromCallable(() -> {
                String responseCopilJsonString = copilotService.getResponseCopilotWithStreaming(
                    githubToken,
                    confluenceRequestParameters.model(),
                    confluenceRequestParameters.reasoningEffort(),
                    promptGenerateTestCase
                );

                log.info("Risposta of the {} model to build the pom.xml: {}", confluenceRequestParameters.model(), responseCopilJsonString);

                this.jsonToExcelConverter.convertJsonToExcel(responseCopilJsonString, confluenceRequestParameters.outputDir());
                return responseCopilJsonString;
            }).subscribeOn(Schedulers.boundedElastic()))
            .then();
    }    



}
