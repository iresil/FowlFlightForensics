package FowlFlightForensics.util.incident.rules;

public class NotNullValidationRule implements ValidationRule<Object> {
    public NotNullValidationRule() { }

    @Override
    public boolean isValid(Object value) {
        return value != null;
    }
}
