package it.nttdata.gaikingCopilot.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.copilot.sdk.json.ModelInfo;
import it.nttdata.gaikingCopilot.copilot.CopilotService;
import it.nttdata.gaikingCopilot.model.ModelCopilot;
import org.springframework.web.bind.annotation.RequestParam;



@Log4j2
@RestController
@RequestMapping("/")
public class CopilotController {

    private final CopilotService copilotService;


    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @GetMapping("/getCopilotModels")
    public List<ModelCopilot> getCopilotModels() throws InterruptedException, ExecutionException {

        List<ModelInfo> models = copilotService.getCopilotModel();
        return models.stream()
                .map(model -> new ModelCopilot(model.getId(), model.getName()))
                .toList();
    }

    @GetMapping("/getTestCopilot")
    public String getTestCopilot(@RequestParam String modelName, @RequestParam String prompt, @RequestParam String streaming) throws InterruptedException, ExecutionException{
        log.info("Received request with param: {}", modelName);

        return switch (streaming.toLowerCase()) {
            case "true" -> copilotService.getResponseCopilotWithStreaming(modelName, prompt);
            case "false" -> copilotService.getResponseCopilotWhitOutStreaming(modelName, prompt);
            default -> throw new IllegalArgumentException("Invalid value for 'streaming' parameter. Expected 'true' or 'false'.");
        };
    }


    
    
    

}
