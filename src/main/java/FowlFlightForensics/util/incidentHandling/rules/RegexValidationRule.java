package FowlFlightForensics.util.incidentHandling.rules;

public class RegexValidationRule implements ValidationRule<String> {
    private final String regex;

    public RegexValidationRule(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean isValid(String value) {
        return !value.matches(regex);
    }
}
