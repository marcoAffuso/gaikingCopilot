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