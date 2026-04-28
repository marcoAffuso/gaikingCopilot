package it.nttdata.gaikingCopilot.model;



public record ConfluenceRequestParameters(
    String model,
    String reasoningEffort,
    String confluenceUrl,
    String spaceKey,
    String pageTitle,
    String outputDir
) {

}
