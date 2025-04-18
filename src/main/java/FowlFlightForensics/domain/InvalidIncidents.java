package FowlFlightForensics.domain;

import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.Consts;

import java.lang.reflect.Field;
import java.util.*;

/**
 * An {@code InvalidIncidents} object contains all {@code IncidentSummary} objects per validation rule. It was utilized
 * during the first runs of the application, to determine how strict the selected rulesets should be.
 */
public class InvalidIncidents extends BaseComponent {
    private final float MISSING_THRESHOLD =
            Consts.INVALID_INCIDENTS_FILTER_STRICT ? Consts.INVALID_INCIDENTS_PERCENTAGE_LIMIT_MAX : Consts.INVALID_INCIDENTS_PERCENTAGE_LIMIT_MIN;

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

    /**
     * Utilizes reflection to retrieve a field's value, provided that the field's name matches the {@code fieldName}
     * parameter.
     * @param fieldName The name of the field whose value we need to retrieve.
     * @return A {@code List} of {@code IncidentSummary} objects.
     */
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

    /**
     * Gets a subset of the invalid incidents contained within this object, based on the selected strictness of the configured
     * invalid percentage thresholds.
     * @param totalIncidents The total number of incidents contained in the original CSV file.
     * @return A {@code Map} containing the field name as key and the corresponding {@code Set} of {@code IncidentSummary}
     * objects as value, where the total size of each {@code Set} can't be larger than {@code MISSING_THRESHOLD}.
     */
    public Map<String, Set<IncidentSummary>> toTrimmedMap(int totalIncidents) {
        Map<String, Set<IncidentSummary>> result = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        logger.info("Generating Map of invalid incidents per type, trimmed to contain types with incident count less than {}% of total.", MISSING_THRESHOLD * 100);
        for (Field f : fields) {
            try {
                if (f.getType() == List.class) {
                    @SuppressWarnings("unchecked") List<IncidentSummary> incidentSummary = (List<IncidentSummary>)f.get(this);
                    if (!incidentSummary.isEmpty() && incidentSummary.size() < totalIncidents * MISSING_THRESHOLD) {
                        result.put(f.getName(), new HashSet<>((incidentSummary)));
                        logger.trace("{} added to trimmed map of invalid incidents.", f.getName());
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error("Error attempting to retrieve InvalidIncidents fields", e);
            }
        }
        logger.info("Generation of trimmed map containing invalid incidents has finished.");
        return result;
    }
}
