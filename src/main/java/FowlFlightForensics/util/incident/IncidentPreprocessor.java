package FowlFlightForensics.util.incident;

import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.util.BaseComponent;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.Map.*;

@Component
public class IncidentPreprocessor extends BaseComponent {
    public List<IncidentSummary> applyTransformations(List<IncidentSummary> incidentSummaryList, Map<String, String> airports,
                                                      Map<String, String> species, Map<String, Map<String, Set<String>>> multiCodeCorrelations) {
        for (Entry<String, Map<String, Set<String>>> set : multiCodeCorrelations.entrySet()) {
            String typeKey = set.getKey();
            Map<String, Set<String>> multiCodeCorrelation = set.getValue();

            Map<String, Boolean> isIdToNameMap = checkMultiCodeCorrelationTypes(multiCodeCorrelation, typeKey, airports,
                    species);

            for (int i = 0; i < incidentSummaryList.size(); i++) {
                IncidentSummary obj = incidentSummaryList.get(i);
                Boolean isIdToName = isIdToNameMap.get(typeKey);
                Map<String, String> mapping = new HashMap<>();
                if (typeKey.equals("airports")) {
                    mapping = airports;
                } else if (typeKey.equals("species")) {
                    mapping = species;
                }
                replaceIncidentInList(typeKey, isIdToName, multiCodeCorrelation, mapping, incidentSummaryList, i, obj,
                        List::set);
            }
        }
        return incidentSummaryList;
    }

    private Map<String, Boolean> checkMultiCodeCorrelationTypes(Map<String, Set<String>> multiCodeCorrelation,
                                                                String type, Map<String, String> airports,
                                                                Map<String, String> species) {
        Map<String, Boolean> isIdToNameMap = new HashMap<>();
        for (Entry<String, Set<String>> innerSet : multiCodeCorrelation.entrySet()) {
            String key = innerSet.getKey();
            Set<String> value = innerSet.getValue();

            Boolean isIdToName = checkIfMapIsIdToName(type, airports, species, key, value);

            isIdToNameMap.put(type, isIdToName);
            logger.info("Obj: {} -> isIdToName: {}", innerSet, isIdToName);
        }
        return isIdToNameMap;
    }

    private Boolean checkIfMapIsIdToName(String type, Map<String, String> airports, Map<String, String> species,
                                         String key, Set<String> value) {
        Boolean isIdToName = null;
        for (String val : value) {
            Boolean itemIsId = checkIfValueIsId(type, airports, species, key, val);

            if (itemIsId != null) {
                if (isIdToName == null) {
                    isIdToName = itemIsId;
                } else {
                    isIdToName &= itemIsId;
                }
            }
        }
        return isIdToName;
    }

    private Boolean checkIfValueIsId(String type, Map<String, String> airports, Map<String, String> species,
                                    String key, String val) {
        Boolean itemIsId = null;
        boolean keyIsId = false, keyIsValue = false, valueIsId = false, valueIsValue = false;
        if (type.equals("airports")) {
            keyIsId = airports.containsKey(key);
            keyIsValue = airports.containsValue(key);

            valueIsId = airports.containsKey(val);
            valueIsValue = airports.containsValue(val);
        } else if (type.equals("species")) {
            keyIsId = species.containsKey(key);
            keyIsValue = species.containsValue(key);

            valueIsId = species.containsKey(val);
            valueIsValue = species.containsValue(val);
        }

        if (keyIsId != keyIsValue && valueIsId != valueIsValue) {
            if (keyIsId != valueIsId) {
                itemIsId = keyIsId;
            }
        } else if (keyIsId != keyIsValue) {
            itemIsId = keyIsId;
        } else if (valueIsId != valueIsValue) {
            itemIsId = valueIsValue;
        }

        return itemIsId;
    }

    @FunctionalInterface
    interface SettingFunction<T, K, V> {
        void set(T target, K index, V value);
    }

    @SuppressWarnings("unchecked")
    private <T, K, V> void replaceIncidentInList(String type, Boolean isIdToNameMap, Map<String, Set<String>> multiCodeCorrelation,
                                              Map<String, String> mapping, T incidentSummaryList, K i, V incidentSummary,
                                              SettingFunction<T, K, V> func) {
        for (String k : multiCodeCorrelation.keySet()) {
            for (String v : multiCodeCorrelation.get(k).stream().toList()) {
                if (mapping.containsKey(v) && mapping.containsValue(v)) {
                    incidentSummary = (V)replaceIncidentIfNecessary(type, isIdToNameMap,
                            (IncidentSummary)incidentSummary, mapping, v);
                    func.set(incidentSummaryList, i, incidentSummary);
                }
            }
        }
    }

    private IncidentSummary replaceIncidentIfNecessary(String type, Boolean isIdToNameMap, IncidentSummary incidentSummary,
                                                       Map<String, String> mapping, String val) {
        String newKey = null;
        String newValue = null;
        if (isIdToNameMap && incidentSummary.airport().equals(val)) {  // val is id, treated as a name
            String entry = mapping.get(val);  // Find the actual name to replace val
            if (entry != null) {
                newKey = val;
                newValue = entry;
            }
        } else if (!isIdToNameMap && incidentSummary.airportId().equals(val)) {  // val is name, treated as an id
            // Look up val as name, and retrieve the entry, if present
            Optional<Entry<String, String>> entry = mapping.entrySet().stream().filter(item -> item.getValue().equals(val)).findFirst();
            if (entry.isPresent()) {
                newKey = entry.get().getKey();  // Find the actual id to replace val, from the entry
                newValue = val;
            }
        }
        if (newKey != null && newValue != null) {
            incidentSummary = replaceIncidentValues(type, incidentSummary, newKey, newValue);
            logger.info("Replaced {} \"{}\" with \"{}\" in {} - {}", (isIdToNameMap ? "name" : "id"),
                    (isIdToNameMap ? newKey : newValue), (isIdToNameMap ? newValue : newKey), type, incidentSummary.toString());
        }
        return incidentSummary;
    }

    private IncidentSummary replaceIncidentValues(String type, IncidentSummary input, String id, String name) {
        String airportId = input.airportId();
        String airportName = input.airport();
        String speciesId = input.speciesId();
        String speciesName = input.speciesName();
        if (type.equals("airports")) {
            airportId = id;
            airportName = name;
        } else if (type.equals("species")) {
            speciesId = id;
            speciesName = name;
        }
        return new IncidentSummary(input.recordId(), input.incidentYear(),
                input.incidentMonth(), input.incidentDay(), input.aircraft(),
                input.aircraftMass(), input.engines(), airportId, airportName,
                input.state(), input.faaRegion(), input.warningIssued(), input.flightPhase(),
                speciesId, speciesName, input.speciesQuantityMin(), input.speciesQuantityMax(),
                input.fatalities(), input.injuries(), input.aircraftDamage());
    }
}
