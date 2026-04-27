package it.nttdata.gaikingCopilot.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.service.copilot.CopilotOAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@RestController
@RequestMapping("/api/copilot/auth")
@RequiredArgsConstructor
public class CopilotAuthController {

    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";

    private final CopilotOAuth copilotOAuth;


    @GetMapping("/device/start")
    public Mono<ResponseEntity<Map<String, Object>>> startDeviceAuthorization(WebSession session) {
        return Mono.fromCallable(() -> copilotOAuth.startDeviceAuthorization(session))
                .subscribeOn(Schedulers.boundedElastic())
                .map(deviceStart -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put(STATUS_KEY, "code_ready");
                    body.put(MESSAGE_KEY, deviceStart.message());
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
                    body.put(STATUS_KEY, "error");
                    body.put(MESSAGE_KEY, "Errore durante l'avvio del GitHub Device Flow: " + ex.getMessage());

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
                    body.put(STATUS_KEY, pollResult.status());
                    body.put(MESSAGE_KEY, pollResult.message());
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
                    body.put(STATUS_KEY, "error");
                    body.put(MESSAGE_KEY, "Errore durante il polling del GitHub Device Flow: " + ex.getMessage());
                    body.put("authenticated", false);
                    body.put("pollAfterSeconds", null);

                    return Mono.just(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body));
                });
    }

}
