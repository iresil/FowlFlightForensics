package FowlFlightForensics.domain.dto;

/**
 * A {@code record} that is utilized for grouping incidents by year and species.
 * @param year The year during which the incident took place.
 * @param speciesId The unique identifier of the species that caused the incident.
 * @param speciesName The name of the species that caused the incident.
 */
public record IncidentGrouped (Integer year, String speciesId, String speciesName) {
}
