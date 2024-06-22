package FowlFlightForensics.domain.dto;

public record IncidentKey(Integer year, Integer month, String speciesId, String speciesName,
                          Boolean aircraftDamage) {
}
