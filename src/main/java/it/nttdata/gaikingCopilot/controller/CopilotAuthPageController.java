package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
@RequestMapping("/api/copilot/auth")
public class CopilotAuthPageController {
    @GetMapping("/login")
    public String login() {
        log.info("Rendering GitHub authentication page.");
        return "authGitHub";
    }

}
