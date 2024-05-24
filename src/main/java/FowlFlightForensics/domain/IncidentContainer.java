package FowlFlightForensics.domain;

import FowlFlightForensics.util.incident.IncidentValidator;

import java.util.List;

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
}
