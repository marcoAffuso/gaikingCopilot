package it.nttdata.gaikingCopilot.utility;

import java.util.regex.Pattern;

public class ValidateJunitForm {

    private static final String EMPTY_FIELDS_MESSAGE = "All fields must be filled in.";
    private static final String INVALID_TEXT_FIELDS_MESSAGE = "Special characters are not allowed in text fields.";
    private static final String INVALID_PROJECT_NAME_MESSAGE = "Project Name cannot contain numeric characters.";
    private static final String INVALID_GROUP_ID_MESSAGE = "Group Id must use the format stringa1.stringa2 with letters only.";
    private static final String INVALID_JUNIT_VERSION_MESSAGE = "Junit Version must start with a major version greater than 4.";
    private static final Pattern ALLOWED_TEXT_FIELD_PATTERN = Pattern.compile("^[A-Za-z0-9._ -]+$");
    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("^[A-Za-z]+\\.[A-Za-z]+$");

    public void validateRequiredFields(String... fields) {
        for (String field : fields) {
            validateFieldIsNotBlank(field);
        }
    }

    public void validateAllowedTextFields(String... fields) {
        for (String field : fields) {
            validateFieldContainsAllowedCharacters(field);
        }
    }

    public void validateJunitVersion(String junitVersion) {
        if (junitVersion == null || junitVersion.isBlank()) {
            throw new IllegalArgumentException(INVALID_JUNIT_VERSION_MESSAGE);
        }

        String trimmedJunitVersion = junitVersion.trim();
        String[] versionParts = trimmedJunitVersion.split("\\.");
        if (versionParts.length == 0 || versionParts[0].isBlank()) {
            throw new IllegalArgumentException(INVALID_JUNIT_VERSION_MESSAGE);
        }

        if (!startsWithValidMajorVersion(versionParts[0])) {
            throw new IllegalArgumentException(INVALID_JUNIT_VERSION_MESSAGE);
        }

        validateNumericVersionParts(versionParts);
    }

    private void validateFieldIsNotBlank(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_FIELDS_MESSAGE);
        }
    }

    private void validateFieldContainsAllowedCharacters(String field) {
        if (!ALLOWED_TEXT_FIELD_PATTERN.matcher(field.trim()).matches()) {
            throw new IllegalArgumentException(INVALID_TEXT_FIELDS_MESSAGE);
        }
    }

    public void validateProjectName(String projectName) {
        if (projectName.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException(INVALID_PROJECT_NAME_MESSAGE);
        }
    }

    public void validateGroupId(String groupId) {
        if (!GROUP_ID_PATTERN.matcher(groupId.trim()).matches()) {
            throw new IllegalArgumentException(INVALID_GROUP_ID_MESSAGE);
        }
    }

    private boolean startsWithValidMajorVersion(String majorVersionPart) {
        if (!majorVersionPart.chars().allMatch(Character::isDigit)) {
            return false;
        }

        return majorVersionPart.charAt(0) >= '5' && majorVersionPart.charAt(0) <= '9';
    }

    private void validateNumericVersionParts(String[] versionParts) {
        try {
            for (String versionPart : versionParts) {
                if (versionPart.isBlank()) {
                    throw new IllegalArgumentException(INVALID_JUNIT_VERSION_MESSAGE);
                }

                Integer.parseInt(versionPart);
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(INVALID_JUNIT_VERSION_MESSAGE, ex);
        }
    }

}
