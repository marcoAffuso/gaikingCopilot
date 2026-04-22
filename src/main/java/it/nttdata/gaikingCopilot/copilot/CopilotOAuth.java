package it.nttdata.gaikingCopilot.copilot;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import it.nttdata.gaikingCopilot.utility.ReadAndWriteJson;
import it.nttdata.gaikingCopilot.utility.RestApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class CopilotOAuth {

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${app.base-url}")
    private String baseUrl;

    private final ReadAndWriteJson readAndWriteJson;

    private final GitHubTokenSessionService gitHubTokenSessionService;

    public String buildAuthorizationUrl(WebSession session) {
        String state = UUID.randomUUID().toString();
        session.getAttributes().put("github_oauth_state", state);

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", baseUrl + "/api/copilot/auth/callback")
                .queryParam("state", state)
                .build()
                .toUriString();

        log.info("Redirecting to GitHub OAuth: {}", authorizationUrl);
        return authorizationUrl;
    }

    public String exchangeAuthorizationCodeForAccessToken(String code, String state, WebSession session) {
        String expectedState = gitHubTokenSessionService.getRequiredOAuthState(session);

        if (!expectedState.equals(state)) {
            throw new IllegalArgumentException("State OAuth non valido");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("redirect_uri", baseUrl + "/api/copilot/auth/callback");
        formData.add("state", state);

        RestApi oauthApi = new RestApi();
        oauthApi.setUrl("https://github.com/login/oauth/access_token");
        oauthApi.setHeaders(headers);
        oauthApi.setQueryParams(new LinkedMultiValueMap<>());
        oauthApi.setPayloadMap(formData);
        oauthApi.requestPostUrlEncoded();

        if (oauthApi.getStatusCode() != 200) {
            throw new IllegalStateException(
                    "GitHub OAuth ha restituito status " + oauthApi.getStatusCode() + " con body: " + oauthApi.getResponse()
            );
        }

        try {
            JsonNode responseJson = this.readAndWriteJson.readJsonNode(oauthApi.getResponse());

            if (responseJson == null) {
                throw new IllegalStateException("Risposta GitHub non parsabile: " + oauthApi.getResponse());
            }

            if (responseJson.hasNonNull("error")) {
                String error = responseJson.path("error").asText();
                String errorDescription = responseJson.path("error_description").asText();
                throw new IllegalStateException("Errore GitHub OAuth: " + error + " - " + errorDescription);
            }

            String accessToken = responseJson.path("access_token").asText();

            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("Access token assente nella risposta GitHub: " + oauthApi.getResponse());
            }

            gitHubTokenSessionService.storeAccessToken(session, accessToken);
            gitHubTokenSessionService.clearOAuthState(session);

            log.info("GitHub access token ottenuto correttamente");
            return accessToken;

        } catch (Exception ex) {
            log.error("Errore parsing risposta OAuth. status={}, body={}", oauthApi.getStatusCode(), oauthApi.getResponse(), ex);
            throw new IllegalStateException("Errore durante il parsing della risposta GitHub OAuth", ex);
        }
    }


    public void revokeApplicationGrant(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token mancante");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-GitHub-Api-Version", "2022-11-28");

        String payload = this.readAndWriteJson.fromObjectToJsonString(Map.of("access_token", accessToken));        

        RestApi revokeApi = new RestApi();
        revokeApi.setUrl("https://api.github.com/applications/" + clientId + "/grant");
        revokeApi.setHeaders(headers);
        revokeApi.setQueryParams(new LinkedMultiValueMap<>());
        revokeApi.setPayload(payload);
        revokeApi.setUsername(clientId);
        revokeApi.setPassword(clientSecret);
        revokeApi.requestDeleteWithHeadersAndParameters();

        if (revokeApi.getStatusCode() != 204) {
            throw new IllegalStateException(
                    "Revoca GitHub fallita. status=" + revokeApi.getStatusCode()
                            + ", body=" + revokeApi.getResponse()
            );
        }

        log.info("GitHub OAuth grant revocato correttamente");
    }

}
