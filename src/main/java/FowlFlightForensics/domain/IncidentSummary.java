package FowlFlightForensics.domain;

public record IncidentSummary(Integer recordId, Integer incidentYear, Integer incidentMonth, Integer incidentDay,
                              String aircraft, Float aircraftMass, Integer engines, String airportId, String airport,
                              String state, String faaRegion, Boolean warningIssued, String flightPhase,
                              String speciesId, String speciesName, Integer speciesQuantityMin,
                              Integer speciesQuantityMax, Integer fatalities, Integer injuries, Boolean aircraftDamage) {
    public IncidentKey getKey() {
        return new IncidentKey(incidentYear, incidentMonth, speciesId, speciesName, aircraftDamage);
    }
}
