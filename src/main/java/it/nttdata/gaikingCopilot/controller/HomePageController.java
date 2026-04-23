package it.nttdata.gaikingCopilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomePageController {

    @GetMapping("/homePage")
    public String homePage() {
        return "homePage";
    }

    @GetMapping("/homePage/createNewProjectTa")
    public String createNewProjectTa() {
        return "createNewProjectTa";
    }

    @GetMapping("/homePage/generateTestCase")
    public String generateTestCase() {
        return "generateTestCase";
    }

    @GetMapping("/homePage/toolManagement")
    public String toolManagement() {
        return "toolManagement";
    }
    

}
