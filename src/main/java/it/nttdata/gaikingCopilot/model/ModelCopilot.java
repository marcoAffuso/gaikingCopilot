package it.nttdata.gaikingCopilot.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelCopilot {
    private String id;
    private String name;
    private List<String> supportedReasoningEfforts;
    private boolean isReasoningEffort;

}
