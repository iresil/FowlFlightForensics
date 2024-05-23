package FowlFlightForensics.util.incident.rules;

public interface ValidationRule<T> {
    boolean isValid(T value);
}
