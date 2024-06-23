package FowlFlightForensics.domain.dto;

/**
 * A {@code record} that is used during the top N calculation, and holds the final entry that will be exported as a single
 * row in the output CSV file.
 * @param index The ranking of each object within the year in question.
 * @param year The year during which the incidents took place.
 * @param speciesId The unique identifier of the species that caused the incidents.
 * @param speciesName The name of the species that caused the incidents.
 * @param amount The number of incidents related to the species in question, that were observed during the year in question.
 */
public record IncidentRanked (Integer index, Integer year, String speciesId, String speciesName, Long amount) {
}
