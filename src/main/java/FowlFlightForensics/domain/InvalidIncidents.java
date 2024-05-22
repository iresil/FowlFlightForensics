package FowlFlightForensics.domain;

import FowlFlightForensics.util.BaseComponent;

import java.lang.reflect.Field;
import java.util.*;

public class InvalidIncidents extends BaseComponent {
    private final float MISSING_THRESHOLD = 0.03f;

    public List<IncidentSummary> invalidYear = new ArrayList<>();
    public List<IncidentSummary> invalidMonth = new ArrayList<>();
    public List<IncidentSummary> invalidDay = new ArrayList<>();
    public List<IncidentSummary> invalidAircraft = new ArrayList<>();
    public List<IncidentSummary> invalidAircraftMass = new ArrayList<>();
    public List<IncidentSummary> invalidEngines = new ArrayList<>();
    public List<IncidentSummary> invalidAirportId = new ArrayList<>();
    public List<IncidentSummary> invalidAirportName = new ArrayList<>();
    public List<IncidentSummary> invalidState = new ArrayList<>();
    public List<IncidentSummary> invalidFaaRegion = new ArrayList<>();
    public List<IncidentSummary> invalidWarningIssued = new ArrayList<>();
    public List<IncidentSummary> invalidFlightPhase = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesId = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesName = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesQuantity = new ArrayList<>();
    public List<IncidentSummary> invalidFatalities = new ArrayList<>();
    public List<IncidentSummary> invalidInjuries = new ArrayList<>();

    public List<IncidentSummary> getFieldValueByName(String fieldName) {
        List<IncidentSummary> result = new ArrayList<>();
        try {
            Field f = getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.getType() == List.class) {
                @SuppressWarnings("unchecked") List<IncidentSummary> temp = (List<IncidentSummary>) f.get(this);
                result = temp;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Error attempting to retrieve field value by name: {}", fieldName, e);
        }
        return result;
    }

    public Map<String, Set<IncidentSummary>> toTrimmedMap(int totalIncidents) {
        Map<String, Set<IncidentSummary>> result = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType() == List.class) {
                    @SuppressWarnings("unchecked") List<IncidentSummary> incidentSummary = (List<IncidentSummary>)f.get(this);
                    if (!incidentSummary.isEmpty() && incidentSummary.size() < totalIncidents * MISSING_THRESHOLD) {
                        result.put(f.getName(), new HashSet<>((incidentSummary)));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error("Error attempting to retrieve InvalidIncidents fields", e);
            }
        }
        return result;
    }
}
