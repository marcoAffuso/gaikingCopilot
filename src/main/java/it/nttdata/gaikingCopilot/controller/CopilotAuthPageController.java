package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/copilot/auth")
public class CopilotAuthPageController {
    @GetMapping("/login")
    public String login() {
        return "authGitHub";
    }

}
