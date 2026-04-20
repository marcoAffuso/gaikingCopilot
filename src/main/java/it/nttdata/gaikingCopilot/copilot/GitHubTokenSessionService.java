package it.nttdata.gaikingCopilot.copilot;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;

@Service
public class GitHubTokenSessionService {

    public static final String GITHUB_ACCESS_TOKEN_KEY = "github_access_token";
    public static final String GITHUB_OAUTH_STATE_KEY = "github_oauth_state";

    public void storeAccessToken(WebSession session, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token mancante");
        }

        session.getAttributes().put(GITHUB_ACCESS_TOKEN_KEY, accessToken);
    }

    public String getRequiredAccessToken(WebSession session) {
        Object token = session.getAttributes().get(GITHUB_ACCESS_TOKEN_KEY);

        if (token == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utente non autenticato con GitHub"
            );
        }

        String accessToken = token.toString();

        if (accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub access token non valido"
            );
        }

        return accessToken;
    }

    public String getOptionalAccessToken(WebSession session) {
        Object token = session.getAttributes().get(GITHUB_ACCESS_TOKEN_KEY);
        return token != null ? token.toString() : null;
    }

    public void storeOAuthState(WebSession session, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state mancante");
        }

        session.getAttributes().put(GITHUB_OAUTH_STATE_KEY, state);
    }

    public String getRequiredOAuthState(WebSession session) {
        Object state = session.getAttributes().get(GITHUB_OAUTH_STATE_KEY);

        if (state == null || state.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OAuth state non presente in sessione"
            );
        }

        return state.toString();
    }

    public void clearOAuthState(WebSession session) {
        session.getAttributes().remove(GITHUB_OAUTH_STATE_KEY);
    }

    public void clearAccessToken(WebSession session) {
        session.getAttributes().remove(GITHUB_ACCESS_TOKEN_KEY);
    }

    public void clearAll(WebSession session) {
        clearOAuthState(session);
        clearAccessToken(session);
    }

    public boolean isAuthenticated(WebSession session) {
        String token = getOptionalAccessToken(session);
        return token != null && !token.isBlank();
    }

}
