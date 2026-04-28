package it.nttdata.gaikingCopilot.service.confluence;


import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import it.nttdata.gaikingCopilot.utility.ReadAndWriteJson;
import it.nttdata.gaikingCopilot.utility.LlmSanitizer;
import it.nttdata.gaikingCopilot.utility.RestApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
@Component
public class RequirementFromConfluence {

    @Value("${confluence.username}")
    private String confluenceUsername;

    @Value("${confluence.api.url}")
    private String apiUrl;

    @Value("${confluence.api.token}")
    private String confluenceApiToken;

    @Value("${confluence.start.page}")
    private String confluenceStartPage;

    @Value("${confluence.limit.page}")
    private String confluenceLimitPage;

    private final ReadAndWriteJson readAndWriteJson;

    private final LlmSanitizer llmSanitizer;

    

    public Mono<String> getRequirementFromConfluence(String confluenceUrl, String spaceKey, String pageTitle) {
        log.info("requirementFromConfluence CALLED");
        String url = confluenceUrl + apiUrl;
        return downloadRequiremntsFromConfluencePage(url, spaceKey, pageTitle)
            .map(this.llmSanitizer::sanitizeConfluenceForLlm)
            .doOnNext(cleanedRequirements -> log.info("Cleaned Requirements: {}", cleanedRequirements));
    }


    private Mono<String> downloadRequiremntsFromConfluencePage(String url, String spaceKey, String pageTitle) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        MultiValueMap<String, String> queryParamsPage = new LinkedMultiValueMap<>();
        queryParamsPage.add("spaceKey", spaceKey);
        queryParamsPage.add("title", pageTitle);
        queryParamsPage.add("start", confluenceStartPage);
        queryParamsPage.add("limit", confluenceLimitPage);
        return buildConfluenceRestApi(url, headers, queryParamsPage)
            .requestGetWithHeadersAndParameters()
            .map(responseEntity -> extractSuccessfulResponseBody(
                responseEntity,
                "Failed to retrieve page information from Confluence."
            ))
            .map(this.readAndWriteJson::readJsonNode)
            .map(jsonResponse -> extractPageId(jsonResponse, pageTitle))
            .flatMap(idPage -> {
                MultiValueMap<String, String> queryParamsReq = new LinkedMultiValueMap<>();
                queryParamsReq.add("expand", "body.storage");

                return buildConfluenceRestApi(url + "/" + idPage, headers, queryParamsReq)
                    .requestGetWithHeadersAndParameters();
            })
            .map(responseEntity -> extractSuccessfulResponseBody(
                responseEntity,
                "Failed to retrieve requirements from Confluence page."
            ))
            .map(responseBody -> {
                JsonNode jsonResponseReq = this.readAndWriteJson.readJsonNode(responseBody);
                String requirements = jsonResponseReq.path("body").path("storage").path("value").asText();
                log.info("Extracted Requirements: {}", requirements);
                return requirements;
            });
    }

    private RestApi buildConfluenceRestApi(String url, HttpHeaders headers, MultiValueMap<String, String> queryParams) {
        RestApi restApi = new RestApi();
        restApi.setUrl(url);
        restApi.setHeaders(headers);
        restApi.setQueryParams(queryParams);
        restApi.setUsername(confluenceUsername);
        restApi.setPassword(confluenceApiToken);
        return restApi;
    }

    private String extractSuccessfulResponseBody(ResponseEntity<String> responseEntity, String errorMessage) {
        int statusCode = responseEntity.getStatusCode().value();
        String responseBody = responseEntity.getBody();
        log.info("Confluence API Response Status Code: {}", statusCode);
        log.info("Confluence API Response: {}", responseBody);

        if (statusCode != 200) {
            log.warn("{} Status Code: {}, Response: {}", errorMessage, statusCode, responseBody);
            throw new IllegalStateException(errorMessage + " Status Code: " + statusCode);
        }

        return responseBody;
    }

    private String extractPageId(JsonNode jsonResponse, String pageTitle) {
        for (JsonNode element : jsonResponse.path("results")) {
            String title = element.path("title").asText();
            log.info("Page Title: {}", title);
            if (pageTitle.equalsIgnoreCase(title)) {
                String idPage = element.path("id").asText();
                log.info("Page ID: {}", idPage);
                return idPage;
            }
        }

        log.warn("Page with title '{}' not found in Confluence response", pageTitle);
        throw new IllegalStateException("Page with title '" + pageTitle + "' not found in Confluence response");
    }


}
