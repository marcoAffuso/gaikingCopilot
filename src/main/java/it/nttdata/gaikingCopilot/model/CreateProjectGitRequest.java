package it.nttdata.gaikingCopilot.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Valid
@Getter
@Setter
public class CreateProjectGitRequest {
    @NotBlank
    private String projectName;

    @NotBlank
    @Pattern(regexp = "^https://.+\\.git$", message = "Repository Name must start with https:// and end with .git.")
    private String repositoryName;

    @NotBlank
    private String userGit;

    @NotBlank
    private String tokenGit;

}
