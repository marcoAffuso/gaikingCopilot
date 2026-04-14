package it.nttdata.gaikingCopilot.utility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

}
