package it.nttdata.gaikingCopilot.utility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ValidateJunitFormTest {

    private final ValidateJunitForm validateJunitForm = new ValidateJunitForm();

    private void validateFullConfiguration(String projectName, String groupId, String junitVersion, String... otherFields) {
        String[] allFields = Stream.concat(
            Stream.of(projectName, groupId, junitVersion),
            Arrays.stream(otherFields)
        ).toArray(String[]::new);

        validateJunitForm.validateRequiredFields(allFields);
        validateJunitForm.validateAllowedTextFields(allFields);
        validateJunitForm.validateProjectName(projectName);
        validateJunitForm.validateGroupId(groupId);
        validateJunitForm.validateJunitVersion(junitVersion);
    }

    @Test
    void shouldAcceptValidConfiguration() {
        assertDoesNotThrow(() -> validateFullConfiguration(
            "demo-project",
            "it.nttdata",
            "5.13.4",
            "17",
            "4.32.0",
            "1.12.2",
            "7.18.1",
            "6.3.0",
            "3.5.3",
            "3.13.0"
        ));
    }

    @Test
    void shouldRejectBlankFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validateJunitForm.validateRequiredFields(
            "demo-project",
            "it.nttdata",
            "5.13.4",
            "17",
            " ",
            "1.12.2",
            "7.18.1",
            "6.3.0",
            "3.5.3",
            "3.13.0"
        ));

        assertEquals("All fields must be filled in.", exception.getMessage());
    }

    @Test
    void shouldRejectSpecialCharactersInTextFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validateJunitForm.validateAllowedTextFields(
            "demo-project",
            "it.nttdata",
            "17!",
            "4.32.0",
            "1.12.2",
            "7.18.1",
            "6.3.0",
            "3.5.3",
            "3.13.0"
        ));

        assertEquals("Special characters are not allowed in text fields.", exception.getMessage());
    }

    @Test
    void shouldRejectProjectNameWithDigits() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validateJunitForm.validateProjectName("demo1-project"));

        assertEquals("Project Name cannot contain numeric characters.", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidGroupId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validateJunitForm.validateGroupId("it1.nttdata"));

        assertEquals("Group Id must use the format stringa1.stringa2 with letters only.", exception.getMessage());
    }

    @Test
    void shouldRejectJunitVersionWithLeadingZeroMajor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validateJunitForm.validateJunitVersion("05.13.4"));

        assertEquals("Junit Version must start with a major version greater than 4.", exception.getMessage());
    }
}