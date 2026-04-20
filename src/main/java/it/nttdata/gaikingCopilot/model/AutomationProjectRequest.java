package it.nttdata.gaikingCopilot.model;

public record AutomationProjectRequest(

        String modelName,
        String projectName,
        String groupId,
        String javaVersion,
        String seleniumVersion,
        String junitVersion,
        String junitPlatformVersion,
        String cucumberVersion,
        String webdrivermanagerVersion,
        String surefireVersion,
        String compilerPluginVersion
) {

}
