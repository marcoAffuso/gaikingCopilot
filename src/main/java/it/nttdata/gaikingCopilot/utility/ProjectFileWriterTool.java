package it.nttdata.gaikingCopilot.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import it.nttdata.gaikingCopilot.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Component
public class ProjectFileWriterTool {

    private final ReadAndWriteJson readAndWriteJson;
    private final XmlMinifyService xmlMinifyService;

    /**
     * Scrive i file del progetto nella cartella base indicata.
     * @param baseDir nome cartella base (es. "automation-generated")
     * @param jsonString JSON con struttura { "files": [ { "path": "...", "content": "..." }, ... ] }
     */
    public void writeProjectFiles(String baseDir, String jsonString) {
        try {

            log.info("JSON ricevuto:\n" + jsonString);

            JsonNode root = readAndWriteJson.readJsonNode(jsonString);
            JsonNode files = root.get("files");

            if (files == null || !files.isArray()) {
                throw new IllegalArgumentException("JSON non contiene un array 'files'");
            }

            for (JsonNode fileNode : files) {
                String filePath = fileNode.get("path").asText();
                String content = fileNode.get("content").asText();

                Path fullPath = Path.of(baseDir, filePath);
                File file = fullPath.toFile();

                // Crea cartelle se non esistono
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        throw new IOException("Impossibile creare cartella: " + parent);
                    }
                }

                if(filePath.endsWith(".xml")) {
                    log.info("Rilevato file XML, procedo a minificarlo.");
                    content = xmlMinifyService.prettyPrintXml(content, 2);
                }

                // Sostituisci \\n con newline reali
                String contentWithNewlines = content.replace("\\n", System.lineSeparator());
                Files.writeString(fullPath, contentWithNewlines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                log.info("Creato file: " + fullPath);
            }
        } catch (IOException e) {
            log.error("Errore durante la scrittura dei file del progetto", e);
            throw new CustomException("Errore durante la scrittura dei file del progetto", 422, e);
        }
    }

}
