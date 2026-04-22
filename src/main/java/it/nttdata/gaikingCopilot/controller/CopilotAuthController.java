package it.nttdata.gaikingCopilot.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.copilot.CopilotOAuth;
import it.nttdata.gaikingCopilot.copilot.GitHubTokenSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@RestController
@RequestMapping("/api/copilot/auth")
@RequiredArgsConstructor
public class CopilotAuthController {


    private static final String LOGIN_HTML = """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Autenticazione GitHub</title>
                <style>
                    body {
                        font-family: sans-serif;
                        max-width: 720px;
                        margin: 40px auto;
                        padding: 0 16px;
                        line-height: 1.5;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        letter-spacing: 3px;
                        margin: 16px 0;
                    }
                    .hidden {
                        display: none;
                    }
                </style>
            </head>
            <body>
                <h1>Autenticazione GitHub</h1>
                <p id="message">Preparazione del codice di accesso...</p>

                <div id="deviceBox" class="hidden">
                    <p>Apri questa pagina:</p>
                    <p>
                        <a id="verifyLink" href="#" target="_blank" rel="noopener noreferrer"></a>
                    </p>
                    <p>e inserisci questo codice:</p>
                    <div id="userCode" class="code"></div>
                </div>

                <script>
                    let pollTimer = null;

                    function stopPolling() {
                        if (pollTimer) {
                            clearTimeout(pollTimer);
                            pollTimer = null;
                        }
                    }

                    function schedulePoll(seconds) {
                        stopPolling();
                        const delaySeconds = Number.isFinite(seconds) && seconds > 0 ? seconds : 5;
                        pollTimer = setTimeout(checkStatus, delaySeconds * 1000);
                    }

                    async function startDeviceFlow() {
                        const response = await fetch('/api/copilot/auth/device/start', {
                            method: 'GET',
                            credentials: 'same-origin',
                            headers: {
                                'Accept': 'application/json'
                            }
                        });

                        const data = await response.json();

                        if (!response.ok || data.status === 'error') {
                            document.getElementById('message').textContent =
                                data.message || 'Errore durante l\\'avvio del Device Flow.';
                            return;
                        }

                        document.getElementById('message').textContent = data.message;
                        document.getElementById('verifyLink').href = data.verificationUri;
                        document.getElementById('verifyLink').textContent = data.verificationUri;
                        document.getElementById('userCode').textContent = data.userCode;
                        document.getElementById('deviceBox').classList.remove('hidden');

                        schedulePoll(data.pollIntervalSeconds);
                    }

                    async function checkStatus() {
                        const response = await fetch('/api/copilot/auth/device/status', {
                            method: 'GET',
                            credentials: 'same-origin',
                            headers: {
                                'Accept': 'application/json'
                            }
                        });

                        const data = await response.json();
                        document.getElementById('message').textContent = data.message || 'Stato non disponibile.';

                        if (data.status === 'pending') {
                            schedulePoll(data.pollAfterSeconds || 5);
                            return;
                        }

                        stopPolling();

                        if (data.status === 'authorized') {
                            document.getElementById('deviceBox').classList.add('hidden');
                            return;
                        }

                        if (data.status === 'expired') {
                            document.getElementById('message').textContent =
                                (data.message || 'Codice scaduto.') + ' Ricarica la pagina per generare un nuovo codice.';
                            return;
                        }

                        if (data.status === 'denied' || data.status === 'error') {
                            return;
                        }
                    }

                    startDeviceFlow().catch(error => {
                        document.getElementById('message').textContent =
                            'Errore durante l\\'avvio del Device Flow: ' + error;
                    });
                </script>
            </body>
            </html>
            """;

    private final CopilotOAuth copilotOAuth;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/login")
    public ResponseEntity<String> login() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.TEXT_HTML)
                .body(LOGIN_HTML);
    }

    @GetMapping("/device/start")
    public Mono<ResponseEntity<Map<String, Object>>> startDeviceAuthorization(WebSession session) {
        return Mono.fromCallable(() -> copilotOAuth.startDeviceAuthorization(session))
                .subscribeOn(Schedulers.boundedElastic())
                .map(deviceStart -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", "code_ready");
                    body.put("message", deviceStart.message());
                    body.put("userCode", deviceStart.userCode());
                    body.put("verificationUri", deviceStart.verificationUri());
                    body.put("expiresInSeconds", deviceStart.expiresInSeconds());
                    body.put("pollIntervalSeconds", deviceStart.pollIntervalSeconds());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                            .header(HttpHeaders.PRAGMA, "no-cache")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body);
                })
                .onErrorResume(ex -> {
                    log.error("Errore durante l'avvio del GitHub Device Flow", ex);

                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", "error");
                    body.put("message", "Errore durante l'avvio del GitHub Device Flow: " + ex.getMessage());

                    return Mono.just(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body));
                });
    }

    @GetMapping("/device/status")
    public Mono<ResponseEntity<Map<String, Object>>> getDeviceAuthorizationStatus(WebSession session) {
        return Mono.fromCallable(() -> copilotOAuth.pollDeviceAuthorization(session))
                .subscribeOn(Schedulers.boundedElastic())
                .map(pollResult -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", pollResult.status());
                    body.put("message", pollResult.message());
                    body.put("authenticated", pollResult.authenticated());
                    body.put("pollAfterSeconds", pollResult.pollAfterSeconds());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                            .header(HttpHeaders.PRAGMA, "no-cache")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body);
                })
                .onErrorResume(ex -> {
                    log.error("Errore durante il polling del GitHub Device Flow", ex);

                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", "error");
                    body.put("message", "Errore durante il polling del GitHub Device Flow: " + ex.getMessage());
                    body.put("authenticated", false);
                    body.put("pollAfterSeconds", null);

                    return Mono.just(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body));
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(WebSession session) {
        String accessToken = gitHubTokenSessionService.getOptionalAccessToken(session);

        return Mono.fromCallable(() -> {
                    if (accessToken != null && !accessToken.isBlank()) {
                        copilotOAuth.revokeApplicationGrant(accessToken);
                        return "Logout completato e autorizzazione GitHub revocata";
                    }

                    return "Logout completato, nessun token GitHub presente in sessione";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.error("Errore durante la revoca del grant GitHub", ex);
                    return Mono.just("Logout locale completato, ma la revoca GitHub non è stata confermata");
                })
                .flatMap(message -> {
                    gitHubTokenSessionService.clearAll(session);

                    return session.invalidate()
                            .thenReturn(ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Map.of("message", message)));
                });
    }

}
