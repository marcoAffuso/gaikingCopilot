package it.nttdata.gaikingCopilot.utility;

import org.springframework.stereotype.Component;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Component
public class JavaFormatter {

    public String toOneLine(String javaCode) {
        try {
            Formatter formatter = new Formatter();
            return formatter.formatSource(javaCode).replaceAll("\\s*\\n\\s*", " ");
        } catch (FormatterException e) {
            throw new RuntimeException("Errore durante la formattazione del codice Java", e);
        }
    }

    public String prettyFormat(String javaCode) {
        try {
            Formatter formatter = new Formatter();
            return formatter.formatSource(javaCode);
        } catch (FormatterException e) {
            throw new RuntimeException("Errore durante la formattazione del codice Java", e);
        }
    }


}
