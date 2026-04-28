package it.nttdata.gaikingCopilot.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class JsonToExcelConverter {

     private final ReadAndWriteJson readAndWriteJson;

    /**
     * Converte un JSON string contenente requirements_summary in un file Excel
     * @param jsonString JSON string da convertire
     * @return il path completo del file Excel creato
     * @throws IOException 
     */
    public void convertJsonToExcel(String jsonString, String outputDir) throws IOException {
        log.info("Starting JSON to Excel conversion.");
        // Pulizia del JSON
        String cleanedJson = readAndWriteJson.cleanJson(jsonString);
            
        // Validazione del JSON
        if (!readAndWriteJson.isValidJson(cleanedJson)) {
            throw new IllegalArgumentException("JSON non valido fornito");
        }
            
        // Parsing del JSON
        JsonNode rootNode = readAndWriteJson.readJsonNode(cleanedJson);
            
        // Crea il workbook e il sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestsCases");
            
        // Estrae gli elementi di requirements_summary
        ArrayNode requirementsSummary = (ArrayNode) rootNode.get("requirements_summary");
            
        if (requirementsSummary != null && requirementsSummary.isArray()) {
            // Crea l'header
            createHeader(sheet);
                
            // Popola le righe con i dati
            int rowNum = 1;
            for (JsonNode requirement : requirementsSummary) {
                addRequirementRow(sheet, requirement, rowNum);
                rowNum++;
            }
                
                // Adatta la larghezza delle colonne
            autoResizeColumns(sheet);
        }
            
        // Salva il file
        String filePath = saveExcelFile(workbook , outputDir);
        log.info("Excel file creato con successo: {}", filePath);

    }
    
    /**
     * Crea l'header del foglio Excel
     */
    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        
        String[] headers = {
            "ID", 
            "Title", 
            "Text", 
            "Actors", 
            "Business Rules", 
            "Validations", 
            "Data Constraints", 
            "Flows"
        };
        
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    /**
     * Aggiunge una riga al foglio con i dati di un requirement
     */
    private void addRequirementRow(Sheet sheet, JsonNode requirement, int rowNum) {
        Row row = sheet.createRow(rowNum);
        
        // ID
        Cell idCell = row.createCell(0);
        idCell.setCellValue(requirement.get("id") != null ? requirement.get("id").asText() : "");
        
        // Title
        Cell titleCell = row.createCell(1);
        titleCell.setCellValue(requirement.get("title") != null ? requirement.get("title").asText() : "");
        
        // Text
        Cell textCell = row.createCell(2);
        textCell.setCellValue(requirement.get("text") != null ? requirement.get("text").asText() : "");
        
        // Actors
        Cell actorsCell = row.createCell(3);
        actorsCell.setCellValue(jsonArrayToString(requirement.get("actors")));
        
        // Business Rules
        Cell businessRulesCell = row.createCell(4);
        businessRulesCell.setCellValue(jsonArrayToString(requirement.get("business_rules")));
        
        // Validations
        Cell validationsCell = row.createCell(5);
        validationsCell.setCellValue(jsonArrayToString(requirement.get("validations")));
        
        // Data Constraints
        Cell dataConstraintsCell = row.createCell(6);
        dataConstraintsCell.setCellValue(jsonArrayToString(requirement.get("data_constraints")));
        
        // Flows
        Cell flowsCell = row.createCell(7);
        flowsCell.setCellValue(jsonArrayToString(requirement.get("flows")));
    }
    
    /**
     * Converte un array JSON in una stringa separata da virgole
     */
    private String jsonArrayToString(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        ArrayNode array = (ArrayNode) arrayNode;
        
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(array.get(i).asText());
        }
        
        return sb.toString();
    }
    
    /**
     * Ridimensiona automaticamente la larghezza delle colonne
     */
    private void autoResizeColumns(Sheet sheet) {
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
            // Aggiunge un piccolo margine
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 256);
        }
    }
    
    /**
     * Salva il workbook in un file Excel nella cartella testsCases
     */
    private String saveExcelFile(Workbook workbook,String outputDir) throws IOException {
        Path dirPath = Paths.get(outputDir);
        
        // Crea la cartella se non esiste
        Files.createDirectories(dirPath);
        
        // Genera il nome del file con timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "testsCases_" + timestamp + ".xlsx";
        
        Path filePath = dirPath.resolve(fileName);
        
        // Scrive il file
        try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            workbook.write(fileOut);
            workbook.close();
        }
        
        return filePath.toString();
    }

}
