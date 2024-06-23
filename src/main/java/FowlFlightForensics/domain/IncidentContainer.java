package FowlFlightForensics.domain;

import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.enums.MappingType;
import FowlFlightForensics.util.incident.IncidentValidator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A singleton implementation based on an {@code enum} wrapper that contains the base {@code IncidentValidator} object
 * of the application.
 */
public enum IncidentContainer {
    INSTANCE(new IncidentValidator());

    private IncidentValidator incidentValidator;

    private IncidentContainer(IncidentValidator incidentValidator) {
        this.incidentValidator = incidentValidator;
    }

    /**
     * To be called whenever we need to access the {@code IncidentContainer} instance, through which we can make calls to
     * {@code IncidentValidator}.
     * @return A (new or existing) {@code IncidentContainer} instance.
     */
    public IncidentContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Applies all configured validations to a {@code List} of {@code IncidentDetails} objects and then does the following:
     * <ul>
     *   <li>transforms the input to a {@code List} of {@code IncidentSummary} objects
     *   <li>adds {@code IncidentSummary} objects that have been identified as invalid to the corresponding field of an
     *   {@code InvalidIncidents} object
     *   <li>creates a {@code Map} containing all invalid incidents per validation rule, with the purpose of counting the
     *   total number of invalid incidents per rule and deciding on the final validations to apply in {@code KStreams}
     *   based on that (the {@code Map} is filtered based on whether we've set the validations in strict mode or not)
     *   <li>creates a slightly modified version of the validation rules, to be applied to {@code IncidentSummary} objects
     *   <li>generates two separate {@code Map}s (one per species and one per airport), which under normal circumstances would be
     *   separate tables in a database, but in this case they were included as flat values in the input CSV
     *   <li>identifies unknown species ids and names (to be used for debugging)
     * </ul>
     * @param incidentDetails The raw {@code List} of {@code IncidentDetails} objects, that will undergo transformations.
     */
    public void validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        incidentValidator.validateAndTransformIncidents(incidentDetails);
    }

    /**
     * This is the method that gets called from within {@code KStreams}, to validate transferred objects. It returns a
     * {@code Set} of topics, denoting to which topic each object should be sent, based on the validations that failed.
     * @param summary The {@code IncidentSummary} object to be validated.
     * @return The invalid data topics to which the input object should be sent. If the input object is considered valid,
     * then the returned {@code Set} will be empty.
     */
    public Set<InvalidIncidentTopic> validateIncidentSummary(IncidentSummary summary) {
        return incidentValidator.validateIncidentSummary(summary);
    }

    /**
     * Gets the {@code List} of {@code IncidentSummary} objects to which the original {@code List} of {@code IncidentDetails}
     * objects has been transformed.
     * @return The {@code List} of incidents, transformed to {@code IncidentSummary} objects.
     */
    public List<IncidentSummary> getIncidentSummaryList() {
        return incidentValidator.getIncidentSummaryList();
    }

    /**
     * Replaces the contents of the original {@code List} of {@code IncidentSummary} objects, in {@code IncidentValidator}.
     * @param incidentSummaryList The new value that's going to replace the old one.
     */
    public void setIncidentSummaryList(List<IncidentSummary> incidentSummaryList) {
        incidentValidator.setIncidentSummaryList(incidentSummaryList);
    }

    /**
     * Retrieves the mapping between validation rules and incidents found invalid based on that rule. To be used during
     * debugging.
     * @return A {@code Map} with the validation name as key and a {@code Set} of {@code IncidentSummary} objects as value.
     */
    public Map<String, Set<IncidentSummary>> getInvalidIncidentsTrimmedMap() {
        return incidentValidator.getInvalidIncidentsTrimmedMap();
    }

    /**
     * Gets the mapping between airport ids and airport names.
     * @return A {@code Map} of {@code String}s, where the keys are the ids and the values are the airport names.
     */
    public Map<String, String> getAirports() {
        return incidentValidator.getAirports();
    }

    /**
     * Gets the mapping between species ids and species names.
     * @return A {@code Map} of {@code String}s, where the keys are the ids and the values are the species names.
     */
    public Map<String, String> getSpecies() {
        return incidentValidator.getSpecies();
    }

    /**
     * Retrieves airport and species mappings, for which either one single id corresponds to multiple names, or vice-versa.
     * @return A {@code Map} where each key corresponds to either "species" or "airports", and the values are separate
     * {@code Map}s, each of which can contain either single id to multiple names or single name to multiple ids correlations.
     */
    public Map<MappingType, Map<String, Set<String>>> getMultiCodeCorrelations() {
        return incidentValidator.getMultiCodeCorrelations();
    }
}
