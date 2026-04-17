package it.nttdata.gaikingCopilot.utility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ReadAndWriteJson {

    private final ObjectMapper objectMapper;

    public ReadAndWriteJson() {
        this.objectMapper = new ObjectMapper();
    }


    /**
     * @param json json file to string
     * @return Json node
     * This method read Json node from json file
     */


    public JsonNode readJsonNode(String json){

        try {
            return this.objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
        
    }


    public Map<String,Object> fromJsonNodeToMap(JsonNode json){
        return this.objectMapper.convertValue(json, new TypeReference<>() {});
    }

    /**
     * @param pathAndNameFile path with name json file
     * @return Json file to string
     * This method parse Json file to string
     */

    public String fromJsonFileToStringJson(String pathAndNameFile){

        String receiptTemplate = null;
        try {
            receiptTemplate = FileUtils.readFileToString(new File(pathAndNameFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return receiptTemplate;

    }

    /**
     * @param pathAndNameFile path with name json file
     * @param  json json String
     * This method parse Json file to string
     */
    public void writeFileJson(String pathAndNameFile, String json){
        try{
            FileUtils.writeStringToFile(new File(pathAndNameFile),json,StandardCharsets.UTF_8,false);
        }catch (IOException e){
            log.error(e.getMessage());
        }
    }

    /**
     * @param object object to transform in json string
     * @return Json string
     * This method transform an object in json string
     */
    public String fromObjectToJsonString(Object object){
        String json = null;
        try {
            json= this.objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        return json;
    }

        public String cleanJson(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // rimuove blocchi markdown iniziali/finali ```json / ```
        if (s.startsWith("```json")) s = s.substring(7).trim();
        if (s.startsWith("```")) s = s.substring(3).trim();
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();

        // a volte i modelli includono "```" più volte: pulizia semplice
        return s;
    }

    public boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JSON detected: {}", e.getMessage());
            return false;
        }
    }

    public String getJsonValidationError(String json) {
        try {
            objectMapper.readTree(json);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String normalizeJsonEscapes(String json) {

        try {
            ObjectMapper tolerant = JsonMapper.builder()
                    .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .build();

            JsonNode node = tolerant.readTree(json);

            // Compatto su una riga (se vuoi pretty, usa writerWithDefaultPrettyPrinter())
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile normalizzare il JSON: " + e.getMessage(), e);
        }

    }

}
