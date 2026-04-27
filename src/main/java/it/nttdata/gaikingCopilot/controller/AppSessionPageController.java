package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
public class AppSessionPageController {



    @GetMapping("/logout")
    public String logoutPage() {
        log.info("Rendering logout page.");
        return "logout";
    }

    @GetMapping("/logged-out")
    public String loggedOutPage(
            @RequestParam(required = false) String message,
            Model model
    ) {
            log.info("Rendering logged-out page. customMessagePresent={}", message != null && !message.isBlank());

        String pageMessage = (message != null && !message.isBlank())
                ? message
                : "Hai effettuato il logout correttamente.";

        model.addAttribute("message", pageMessage);
        return "logged-out";
    }
}