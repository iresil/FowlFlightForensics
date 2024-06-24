package FowlFlightForensics.domain.dto;

import FowlFlightForensics.util.BaseComponent;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * An {@code IncidentSummary} object describes a shortened version of the {@code IncidentDetails} object, from which
 * various fields have been removed, e.g.:
 * <ul>
 *   <li>fields whose meaning wasn't immediately apparent
 *   <li>fields with a lot of invalid data
 *   <li>fields that were definitely not going to be used for the final calculation
 * </ul>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class IncidentSummary extends BaseComponent {
    public Integer recordId;
    public Integer year;
    public Integer month;
    public Integer day;
    public String aircraft;
    public Float aircraftMass;
    public Integer engines;
    public String airportId;
    public String airportName;
    public String state;
    public String faaRegion;
    public Boolean warningIssued;
    public String flightPhase;
    public String speciesId;
    public String speciesName;
    public Integer speciesQuantityMin;
    public Integer speciesQuantityMax;
    public Integer fatalities;
    public Integer injuries;
    public Boolean aircraftDamage;

    /**
     * This method is just a convenient way of generating the {@code IncidentKey} object that corresponds to this specific
     * {@code IncidentSummary}, which will be used as a Key for sending {@code Kafka} messages.
     * @return An {@code IncidentKey} object, using fields from the current {@code IncidentSummary}.
     */
    public IncidentKey getKey() {
        return new IncidentKey(year, month, speciesId, speciesName, aircraftDamage);
    }

    /**
     * Utilizes reflection to retrieve a field's value, provided that the field's name matches the {@code fieldName}
     * parameter.
     * @param fieldName The name of the field whose value we need to retrieve.
     * @return A generic {@code Object} containing the field's value, regardless of its original type.
     */
    public Object getFieldValueByName(String fieldName) {
        Object result = null;
        try {
            Field f = getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            result = f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Error attempting to retrieve field value by name: {}", fieldName, e);
        }
        return result;
    }

    /**
     * Method used for logging purposes, in places where we need to know the internal contents of the {@code IncidentSummary}
     * object.
     * @return A {@code String} representation of the original {@code IncidentSummary} object.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("IncidentSummary { ");
        try {
            Field[] fields = getClass().getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                result.append(f.getName()).append("=").append(f.get(this)).append(", ");
            }
        } catch (IllegalAccessException e) {
            logger.error("Error attempting to retrieve declared fields for printing", e);
        }
        result.delete(result.length() - 2, result.length() - 1);
        result.append(" }");
        return result.toString();
    }

    /**
     * Overrides the default {@code equals}, to be used for testing.
     * @param o The object to compare to.
     * @return {@code true} if objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof IncidentSummary)) {
            return false;
        }

        IncidentSummary c = (IncidentSummary) o;

        return Objects.equals(recordId, c.recordId)
                && Objects.equals(year, c.year)
                && Objects.equals(month, c.month)
                && Objects.equals(day, c.day)
                && Objects.equals(aircraft, c.aircraft)
                && Objects.equals(aircraftMass, c.aircraftMass)
                && Objects.equals(engines, c.engines)
                && Objects.equals(airportId, c.airportId)
                && Objects.equals(airportName, c.airportName)
                && Objects.equals(state, c.state)
                && Objects.equals(faaRegion, c.faaRegion)
                && Objects.equals(warningIssued, c.warningIssued)
                && Objects.equals(flightPhase, c.flightPhase)
                && Objects.equals(speciesId, c.speciesId)
                && Objects.equals(speciesName, c.speciesName)
                && Objects.equals(speciesQuantityMin, c.speciesQuantityMin)
                && Objects.equals(speciesQuantityMax, c.speciesQuantityMax)
                && Objects.equals(fatalities, c.fatalities)
                && Objects.equals(injuries, c.injuries)
                && Objects.equals(aircraftDamage, c.aircraftDamage);
    }

    /**
     * Overrides the default {@code hashCode} method, since {@code equals} has also been overridden, to ensure the same
     * objects don't get different hash values.
     * @return The {@code recordId} as a unique hash code.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(recordId, year, month, day, aircraft, airportId, airportName, state, flightPhase, speciesId,
                speciesName, aircraftDamage);
    }
}
