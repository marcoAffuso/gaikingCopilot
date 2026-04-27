package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;


@Log4j2
@Controller
@RequiredArgsConstructor
public class HomePageController {

    private static final String LOGIN_REDIRECT = "redirect:/api/copilot/auth/login";

    public final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/homePage")
    public String homePage(WebSession session) {
        return resolveProtectedView(session, "homePage", "/homePage");
    }

    @GetMapping("/homePage/createNewProjectTa")
    public String createNewProjectTa(WebSession session) {
        return resolveProtectedView(session, "createNewProjectTa", "/homePage/createNewProjectTa");
    }

    @GetMapping("/homePage/generateTestCase")
    public String generateTestCase(WebSession session) {
        return resolveProtectedView(session, "generateTestCase", "/homePage/generateTestCase");
    }

    @GetMapping("/homePage/toolManagement")
    public String toolManagement(WebSession session) {
        return resolveProtectedView(session, "toolManagement", "/homePage/toolManagement");
    }

    private String resolveProtectedView(WebSession session, String viewName, String path) {
        log.info("Handling page request. path={}, sessionId={}", path, sessionId(session));

        if (session == null || session.isExpired()) {
            log.info("Redirecting to login because session is missing or expired. path={}, sessionId={}", path, sessionId(session));
            return LOGIN_REDIRECT;
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            log.info("Redirecting to login because GitHub token is missing in session. path={}, sessionId={}", path, session.getId());
            return LOGIN_REDIRECT;
        }

        log.info("Rendering protected page. path={}, viewName={}, sessionId={}", path, viewName, session.getId());
        return viewName;
    }

    private String sessionId(WebSession session) {
        return session != null ? session.getId() : "unknown";
    }

}
