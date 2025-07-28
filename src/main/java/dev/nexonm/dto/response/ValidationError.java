package dev.nexonm.dto.response;

/**
 * Validation error information
 */
public record ValidationError(Long rowNumber, String message) {
    @Override
    public String toString() {
        return String.format("Row %d: %s", rowNumber, message);
    }
}
