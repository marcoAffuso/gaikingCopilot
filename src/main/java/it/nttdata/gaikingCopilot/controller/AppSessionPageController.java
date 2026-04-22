package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AppSessionPageController {

    @GetMapping("/logout")
    public String logoutPage() {
        return "logout";
    }

    @GetMapping("/logged-out")
    public String loggedOutPage(
            @RequestParam(required = false) String message,
            Model model
    ) {
        String pageMessage = (message != null && !message.isBlank())
                ? message
                : "Hai effettuato il logout correttamente.";

        model.addAttribute("message", pageMessage);
        return "logged-out";
    }
}