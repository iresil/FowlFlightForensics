package FowlFlightForensics.domain.dto;

import FowlFlightForensics.util.BaseComponent;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;

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

    public IncidentKey getKey() {
        return new IncidentKey(year, month, speciesId, speciesName, aircraftDamage);
    }

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
}