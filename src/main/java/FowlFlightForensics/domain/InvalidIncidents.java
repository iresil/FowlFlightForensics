package FowlFlightForensics.domain;

import FowlFlightForensics.util.BaseComponent;

import java.lang.reflect.Field;
import java.util.*;

public class InvalidIncidents extends BaseComponent {
    private final float MISSING_THRESHOLD = 0.03f;

    public List<IncidentSummary> invalidYears = new ArrayList<>();
    public List<IncidentSummary> invalidMonths = new ArrayList<>();
    public List<IncidentSummary> invalidDays = new ArrayList<>();
    public List<IncidentSummary> invalidAircraft = new ArrayList<>();
    public List<IncidentSummary> invalidAircraftMass = new ArrayList<>();
    public List<IncidentSummary> invalidEngines = new ArrayList<>();
    public List<IncidentSummary> invalidAirportIds = new ArrayList<>();
    public List<IncidentSummary> invalidAirportNames = new ArrayList<>();
    public List<IncidentSummary> invalidStates = new ArrayList<>();
    public List<IncidentSummary> invalidFaaRegions = new ArrayList<>();
    public List<IncidentSummary> invalidWarningIssued = new ArrayList<>();
    public List<IncidentSummary> invalidFlightPhases = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesIds = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesNames = new ArrayList<>();
    public List<IncidentSummary> invalidSpeciesQuantities = new ArrayList<>();
    public List<IncidentSummary> invalidFatalities = new ArrayList<>();
    public List<IncidentSummary> invalidInjuries = new ArrayList<>();

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
