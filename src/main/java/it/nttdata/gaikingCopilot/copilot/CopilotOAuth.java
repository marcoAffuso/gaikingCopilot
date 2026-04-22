package it.nttdata.gaikingCopilot.copilot;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.WebSession;

import com.fasterxml.jackson.databind.JsonNode;

import it.nttdata.gaikingCopilot.utility.ReadAndWriteJson;
import it.nttdata.gaikingCopilot.utility.RestApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class CopilotOAuth {

    private static final String GITHUB_DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String GITHUB_ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String DEVICE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";
    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 900L;

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.scope:}")
    private String oauthScope;

    private final ReadAndWriteJson readAndWriteJson;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    public DeviceAuthorizationStart startDeviceAuthorization(WebSession session) {
        gitHubTokenSessionService.clearAll(session);

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);

        if (oauthScope != null && !oauthScope.isBlank()) {
            formData.add("scope", oauthScope);
        }

        GitHubFormResponse githubResponse = executeGithubFormPost(
                GITHUB_DEVICE_CODE_URL,
                formData,
                "avvio del GitHub Device Flow"
        );

        if (githubResponse.statusCode() != 200) {
            throw buildGithubException("avvio del GitHub Device Flow", githubResponse);
        }

        JsonNode responseJson = githubResponse.body();

        if (responseJson.hasNonNull("error")) {
            throw buildGithubException("avvio del GitHub Device Flow", githubResponse);
        }

        String deviceCode = getRequiredText(responseJson, "device_code", "avvio del GitHub Device Flow");
        String userCode = getRequiredText(responseJson, "user_code", "avvio del GitHub Device Flow");
        String verificationUri = getRequiredText(responseJson, "verification_uri", "avvio del GitHub Device Flow");

        int pollIntervalSeconds = getPositiveInt(
                responseJson,
                "interval",
                DEFAULT_POLL_INTERVAL_SECONDS
        );

        long expiresInSeconds = getPositiveLong(
                responseJson,
                "expires_in",
                DEFAULT_EXPIRES_IN_SECONDS
        );

        long expiresAtEpochMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L);

        gitHubTokenSessionService.storeDeviceAuthorization(
                session,
                deviceCode,
                userCode,
                pollIntervalSeconds,
                expiresAtEpochMillis
        );

        log.info(
                "GitHub Device Flow avviato correttamente. userCode={}, interval={}, expiresIn={}",
                userCode,
                pollIntervalSeconds,
                expiresInSeconds
        );

        return new DeviceAuthorizationStart(
                userCode,
                verificationUri,
                expiresInSeconds,
                pollIntervalSeconds,
                "Apri GitHub, inserisci il codice mostrato e attendi il completamento."
        );
    }

    public DeviceAuthorizationPollResult pollDeviceAuthorization(WebSession session) {
        String existingAccessToken = gitHubTokenSessionService.getOptionalAccessToken(session);

        if (existingAccessToken != null && !existingAccessToken.isBlank()) {
            return new DeviceAuthorizationPollResult(
                    "authorized",
                    "Autenticazione GitHub completata correttamente.",
                    true,
                    null
            );
        }

        String deviceCode = gitHubTokenSessionService.getRequiredDeviceCode(session);
        int currentPollIntervalSeconds = gitHubTokenSessionService.getRequiredDeviceInterval(session);
        long expiresAtEpochMillis = gitHubTokenSessionService.getRequiredDeviceExpiresAt(session);

        if (System.currentTimeMillis() >= expiresAtEpochMillis) {
            gitHubTokenSessionService.clearDeviceAuthorization(session);

            return new DeviceAuthorizationPollResult(
                    "expired",
                    "Codice GitHub scaduto. Riavvia il login.",
                    false,
                    null
            );
        }

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("device_code", deviceCode);
        formData.add("grant_type", DEVICE_GRANT_TYPE);

        GitHubFormResponse githubResponse = executeGithubFormPost(
                GITHUB_ACCESS_TOKEN_URL,
                formData,
                "polling del GitHub Device Flow"
        );

        JsonNode responseJson = githubResponse.body();

        String accessToken = responseJson.path("access_token").asText(null);
        if (accessToken != null && !accessToken.isBlank()) {
            gitHubTokenSessionService.storeAccessToken(session, accessToken);
            gitHubTokenSessionService.clearDeviceAuthorization(session);

            log.info("GitHub access token ottenuto correttamente tramite Device Flow");

            return new DeviceAuthorizationPollResult(
                    "authorized",
                    "Autenticazione GitHub completata correttamente.",
                    true,
                    null
            );
        }

        String error = responseJson.path("error").asText("");
        String errorDescription = responseJson.path("error_description").asText("");

        if (!error.isBlank()) {
            switch (error) {
                case "authorization_pending":
                    return new DeviceAuthorizationPollResult(
                            "pending",
                            "Autorizzazione in attesa su GitHub.",
                            false,
                            currentPollIntervalSeconds
                    );

                case "slow_down":
                    int updatedPollIntervalSeconds = currentPollIntervalSeconds + 5;
                    gitHubTokenSessionService.updateDeviceInterval(session, updatedPollIntervalSeconds);

                    return new DeviceAuthorizationPollResult(
                            "pending",
                            "GitHub richiede di rallentare il polling.",
                            false,
                            updatedPollIntervalSeconds
                    );

                case "access_denied":
                    gitHubTokenSessionService.clearDeviceAuthorization(session);

                    return new DeviceAuthorizationPollResult(
                            "denied",
                            "Autorizzazione negata dall'utente su GitHub.",
                            false,
                            null
                    );

                case "expired_token":
                    gitHubTokenSessionService.clearDeviceAuthorization(session);

                    return new DeviceAuthorizationPollResult(
                            "expired",
                            "Codice GitHub scaduto. Riavvia il login.",
                            false,
                            null
                    );

                case "device_flow_disabled":
                    gitHubTokenSessionService.clearDeviceAuthorization(session);

                    return new DeviceAuthorizationPollResult(
                            "error",
                            "Device Flow non abilitato nella GitHub OAuth App.",
                            false,
                            null
                    );

                default:
                    throw new IllegalStateException(
                            "Errore GitHub Device Flow: " + error
                                    + (errorDescription != null && !errorDescription.isBlank() ? " - " + errorDescription : "")
                    );
            }
        }

        if (githubResponse.statusCode() != 200) {
            throw buildGithubException("polling del GitHub Device Flow", githubResponse);
        }

        throw new IllegalStateException(
                "Risposta GitHub inattesa durante il polling del Device Flow: " + githubResponse.rawBody()
        );
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

    private HttpHeaders buildGithubJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private GitHubFormResponse executeGithubFormPost(
            String url,
            LinkedMultiValueMap<String, String> formData,
            String operationName
    ) {
        RestApi api = new RestApi();
        api.setUrl(url);
        api.setHeaders(buildGithubJsonHeaders());
        api.setQueryParams(new LinkedMultiValueMap<>());
        api.setPayloadMap(formData);
        api.requestPostUrlEncoded();

        String rawBody = api.getResponse();

        if (rawBody == null || rawBody.isBlank()) {
            throw new IllegalStateException(
                    "GitHub ha restituito una risposta vuota durante " + operationName
                            + ". status=" + api.getStatusCode()
            );
        }

        try {
            JsonNode responseJson = readAndWriteJson.readJsonNode(rawBody);

            if (responseJson == null) {
                throw new IllegalStateException(
                        "Risposta GitHub non parsabile durante " + operationName + ": " + rawBody
                );
            }

            return new GitHubFormResponse(api.getStatusCode(), responseJson, rawBody);

        } catch (Exception ex) {
            log.error(
                    "Errore parsing risposta GitHub. operation={}, status={}, body={}",
                    operationName,
                    api.getStatusCode(),
                    rawBody,
                    ex
            );

            throw new IllegalStateException(
                    "Errore durante il parsing della risposta GitHub in " + operationName,
                    ex
            );
        }
    }

    private IllegalStateException buildGithubException(String operationName, GitHubFormResponse githubResponse) {
        String githubMessage = extractGithubErrorMessage(githubResponse.body());

        return new IllegalStateException(
                "GitHub ha restituito un errore durante " + operationName
                        + ". status=" + githubResponse.statusCode()
                        + ", message=" + githubMessage
        );
    }

    private String extractGithubErrorMessage(JsonNode responseJson) {
        String error = responseJson.path("error").asText("");
        String errorDescription = responseJson.path("error_description").asText("");

        if (error != null && !error.isBlank()) {
            if (errorDescription != null && !errorDescription.isBlank()) {
                return error + " - " + errorDescription;
            }
            return error;
        }

        return responseJson.toString();
    }

    private String getRequiredText(JsonNode jsonNode, String fieldName, String operationName) {
        String value = jsonNode.path(fieldName).asText(null);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Campo obbligatorio mancante nella risposta GitHub durante "
                            + operationName
                            + ": "
                            + fieldName
            );
        }

        return value;
    }

    private int getPositiveInt(JsonNode jsonNode, String fieldName, int defaultValue) {
        int value = jsonNode.path(fieldName).asInt(defaultValue);
        return value > 0 ? value : defaultValue;
    }

    private long getPositiveLong(JsonNode jsonNode, String fieldName, long defaultValue) {
        long value = jsonNode.path(fieldName).asLong(defaultValue);
        return value > 0 ? value : defaultValue;
    }

    public record DeviceAuthorizationStart(
            String userCode,
            String verificationUri,
            long expiresInSeconds,
            int pollIntervalSeconds,
            String message
    ) {
    }

    public record DeviceAuthorizationPollResult(
            String status,
            String message,
            boolean authenticated,
            Integer pollAfterSeconds
    ) {
    }

    private record GitHubFormResponse(
            int statusCode,
            JsonNode body,
            String rawBody
    ) {
    }

}
