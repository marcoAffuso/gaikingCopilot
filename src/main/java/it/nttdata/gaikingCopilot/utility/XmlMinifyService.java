package it.nttdata.gaikingCopilot.utility;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class XmlMinifyService {

    public String toSingleLineXml(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

            Document doc = db.parse(new InputSource(new StringReader(xml)));

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));

            String compact = sw.toString();
            compact = compact.replaceAll(">\\s+<", "><").replace("\n", "").replace("\r", "").trim();
            return compact;
        } catch (Exception e) {
            log.error("Errore durante la minificazione dell'XML: " + e.getMessage());
            throw new RuntimeException("Errore durante la scrittura del pom nel db Cassandra", e);
            
            
        }
    }    

    public String prettyPrintXml(String xmlString, int indentAmount){
            TransformerFactory tf = TransformerFactory.newInstance();
            // Se vuoi assicurarti di usare Xalan, potresti dover specificare la factory.
            // tf.setAttribute("http://xml.apache.org/xalan/properties/indent-amount", indentAmount);
            try {
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));

                StreamSource source = new StreamSource(new StringReader(xmlString));
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                transformer.transform(source, result);
                return writer.toString();
            } catch (TransformerException e) {
                log.error("Errore durante la ri-formattazione del pom : {}" , e.getMessage());
                throw new RuntimeException("Errore durante la ri-formattazione del pom", e);
            }
            
    }

}
