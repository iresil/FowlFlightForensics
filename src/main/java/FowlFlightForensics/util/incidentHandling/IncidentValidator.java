package FowlFlightForensics.util.incidentHandling;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.domain.InvalidIncidents;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Component
public class IncidentValidator {
    public InvalidIncidents invalidIncidents;

    public List<IncidentSummary> validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = validateAndGenerateSummary(incidentDetails);

        Map<String, Set<String>> airports = extractKeyValuePairs(incidentSummaryList, IncidentSummary::airportId, IncidentSummary::airport);
        Map<String, String> airportsMap = transformToMapOfStrings(airports);
        Map<String, Set<String>> airportsToCheck = extractKeyValuePairs(incidentSummaryList, IncidentSummary::airport, IncidentSummary::airportId);
        var multiCodeAirports = airportsToCheck.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));

        Map<String, Set<String>> species = extractKeyValuePairs(incidentSummaryList, IncidentSummary::speciesId, IncidentSummary::speciesName);
        Map<String, String> speciesMap = transformToMapOfStrings(species);
        Map<String, Set<String>> speciesToCheck = extractKeyValuePairs(incidentSummaryList, IncidentSummary::speciesName, IncidentSummary::speciesId);
        var multiCodeSpecies = speciesToCheck.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));

        return incidentSummaryList;
    }

    private List<IncidentSummary> validateAndGenerateSummary(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = new ArrayList<>();
        invalidIncidents = new InvalidIncidents();
        for (IncidentDetails incident : incidentDetails) {
            String qty = incident.getSpeciesQuantity();
            Pair<Integer, Integer> qtyRange = parseSpeciesQuantity(qty);

            IncidentSummary summary = extractSummaryFromIncidentDetails(incident, qtyRange);
            incidentSummaryList.add(summary);

            validateByRange(incident.getIncidentYear(), 1990, 2015, invalidIncidents.invalidYears, summary, List::add);
            validateByRange(incident.getIncidentMonth(), 1, 12, invalidIncidents.invalidMonths, summary, List::add);
            validateByRange(incident.getIncidentDay(), 1, 31, invalidIncidents.invalidDays, summary, List::add);
            validateByValue(incident.getAircraft(), "UNKNOWN", invalidIncidents.invalidAircraft, summary, List::add);
            validateNotNull(incident.getAircraftMass(), invalidIncidents.invalidAircraftMass, summary, List::add);
            validateNotNull(incident.getEngines(), invalidIncidents.invalidEngines, summary, List::add);
            validateByRegex(incident.getAirportId(), "-?\\d+(\\.\\d+)?", invalidIncidents.invalidAirportIds, summary, List::add);
            validateByRegex(incident.getAirportId(), "UNKN", invalidIncidents.invalidAirportIds, summary, List::add);
            validateNotEmpty(incident.getAirport(), invalidIncidents.invalidAirportNames, summary, List::add);
            validateNotEmpty(incident.getState(), invalidIncidents.invalidStates, summary, List::add);
            validateNotEmpty(incident.getFaaRegion(), invalidIncidents.invalidFaaRegions, summary, List::add);
            validateNotNull(incident.getWarningIssued(), invalidIncidents.invalidWarningIssued, summary, List::add);
            validateNotEmpty(incident.getFlightPhase(), invalidIncidents.invalidFlightPhases, summary, List::add);
            validateByValue(incident.getSpeciesId(), "100000000000", invalidIncidents.invalidSpeciesIds, summary, List::add);
            validateNotEmpty(incident.getSpeciesName(), invalidIncidents.invalidSpeciesNames, summary, List::add);
            validateNotEmpty(incident.getSpeciesQuantity(), invalidIncidents.invalidSpeciesQuantities, summary, List::add);
            validateNotNull(incident.getFatalities(), invalidIncidents.invalidFatalities, summary, List::add);
            validateNotNull(incident.getInjuries(), invalidIncidents.invalidInjuries, summary, List::add);
        }
        return incidentSummaryList;
    }

    private Pair<Integer, Integer> parseSpeciesQuantity(String qty) {
        Integer qtyMin = null;
        Integer qtyMax = null;
        if (!qty.isEmpty()) {
            qtyMin = (qty.contains("Over ")
                    ? Integer.parseInt(qty.replace("Over ", ""))
                    : (qty.contains("-") ? Integer.parseInt(qty.substring(0, qty.indexOf("-"))) : Integer.parseInt(qty)));
            qtyMax = (qty.contains("Over ")
                    ? 10000
                    : (qty.contains("-") ? Integer.parseInt(qty.substring(qty.indexOf("-"))) : Integer.parseInt(qty)));
        }
        return new ImmutablePair<>(qtyMin, qtyMax);
    }

    private IncidentSummary extractSummaryFromIncidentDetails(IncidentDetails incident, Pair<Integer, Integer> qtyRange) {
        return new IncidentSummary(incident.getRecordId(), incident.getIncidentYear(),
                incident.getIncidentMonth(), incident.getIncidentDay(), incident.getAircraft(),
                incident.getAircraftMass(), incident.getEngines(), incident.getAirportId(), incident.getAirport(),
                incident.getState(), incident.getFaaRegion(), incident.getWarningIssued(), incident.getFlightPhase(),
                incident.getSpeciesId(), incident.getSpeciesName(), qtyRange.getKey(), qtyRange.getValue(),
                incident.getFatalities(), incident.getInjuries(), incident.getAircraftDamage());
    }

    // region [Validation]
    @FunctionalInterface
    interface AddingFunction<T, R> {
        void add(T target, R value);
    }

    private <T, R> void validateByRange(Integer value, Integer start, Integer end, T obj, R summary,
                                        AddingFunction<T, R> func) {
        if (value < start || value > end) {
            func.add(obj, summary);
        }
    }

    private <T, R> void validateByValue(String value, String toCompare, T obj, R summary, AddingFunction<T, R> func) {
        if (value.equals(toCompare)) {
            func.add(obj, summary);
        }
    }

    private <T, R> void validateByRegex(String value, String regex, T obj, R summary, AddingFunction<T, R> func) {
        if (value.matches(regex)) {
            func.add(obj, summary);
        }
    }

    private <T, R> void validateNotNull(Object value, T obj, R summary, AddingFunction<T, R> func) {
        if (value == null) {
            func.add(obj, summary);
        }
    }

    private <T, R> void validateNotEmpty(String value, T obj, R summary, AddingFunction<T, R> func) {
        if (value.isEmpty()) {
            func.add(obj, summary);
        }
    }
    // endregion

    // region [Util]
    private <T, K, V> Map<K, Set<V>> extractKeyValuePairs(List<T> list, Function<T, K> keyExtractor,
                                                               Function<T, V> valueExtractor) {
        return list.stream()
                .collect(groupingBy(keyExtractor, Collectors.mapping(valueExtractor, Collectors.toSet())));
    }

    private Map<String, String> transformToMapOfStrings(Map<String, Set<String>> param) {
        Map<String, String> result = null;
        if (param.values().stream().map(Set::size).noneMatch(i -> i > 1)) {
            result = param.entrySet().stream().collect(toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream().findFirst().orElse("")
            ));
        }
        return result;
    }
    // endregion
}
