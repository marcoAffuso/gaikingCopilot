package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.WebSession;

import it.nttdata.gaikingCopilot.service.copilot.GitHubTokenSessionService;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class HomePageController {

    private static final String LOGIN_REDIRECT = "redirect:/api/copilot/auth/login";

    public final GitHubTokenSessionService gitHubTokenSessionService;

    @GetMapping("/homePage")
    public String homePage(WebSession session) {
        if (session == null || session.isExpired()) {
            return LOGIN_REDIRECT;
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return LOGIN_REDIRECT;
        }

        return "homePage";
    }

    @GetMapping("/homePage/createNewProjectTa")
    public String createNewProjectTa(WebSession session) {
        if (session == null || session.isExpired()) {
            return LOGIN_REDIRECT;
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return LOGIN_REDIRECT;
        }
        return "createNewProjectTa";
    }

    @GetMapping("/homePage/generateTestCase")
    public String generateTestCase(WebSession session) {
        if (session == null || session.isExpired()) {
            return LOGIN_REDIRECT;
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return LOGIN_REDIRECT;
        }
        return "generateTestCase";
    }

    @GetMapping("/homePage/toolManagement")
    public String toolManagement(WebSession session) {
        if (session == null || session.isExpired()) {
            return LOGIN_REDIRECT;
        }

        String token = gitHubTokenSessionService.getOptionalAccessToken(session);
        if (token == null || token.isBlank()) {
            return LOGIN_REDIRECT;
        }
        return "toolManagement";
    }
    

}
