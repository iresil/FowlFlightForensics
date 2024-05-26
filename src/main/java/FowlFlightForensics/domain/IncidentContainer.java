package FowlFlightForensics.domain;

import FowlFlightForensics.util.incident.IncidentValidator;

import java.util.List;
import java.util.Map;
import java.util.Set;

public enum IncidentContainer {
    INSTANCE(new IncidentValidator());

    private IncidentValidator incidentValidator;

    private IncidentContainer(IncidentValidator incidentValidator) {
        this.incidentValidator = incidentValidator;
    }

    public IncidentContainer getInstance() {
        return INSTANCE;
    }

    public void validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        incidentValidator.validateAndTransformIncidents(incidentDetails);
    }

    public List<IncidentSummary> getIncidentSummaryList() {
        return incidentValidator.getIncidentSummaryList();
    }

    public void setIncidentSummaryList(List<IncidentSummary> incidentSummaryList) {
        incidentValidator.setIncidentSummaryList(incidentSummaryList);
    }

    public Map<String, Set<IncidentSummary>> getInvalidIncidentsTrimmedMap() {
        return incidentValidator.getInvalidIncidentsTrimmedMap();
    }

    public Map<String, String> getAirports() {
        return incidentValidator.getAirports();
    }

    public Map<String, String> getSpecies() {
        return incidentValidator.getSpecies();
    }

    public Map<String, Map<String, Set<String>>> getMultiCodeCorrelations() {
        return incidentValidator.getMultiCodeCorrelations();
    }
}
