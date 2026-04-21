package it.nttdata.gaikingCopilot.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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


    private static final String SUCCESS_HTML = """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Autenticazione completata</title>
            </head>
            <body>
                <p>Autenticazione GitHub completata correttamente.</p>
                <script>
                    window.history.replaceState({}, document.title, '/api/copilot/auth/success');
                </script>
            </body>
            </html>
            """;

    private final CopilotOAuth copilotOAuth;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/login")
    public ResponseEntity<Void> login(WebSession session) {
        String authorizationUrl = copilotOAuth.buildAuthorizationUrl(session);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizationUrl))
                .build();
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            WebSession session) {

        if (error != null && !error.isBlank()) {
            String message = "GitHub ha restituito un errore: " + error
                    + (errorDescription != null && !errorDescription.isBlank() ? " - " + errorDescription : "");
            log.error(message);

            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message));
        }

        if (code == null || code.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Authorization code mancante"));
        }

        if (state == null || state.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("State mancante"));
        }

        return Mono.fromCallable(() -> copilotOAuth.exchangeAuthorizationCodeForAccessToken(code, state, session))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ignored -> ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .contentType(MediaType.TEXT_HTML)
                        .body(SUCCESS_HTML));
    }

    @GetMapping("/success")
    public ResponseEntity<String> successPage() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Autenticazione GitHub completata correttamente.");
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
