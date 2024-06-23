package FowlFlightForensics.domain.dto;

/**
 * A {@code record} that is used as a Key during the early {@code KStreams} filtering operations.
 * @param year The year during which the incident took place.
 * @param month The month during which the incident took place.
 * @param speciesId The unique identifier of the species that caused the incident.
 * @param speciesName The name of the species that caused the incident.
 * @param aircraftDamage Signifies whether the incident resulted in aircraft damage or not.
 */
public record IncidentKey(Integer year, Integer month, String speciesId, String speciesName,
                          Boolean aircraftDamage) {
}
