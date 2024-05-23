package FowlFlightForensics.util.incident.rules;

public class NotEmptyValidationRule implements ValidationRule<String> {
    public NotEmptyValidationRule() { }

    @Override
    public boolean isValid(String value) {
        return !value.isEmpty();
    }
}
