package it.nttdata.gaikingCopilot.service.copilot;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;

@Service
public class GitHubTokenSessionService {

    public static final String GITHUB_ACCESS_TOKEN_KEY = "github_access_token";
    public static final String GITHUB_OAUTH_STATE_KEY = "github_oauth_state";

    public static final String GITHUB_DEVICE_CODE_KEY = "github_device_code";
    public static final String GITHUB_DEVICE_USER_CODE_KEY = "github_device_user_code";
    public static final String GITHUB_DEVICE_INTERVAL_KEY = "github_device_interval";
    public static final String GITHUB_DEVICE_EXPIRES_AT_KEY = "github_device_expires_at";

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

    public void storeDeviceAuthorization(
            WebSession session,
            String deviceCode,
            String userCode,
            int pollIntervalSeconds,
            long expiresAtEpochMillis
    ) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("GitHub device code mancante");
        }

        if (userCode == null || userCode.isBlank()) {
            throw new IllegalArgumentException("GitHub user code mancante");
        }

        if (pollIntervalSeconds <= 0) {
            throw new IllegalArgumentException("Intervallo di polling non valido");
        }

        if (expiresAtEpochMillis <= 0) {
            throw new IllegalArgumentException("Scadenza device flow non valida");
        }

        session.getAttributes().put(GITHUB_DEVICE_CODE_KEY, deviceCode);
        session.getAttributes().put(GITHUB_DEVICE_USER_CODE_KEY, userCode);
        session.getAttributes().put(GITHUB_DEVICE_INTERVAL_KEY, pollIntervalSeconds);
        session.getAttributes().put(GITHUB_DEVICE_EXPIRES_AT_KEY, expiresAtEpochMillis);
    }

    public String getRequiredDeviceCode(WebSession session) {
        Object deviceCode = session.getAttributes().get(GITHUB_DEVICE_CODE_KEY);

        if (deviceCode == null || deviceCode.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub device code non presente in sessione"
            );
        }

        return deviceCode.toString();
    }

    public String getOptionalDeviceCode(WebSession session) {
        Object deviceCode = session.getAttributes().get(GITHUB_DEVICE_CODE_KEY);
        return deviceCode != null ? deviceCode.toString() : null;
    }

    public String getRequiredDeviceUserCode(WebSession session) {
        Object userCode = session.getAttributes().get(GITHUB_DEVICE_USER_CODE_KEY);

        if (userCode == null || userCode.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub user code non presente in sessione"
            );
        }

        return userCode.toString();
    }

    public int getRequiredDeviceInterval(WebSession session) {
        Object interval = session.getAttributes().get(GITHUB_DEVICE_INTERVAL_KEY);

        if (interval == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Intervallo di polling GitHub non presente in sessione"
            );
        }

        if (interval instanceof Number numberValue) {
            int pollIntervalSeconds = numberValue.intValue();

            if (pollIntervalSeconds <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Intervallo di polling GitHub non valido"
                );
            }

            return pollIntervalSeconds;
        }

        try {
            int pollIntervalSeconds = Integer.parseInt(interval.toString());

            if (pollIntervalSeconds <= 0) {
                throw new NumberFormatException("Intervallo <= 0");
            }

            return pollIntervalSeconds;

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Intervallo di polling GitHub non valido"
            );
        }
    }

    public long getRequiredDeviceExpiresAt(WebSession session) {
        Object expiresAt = session.getAttributes().get(GITHUB_DEVICE_EXPIRES_AT_KEY);

        if (expiresAt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Scadenza GitHub device flow non presente in sessione"
            );
        }

        if (expiresAt instanceof Number numberValue) {
            long expiresAtEpochMillis = numberValue.longValue();

            if (expiresAtEpochMillis <= 0L) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Scadenza GitHub device flow non valida"
                );
            }

            return expiresAtEpochMillis;
        }

        try {
            long expiresAtEpochMillis = Long.parseLong(expiresAt.toString());

            if (expiresAtEpochMillis <= 0L) {
                throw new NumberFormatException("Scadenza <= 0");
            }

            return expiresAtEpochMillis;

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Scadenza GitHub device flow non valida"
            );
        }
    }

    public void updateDeviceInterval(WebSession session, int pollIntervalSeconds) {
        if (pollIntervalSeconds <= 0) {
            throw new IllegalArgumentException("Intervallo di polling non valido");
        }

        session.getAttributes().put(GITHUB_DEVICE_INTERVAL_KEY, pollIntervalSeconds);
    }

    public boolean hasActiveDeviceAuthorization(WebSession session) {
        String deviceCode = getOptionalDeviceCode(session);

        if (deviceCode == null || deviceCode.isBlank()) {
            return false;
        }

        Object expiresAt = session.getAttributes().get(GITHUB_DEVICE_EXPIRES_AT_KEY);
        if (expiresAt == null) {
            return false;
        }

        try {
            long expiresAtEpochMillis = expiresAt instanceof Number numberValue
                    ? numberValue.longValue()
                    : Long.parseLong(expiresAt.toString());

            return expiresAtEpochMillis > System.currentTimeMillis();

        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public void clearDeviceAuthorization(WebSession session) {
        session.getAttributes().remove(GITHUB_DEVICE_CODE_KEY);
        session.getAttributes().remove(GITHUB_DEVICE_USER_CODE_KEY);
        session.getAttributes().remove(GITHUB_DEVICE_INTERVAL_KEY);
        session.getAttributes().remove(GITHUB_DEVICE_EXPIRES_AT_KEY);
    }

    public void clearAccessToken(WebSession session) {
        session.getAttributes().remove(GITHUB_ACCESS_TOKEN_KEY);
    }

    public void clearAll(WebSession session) {
        clearOAuthState(session);
        clearDeviceAuthorization(session);
        clearAccessToken(session);
    }

    public boolean isAuthenticated(WebSession session) {
        String token = getOptionalAccessToken(session);
        return token != null && !token.isBlank();
    }

}
