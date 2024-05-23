package FowlFlightForensics.util.incident.rules;

public class ValueValidationRule implements ValidationRule<String> {
    private final String val;

    public ValueValidationRule(String val) {
        this.val = val;
    }

    @Override
    public boolean isValid(String value) {
        return !value.contains(val);
    }
}
