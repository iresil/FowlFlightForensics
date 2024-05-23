package FowlFlightForensics.util.incident;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.domain.InvalidIncidents;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.incident.rules.*;
import FowlFlightForensics.util.string.Transformer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Component
public class IncidentValidator extends BaseComponent {
    public InvalidIncidents invalidIncidents;

    @SuppressWarnings("unchecked") Map<String, List<ValidationRule<Object>>> validationRules = Stream.of(new Object[][] {
        { "Year", List.of(new RangeValidationRule(1990, 2015))},
        { "Month", List.of(new RangeValidationRule(1, 12))},
        { "Day", List.of(new RangeValidationRule(1, 31))},
        { "Aircraft", List.of(new ValueValidationRule("UNKNOWN"))},
        { "AircraftMass", List.of(new NotNullValidationRule())},
        { "Engines", List.of(new NotNullValidationRule())},
        { "AirportId", List.of(new RegexValidationRule("-?\\d+(\\.\\d+)?"), new ValueValidationRule("UNKN")) },
        { "AirportName", List.of(new NotEmptyValidationRule())},
        { "State", List.of(new NotEmptyValidationRule())},
        { "FaaRegion", List.of(new NotEmptyValidationRule())},
        { "WarningIssued", List.of(new NotNullValidationRule())},
        { "FlightPhase", List.of(new NotEmptyValidationRule())},
        { "SpeciesId", List.of(new ValueValidationRule("100000000000")/*, new ValueValidationRule("UNK")*/) },
        { "SpeciesName", List.of(new NotEmptyValidationRule()/*, new ValueValidationRule("UNKNOWN")*/) },
        { "SpeciesQuantity", List.of(new NotEmptyValidationRule())},
        { "Fatalities", List.of(new NotNullValidationRule())},
        { "Injuries", List.of(new NotNullValidationRule())}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (List<ValidationRule<Object>>) data[1]));

    public List<IncidentSummary> validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = validateAndGenerateSummary(incidentDetails);

        Map<String, Set<IncidentSummary>> invalidIncidentsTrimmedMap = invalidIncidents.toTrimmedMap(incidentSummaryList.size());

        logger.info("Comparing id-to-names with name-to-ids airport Maps, to see if other invalid information exists ...");
        Map<String, Set<String>> airportIdToNamesMap = extractKeyValuePairs(incidentSummaryList, IncidentSummary::airportId, IncidentSummary::airport);
        Map<String, Set<String>> airportNameToIdsMap = extractKeyValuePairs(incidentSummaryList, IncidentSummary::airport, IncidentSummary::airportId);
        List<Entry<String, Set<String>>> multiCodeAirports = null;
        if (airportIdToNamesMap.size() > airportNameToIdsMap.size()) {
            multiCodeAirports = airportNameToIdsMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));
        } else {
            multiCodeAirports = airportIdToNamesMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));
        }
        Map<String, String> airports = transformToMapOfStrings(airportIdToNamesMap);

        logger.info("Comparing id-to-names with name-to-ids species Maps, to see if other invalid information exists ...");
        Map<String, Set<String>> speciesIdToNamesMap = extractKeyValuePairs(incidentSummaryList, IncidentSummary::speciesId, IncidentSummary::speciesName);
        Map<String, Set<String>> speciesNameToIdsMap = extractKeyValuePairs(incidentSummaryList, IncidentSummary::speciesName, IncidentSummary::speciesId);
        List<Entry<String, Set<String>>> multiCodeSpecies = null;
        if (speciesIdToNamesMap.size() > speciesNameToIdsMap.size()) {
            multiCodeSpecies = speciesNameToIdsMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));
        } else {
            multiCodeSpecies = speciesIdToNamesMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()));
        }
        Map<String, String> species = transformToMapOfStrings(speciesIdToNamesMap);

        logger.info("Getting distinct lists of unknown species ids and names ...");
        List<String> unknownSpeciesIds = species.keySet().stream().filter(i -> i.contains("UNK")).toList();
        List<String> unknownSpeciesNames = speciesNameToIdsMap.keySet().stream().filter(i -> i.contains("UNKNOWN")).toList();

        return incidentSummaryList;
    }

    // region [Object Validators]
    private List<IncidentSummary> validateAndGenerateSummary(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = new ArrayList<>();
        invalidIncidents = new InvalidIncidents();
        logger.info("Parsing ranges and calculating Summary ...");
        for (IncidentDetails incident : incidentDetails) {
            String qty = incident.getSpeciesQuantity();
            Pair<Integer, Integer> qtyRange = parseSpeciesQuantity(qty);

            IncidentSummary summary = extractSummaryFromIncidentDetails(incident, qtyRange);
            incidentSummaryList.add(summary);

            validateIncidentFields(incident, summary);
        }
        logger.info("Summary generation and range parsing process finished.");
        return incidentSummaryList;
    }

    private void validateIncidentFields(IncidentDetails incident, IncidentSummary summary) {
        logger.trace("Applying validation rules ...");
        for (String ruleName : validationRules.keySet().stream().toList()) {
            for (ValidationRule<Object> vr : validationRules.get(ruleName)) {
                validateField(incident.getFieldValueByName(Transformer.toLowerFirstChar(ruleName)),
                        vr,
                        invalidIncidents.getFieldValueByName("invalid" + ruleName),
                        summary, List::add);
            }
        }
    }
    // endregion

    // region [Transformers]
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
        return new IncidentSummary(incident.getRecordId(), incident.getYear(),
                incident.getMonth(), incident.getDay(), incident.getAircraft(),
                incident.getAircraftMass(), incident.getEngines(), incident.getAirportId(), incident.getAirportName(),
                incident.getState(), incident.getFaaRegion(), incident.getWarningIssued(), incident.getFlightPhase(),
                incident.getSpeciesId(), incident.getSpeciesName(), qtyRange.getKey(), qtyRange.getValue(),
                incident.getFatalities(), incident.getInjuries(), incident.getAircraftDamage());
    }
    // endregion

    // region [Value Validators]
    @FunctionalInterface
    interface AddingFunction<T, R> {
        void add(T target, R value);
    }

    private <T, R> void validateField(Object value, ValidationRule<Object> rule, T obj, R summary,
                                      AddingFunction<T, R> func) {
        if (rule != null && !rule.isValid(value)) {
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
                    Entry::getKey,
                    entry -> entry.getValue().stream().findFirst().orElse("")
            ));
        }
        return result;
    }
    // endregion
}
