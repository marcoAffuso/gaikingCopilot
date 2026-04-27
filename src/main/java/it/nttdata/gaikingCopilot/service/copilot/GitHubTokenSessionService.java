package it.nttdata.gaikingCopilot.service.copilot;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;

import lombok.extern.log4j.Log4j2;

@Log4j2
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
            log.warn("Cannot store GitHub access token because it is missing. sessionId={}", sessionId(session));
            throw new IllegalArgumentException("GitHub access token mancante");
        }

        session.getAttributes().put(GITHUB_ACCESS_TOKEN_KEY, accessToken);
        log.info("GitHub access token stored in session. sessionId={}", sessionId(session));
    }

    public String getRequiredAccessToken(WebSession session) {
        Object token = session.getAttributes().get(GITHUB_ACCESS_TOKEN_KEY);

        if (token == null) {
            log.warn("GitHub access token not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utente non autenticato con GitHub"
            );
        }

        String accessToken = token.toString();

        if (accessToken.isBlank()) {
            log.warn("GitHub access token is blank in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub access token non valido"
            );
        }

        log.debug("GitHub access token retrieved from session. sessionId={}", sessionId(session));
        return accessToken;
    }

    public String getOptionalAccessToken(WebSession session) {
        Object token = session.getAttributes().get(GITHUB_ACCESS_TOKEN_KEY);
        return token != null ? token.toString() : null;
    }

    public void storeOAuthState(WebSession session, String state) {
        if (state == null || state.isBlank()) {
            log.warn("Cannot store GitHub OAuth state because it is missing. sessionId={}", sessionId(session));
            throw new IllegalArgumentException("OAuth state mancante");
        }

        session.getAttributes().put(GITHUB_OAUTH_STATE_KEY, state);
        log.debug("GitHub OAuth state stored in session. sessionId={}", sessionId(session));
    }

    public String getRequiredOAuthState(WebSession session) {
        Object state = session.getAttributes().get(GITHUB_OAUTH_STATE_KEY);

        if (state == null || state.toString().isBlank()) {
            log.warn("GitHub OAuth state not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OAuth state non presente in sessione"
            );
        }

        log.debug("GitHub OAuth state retrieved from session. sessionId={}", sessionId(session));
        return state.toString();
    }

    public void clearOAuthState(WebSession session) {
        session.getAttributes().remove(GITHUB_OAUTH_STATE_KEY);
        log.debug("GitHub OAuth state cleared from session. sessionId={}", sessionId(session));
    }

    public void storeDeviceAuthorization(
            WebSession session,
            String deviceCode,
            String userCode,
            int pollIntervalSeconds,
            long expiresAtEpochMillis
    ) {
        if (deviceCode == null || deviceCode.isBlank()) {
            log.warn("Cannot store GitHub device authorization because device code is missing. sessionId={}", sessionId(session));
            throw new IllegalArgumentException("GitHub device code mancante");
        }

        if (userCode == null || userCode.isBlank()) {
            log.warn("Cannot store GitHub device authorization because user code is missing. sessionId={}", sessionId(session));
            throw new IllegalArgumentException("GitHub user code mancante");
        }

        if (pollIntervalSeconds <= 0) {
            log.warn("Cannot store GitHub device authorization because polling interval is invalid. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
            throw new IllegalArgumentException("Intervallo di polling non valido");
        }

        if (expiresAtEpochMillis <= 0) {
            log.warn("Cannot store GitHub device authorization because expiry is invalid. sessionId={}, expiresAtEpochMillis={}", sessionId(session), expiresAtEpochMillis);
            throw new IllegalArgumentException("Scadenza device flow non valida");
        }

        session.getAttributes().put(GITHUB_DEVICE_CODE_KEY, deviceCode);
        session.getAttributes().put(GITHUB_DEVICE_USER_CODE_KEY, userCode);
        session.getAttributes().put(GITHUB_DEVICE_INTERVAL_KEY, pollIntervalSeconds);
        session.getAttributes().put(GITHUB_DEVICE_EXPIRES_AT_KEY, expiresAtEpochMillis);
        log.info(
                "GitHub device authorization stored in session. sessionId={}, pollIntervalSeconds={}, expiresAtEpochMillis={}",
                sessionId(session),
                pollIntervalSeconds,
                expiresAtEpochMillis
        );
    }

    public String getRequiredDeviceCode(WebSession session) {
        Object deviceCode = session.getAttributes().get(GITHUB_DEVICE_CODE_KEY);

        if (deviceCode == null || deviceCode.toString().isBlank()) {
            log.warn("GitHub device code not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub device code non presente in sessione"
            );
        }

        log.debug("GitHub device code retrieved from session. sessionId={}", sessionId(session));
        return deviceCode.toString();
    }

    public String getOptionalDeviceCode(WebSession session) {
        Object deviceCode = session.getAttributes().get(GITHUB_DEVICE_CODE_KEY);
        return deviceCode != null ? deviceCode.toString() : null;
    }

    public String getRequiredDeviceUserCode(WebSession session) {
        Object userCode = session.getAttributes().get(GITHUB_DEVICE_USER_CODE_KEY);

        if (userCode == null || userCode.toString().isBlank()) {
            log.warn("GitHub user code not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub user code non presente in sessione"
            );
        }

        log.debug("GitHub user code retrieved from session. sessionId={}", sessionId(session));
        return userCode.toString();
    }

    public int getRequiredDeviceInterval(WebSession session) {
        Object interval = session.getAttributes().get(GITHUB_DEVICE_INTERVAL_KEY);

        if (interval == null) {
            log.warn("GitHub polling interval not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Intervallo di polling GitHub non presente in sessione"
            );
        }

        if (interval instanceof Number numberValue) {
            int pollIntervalSeconds = numberValue.intValue();

            if (pollIntervalSeconds <= 0) {
                log.warn("GitHub polling interval in session is invalid. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Intervallo di polling GitHub non valido"
                );
            }

            log.debug("GitHub polling interval retrieved from session. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
            return pollIntervalSeconds;
        }

        try {
            int pollIntervalSeconds = Integer.parseInt(interval.toString());

            if (pollIntervalSeconds <= 0) {
                throw new NumberFormatException("Intervallo <= 0");
            }

            log.debug("GitHub polling interval parsed from session. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
            return pollIntervalSeconds;

        } catch (NumberFormatException ex) {
            log.warn("GitHub polling interval in session is not parseable. sessionId={}, rawValue={}", sessionId(session), interval);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Intervallo di polling GitHub non valido"
            );
        }
    }

    public long getRequiredDeviceExpiresAt(WebSession session) {
        Object expiresAt = session.getAttributes().get(GITHUB_DEVICE_EXPIRES_AT_KEY);

        if (expiresAt == null) {
            log.warn("GitHub device flow expiry not found in session. sessionId={}", sessionId(session));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Scadenza GitHub device flow non presente in sessione"
            );
        }

        if (expiresAt instanceof Number numberValue) {
            long expiresAtEpochMillis = numberValue.longValue();

            if (expiresAtEpochMillis <= 0L) {
                log.warn("GitHub device flow expiry in session is invalid. sessionId={}, expiresAtEpochMillis={}", sessionId(session), expiresAtEpochMillis);
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Scadenza GitHub device flow non valida"
                );
            }

            log.debug("GitHub device flow expiry retrieved from session. sessionId={}, expiresAtEpochMillis={}", sessionId(session), expiresAtEpochMillis);
            return expiresAtEpochMillis;
        }

        try {
            long expiresAtEpochMillis = Long.parseLong(expiresAt.toString());

            if (expiresAtEpochMillis <= 0L) {
                throw new NumberFormatException("Scadenza <= 0");
            }

            log.debug("GitHub device flow expiry parsed from session. sessionId={}, expiresAtEpochMillis={}", sessionId(session), expiresAtEpochMillis);
            return expiresAtEpochMillis;

        } catch (NumberFormatException ex) {
            log.warn("GitHub device flow expiry in session is not parseable. sessionId={}, rawValue={}", sessionId(session), expiresAt);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Scadenza GitHub device flow non valida"
            );
        }
    }

    public void updateDeviceInterval(WebSession session, int pollIntervalSeconds) {
        if (pollIntervalSeconds <= 0) {
            log.warn("Cannot update GitHub polling interval because it is invalid. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
            throw new IllegalArgumentException("Intervallo di polling non valido");
        }

        session.getAttributes().put(GITHUB_DEVICE_INTERVAL_KEY, pollIntervalSeconds);
        log.info("GitHub polling interval updated in session. sessionId={}, pollIntervalSeconds={}", sessionId(session), pollIntervalSeconds);
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
        log.debug("GitHub device authorization cleared from session. sessionId={}", sessionId(session));
    }

    public void clearAccessToken(WebSession session) {
        session.getAttributes().remove(GITHUB_ACCESS_TOKEN_KEY);
        log.info("GitHub access token cleared from session. sessionId={}", sessionId(session));
    }

    public void clearAll(WebSession session) {
        clearOAuthState(session);
        clearDeviceAuthorization(session);
        clearAccessToken(session);
        log.info("GitHub authentication state fully cleared from session. sessionId={}", sessionId(session));
    }

    public boolean isAuthenticated(WebSession session) {
        String token = getOptionalAccessToken(session);
        return token != null && !token.isBlank();
    }

    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }

}
