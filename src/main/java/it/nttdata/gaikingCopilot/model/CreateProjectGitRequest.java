package it.nttdata.gaikingCopilot.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Valid
@Getter
@Setter
public class CreateProjectGitRequest {
    @NotBlank
    private String projectName;
    @NotBlank
    private String repositoryName;
    @NotBlank
    private String userGit;
    @NotBlank
    private String tokenGit;

}
