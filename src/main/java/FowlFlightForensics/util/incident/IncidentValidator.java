package FowlFlightForensics.util.incident;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.domain.InvalidIncidents;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.enums.MappingType;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.CommandUtils;
import FowlFlightForensics.util.incident.rules.*;
import FowlFlightForensics.util.string.CaseTransformer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Component
@Getter
@Setter
public class IncidentValidator extends BaseComponent {
    @SuppressWarnings("unchecked") private final Map<String, List<ValidationRule<Object>>> validationRules = Stream.of(new Object[][] {
        { "Year", List.of(new NotNullValidationRule(), new RangeValidationRule(1990, 2015)) },
        { "Month", List.of(new NotNullValidationRule(), new RangeValidationRule(1, 12)) },
        { "Day", List.of(new NotNullValidationRule(), new RangeValidationRule(1, 31)) },
        { "Aircraft", List.of(new ValueValidationRule("UNKNOWN")) },
        { "AircraftMass", List.of(new NotNullValidationRule()) },
        { "Engines", List.of(new NotNullValidationRule()) },
        { "AirportId", List.of(new RegexValidationRule("-?\\d+(\\.\\d+)?"), new ValueValidationRule("UNKN")) },
        { "AirportName", List.of(new NotEmptyValidationRule()) },
        { "State", List.of(new NotEmptyValidationRule()) },
        { "FaaRegion", List.of(new NotEmptyValidationRule()) },
        { "WarningIssued", List.of(new NotNullValidationRule()) },
        { "FlightPhase", List.of(new NotEmptyValidationRule()) },
        { "SpeciesId", List.of(new ValueValidationRule("100000000000"), new ValueValidationRule("UNK")) },
        { "SpeciesName", List.of(new NotEmptyValidationRule(), new ValueValidationRule("UNKNOWN")) },
        { "SpeciesQuantity", List.of(new NotEmptyValidationRule()) },
        { "Fatalities", List.of(new NotNullValidationRule()) },
        { "Injuries", List.of(new NotNullValidationRule()) }
    }).collect(toMap(data -> (String) data[0], data -> (List<ValidationRule<Object>>) data[1]));
    @SuppressWarnings("unchecked") private Map<String, List<ValidationRule<Object>>> summaryValidationRules = Stream.of(new Object[][] {
        { "SpeciesQuantityMin", List.of(new NotNullValidationRule()) },
        { "SpeciesQuantityMax", List.of(new NotNullValidationRule()) },
    }).collect(toMap(data -> (String) data[0], data -> (List<ValidationRule<Object>>) data[1]));

    public List<IncidentSummary> incidentSummaryList = new ArrayList<>();
    public Map<String, Set<IncidentSummary>> invalidIncidentsTrimmedMap = new HashMap<>();
    public Map<String, String> airports = new HashMap<>();
    public Map<String, String> species = new HashMap<>();
    public List<String> unknownSpeciesIds = new ArrayList<>();
    public List<String> unknownSpeciesNames = new ArrayList<>();
    public Map<MappingType, Map<String, Set<String>>> multiCodeCorrelations = new EnumMap<>(MappingType.class);

    private InvalidIncidents invalidIncidents = new InvalidIncidents();

    public void validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        incidentSummaryList = validateAndGenerateSummary(incidentDetails);
        invalidIncidentsTrimmedMap = invalidIncidents.toTrimmedMap(incidentSummaryList.size());
        summaryValidationRules = generateSummaryValidationRules(validationRules, invalidIncidentsTrimmedMap);

        airports = validateAndGenerateMap(MappingType.AIRPORTS, incidentSummaryList, IncidentSummary::getAirportId, IncidentSummary::getAirportName);
        species = validateAndGenerateMap(MappingType.SPECIES, incidentSummaryList, IncidentSummary::getSpeciesId, IncidentSummary::getSpeciesName);

