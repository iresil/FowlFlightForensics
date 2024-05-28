package FowlFlightForensics.domain;

public record IncidentKey(Integer incidentYear, Integer incidentMonth, String speciesId, String speciesName,
                          Boolean aircraftDamage) {
}
