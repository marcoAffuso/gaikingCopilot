package it.nttdata.gaikingCopilot.utility;

import java.text.Normalizer;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LlmSanitizer {

    // Remove any XML/HTML-like tag: <...>
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    // Remove zero-width and BOM characters that can confuse tokenization
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\uFEFF\\u2060]");

    // Remove control chars except common whitespace (\n \r \t)
    private static final Pattern CTRL_EXCEPT_WS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    // Collapse horizontal whitespace (spaces, tabs, etc.)
    private static final Pattern HSPACES = Pattern.compile("[\\h\\x0B\\f]+");

    // Collapse 3+ line breaks into 2 (Sonar-safe; avoids nested quantifiers/backtracking)
    private static final Pattern MANY_BLANK_LINES = Pattern.compile("(?:\\R){3,}");

    /**
     * Cleans Confluence storage format / HTML-like content for LLM ingestion.
     */
    public String sanitizeConfluenceForLlm(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String s = raw;

        // 1) Decode HTML entities first (turn &agrave; into à, &nbsp; into NBSP, etc.)
        s = StringEscapeUtils.unescapeHtml4(s);

        // 2) Strip all markup tags (Confluence storage format is tag-heavy)
        s = TAGS.matcher(s).replaceAll(" ");

        // 3) Unicode normalize (merges compatibility chars, reduces weird variants)
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);

        // 4) Remove zero-width characters / BOM
        s = ZERO_WIDTH.matcher(s).replaceAll("");

        // 5) Replace NBSP with normal space (in case unescape produced NBSP)
        s = s.replace('\u00A0', ' ');

        // 6) Normalize “smart punctuation” to simpler equivalents (optional but helpful)
        s = s.replace('’', '\'')
             .replace('‘', '\'')
             .replace('“', '"')
             .replace('”', '"')
             .replace('–', '-')
             .replace('—', '-');

        // 7) Remove remaining control chars (keep \n \r \t)
        s = CTRL_EXCEPT_WS.matcher(s).replaceAll("");

        // 8) Normalize whitespace:
        //    - collapse horizontal whitespace
        //    - trim around line breaks
        //    - collapse excessive blank lines (3+ -> 2)
        s = HSPACES.matcher(s).replaceAll(" ");
        s = s.replaceAll("[\\h]*\\R[\\h]*", "\n");
        s = MANY_BLANK_LINES.matcher(s).replaceAll("\n\n");

        return s.trim();
    }     

}
