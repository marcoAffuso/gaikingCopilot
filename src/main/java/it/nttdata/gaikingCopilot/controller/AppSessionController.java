package it.nttdata.gaikingCopilot.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.service.copilot.CopilotOAuth;
import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@RestController
@RequiredArgsConstructor
public class AppSessionController {

    private final CopilotOAuth copilotOAuth;
    private final GitHubTokenSessionService gitHubTokenSessionService;

    @PostMapping({"/api/session/logout", "/api/copilot/auth/logout"})
    public Mono<ResponseEntity<Map<String, String>>> logout(WebSession session) {
        log.info("Handling logout request. sessionId={}", sessionId(session));

        String accessToken = gitHubTokenSessionService.getOptionalAccessToken(session);

        return Mono.fromCallable(() -> {
                    if (accessToken != null && !accessToken.isBlank()) {
                        log.info("Revoking GitHub authorization during logout. sessionId={}", sessionId(session));
                        copilotOAuth.revokeApplicationGrant(accessToken);
                        return "Logout completato e autorizzazione GitHub revocata";
                    }

                    log.info("Completing logout without GitHub access token in session. sessionId={}", sessionId(session));
                    return "Logout completato, nessun token GitHub presente in sessione";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.error("Logout request failed during GitHub authorization revocation. sessionId={}", sessionId(session), ex);
                    return Mono.just("Logout locale completato, ma la revoca GitHub non è stata confermata");
                })
                .flatMap(message -> {
                    log.info("Completing logout request and invalidating session. sessionId={}", sessionId(session));
                    gitHubTokenSessionService.clearAll(session);

                    return session.invalidate()
                            .thenReturn(ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Map.of("message", message)));
                });
    }

    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }
}