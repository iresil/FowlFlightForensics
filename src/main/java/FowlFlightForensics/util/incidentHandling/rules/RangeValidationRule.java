package FowlFlightForensics.util.incidentHandling.rules;

public class RangeValidationRule implements ValidationRule<Integer> {
    private final int min;
    private final int max;

    public RangeValidationRule(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean isValid(Integer value) {
        return value >= min && value <= max;
    }
}
