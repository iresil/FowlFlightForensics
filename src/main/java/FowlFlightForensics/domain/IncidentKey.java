package FowlFlightForensics.domain;

public record IncidentKey(Integer year, Integer month, String speciesId, String speciesName,
                          Boolean aircraftDamage) {
}