        logger.info("Getting distinct lists of unknown species ids and names ...");
        unknownSpeciesIds = species.keySet().stream().filter(i -> i.contains("UNK")).toList();
        unknownSpeciesNames = species.values().stream().filter(i -> i.contains("UNKNOWN")).toList();
    }

    public Set<InvalidIncidentTopic> validateIncidentSummary(IncidentSummary summary) {
        logger.trace("Applying summary validation rules - {}", summary.toString());
        Map<String, IncidentSummary> resultMap = new HashMap<>();
        for (String ruleName : summaryValidationRules.keySet().stream().toList()) {
            for (ValidationRule<Object> vr : summaryValidationRules.get(ruleName)) {
                Object fieldVal = summary.getFieldValueByName(CaseTransformer.toLowerFirstChar(ruleName));
                validateAndPutField(fieldVal, vr, resultMap, ruleName, summary, Map::put);
            }
        }
        return resultMap.keySet().stream().map(InvalidIncidentTopic::fromString).collect(toSet());
    }

    // region [Object Validators]
    private List<IncidentSummary> validateAndGenerateSummary(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = new ArrayList<>();
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
                validateAndAddField(incident.getFieldValueByName(CaseTransformer.toLowerFirstChar(ruleName)),
                        vr,
                        invalidIncidents.getFieldValueByName("invalid" + ruleName),
                        summary, List::add);
            }
        }
    }

    private <T> Map<String, String> validateAndGenerateMap(MappingType type, List<T> list, Function<T, String> valueExtractor1, Function<T, String> valueExtractor2) {
        logger.info("Comparing id-to-names with name-to-ids {} Maps, to see if other invalid information exists ...", type);
        Map<String, Set<String>> idToNamesMap = extractKeyValuePairs(list, valueExtractor1, valueExtractor2);
        Map<String, Set<String>> nameToIdsMap = extractKeyValuePairs(list, valueExtractor2, valueExtractor1);
        if (idToNamesMap.size() > nameToIdsMap.size()) {
            multiCodeCorrelations.put(type, nameToIdsMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()))
                    .stream().collect(toMap(Entry::getKey, Entry::getValue)));
            logger.warn("Checking {} Maps found {} in [nameToIdsMap]", type, multiCodeCorrelations.get(type).toString());
        } else if (idToNamesMap.size() < nameToIdsMap.size()) {
            multiCodeCorrelations.put(type, idToNamesMap.entrySet().stream().collect(filtering(i -> i.getValue().size() > 1, toList()))
                    .stream().collect(toMap(Entry::getKey, Entry::getValue)));
            logger.warn("Checking {} Maps found {} in [idToNamesMap]", type, multiCodeCorrelations.get(type).toString());
        }
        return transformToMapOfStrings(idToNamesMap);
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
                    : (qty.contains("-") ? Integer.parseInt(qty.substring(qty.indexOf("-") + 1)) : Integer.parseInt(qty)));
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

    private Map<String, List<ValidationRule<Object>>> generateSummaryValidationRules(Map<String, List<ValidationRule<Object>>> validationRules,
                                                                                     Map<String, Set<IncidentSummary>> invalidIncidentsTrimmedMap) {
        Map<String, List<ValidationRule<Object>>> result = summaryValidationRules;
        if (!invalidIncidentsTrimmedMap.containsKey("invalidSpeciesQuantity")) {
            result.remove("SpeciesQuantityMin");
            result.remove("SpeciesQuantityMax");
        }
        for (String ruleName : validationRules.keySet().stream().toList()) {
            if (invalidIncidentsTrimmedMap.containsKey("invalid" + ruleName) && !ruleName.toLowerCase().contains("quantity")) {
                result.put(ruleName, validationRules.get(ruleName));
            }
        }
        return result;
    }
    // endregion

    // region [Value Validators]
    private <T, R> void validateAndAddField(Object value, ValidationRule<Object> rule, T obj, R summary,
                                            CommandUtils.AddingFunction<T, R> func) {
        if (rule != null && !rule.isValid(value)) {
            func.add(obj, summary);
        }
    }

    private <T, K, V> void validateAndPutField(Object value, ValidationRule<Object> rule, T obj, K key, V summary,
                                               CommandUtils.PuttingFunction<T, K, V> func) {
        if (rule != null && !rule.isValid(value)) {
            func.put(obj, key, summary);
        }
    }
    // endregion

    // region [Util]
    private <T, K, V> Map<K, Set<V>> extractKeyValuePairs(List<T> list, Function<T, K> keyExtractor,
                                                          Function<T, V> valueExtractor) {
        return list.stream()
                .collect(groupingBy(keyExtractor, mapping(valueExtractor, toSet())));
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
