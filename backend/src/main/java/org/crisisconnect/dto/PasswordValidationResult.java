package org.crisisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of password validation containing errors and strength score.
 * Used by PasswordValidationService to return validation results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordValidationResult {

    private boolean valid;
    private int strengthScore; // 0-100
    private List<String> errors = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public PasswordValidationResult(boolean valid) {
        this.valid = valid;
        this.strengthScore = 0;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }

    public static PasswordValidationResult success(int strengthScore) {
        PasswordValidationResult result = new PasswordValidationResult();
        result.setValid(true);
        result.setStrengthScore(strengthScore);
        return result;
    }

    public static PasswordValidationResult failure(String error) {
        PasswordValidationResult result = new PasswordValidationResult();
        result.setValid(false);
        result.addError(error);
        return result;
    }
}
