package FowlFlightForensics.util.incidentHandling.rules;

public interface ValidationRule<T> {
    boolean isValid(T value);
}
