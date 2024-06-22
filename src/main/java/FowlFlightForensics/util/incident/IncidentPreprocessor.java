package FowlFlightForensics.util.incident;

import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.enums.MappingType;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.CommandUtils;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.Map.*;

@Component
public class IncidentPreprocessor extends BaseComponent {
    public List<IncidentSummary> applyTransformations(List<IncidentSummary> incidentSummaryList, Map<String, String> airports,
                                                      Map<String, String> species, Map<MappingType, Map<String, Set<String>>> multiCodeCorrelations) {
        for (Entry<MappingType, Map<String, Set<String>>> set : multiCodeCorrelations.entrySet()) {
            MappingType typeKey = set.getKey();
            Map<String, Set<String>> multiCodeCorrelation = set.getValue();

            Map<MappingType, Boolean> isIdToNameMap = checkMultiCodeCorrelationTypes(multiCodeCorrelation, typeKey, airports,
                    species);

            for (int i = 0; i < incidentSummaryList.size(); i++) {
                IncidentSummary obj = incidentSummaryList.get(i);
                Boolean isIdToName = isIdToNameMap.get(typeKey);
                Map<String, String> mapping = new HashMap<>();
                if (typeKey.equals(MappingType.AIRPORTS)) {
                    mapping = airports;
                } else if (typeKey.equals(MappingType.SPECIES)) {
                    mapping = species;
                }
                replaceIncidentInList(typeKey, isIdToName, multiCodeCorrelation, mapping, incidentSummaryList, i, obj,
                        List::set);
            }
        }
        return incidentSummaryList;
    }

    private Map<MappingType, Boolean> checkMultiCodeCorrelationTypes(Map<String, Set<String>> multiCodeCorrelation,
                                                                     MappingType type, Map<String, String> airports,
                                                                     Map<String, String> species) {
        Map<MappingType, Boolean> isIdToNameMap = new HashMap<>();
        for (Entry<String, Set<String>> innerSet : multiCodeCorrelation.entrySet()) {
            String key = innerSet.getKey();
            Set<String> value = innerSet.getValue();

            Boolean isIdToName = checkIfMapIsIdToName(type, airports, species, key, value);

            isIdToNameMap.put(type, isIdToName);
            logger.info("Obj: {} -> isIdToName: {}", innerSet, isIdToName);
        }
        return isIdToNameMap;
    }

    private Boolean checkIfMapIsIdToName(MappingType type, Map<String, String> airports, Map<String, String> species,
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

    private Boolean checkIfValueIsId(MappingType type, Map<String, String> airports, Map<String, String> species,
                                     String key, String val) {
        Boolean itemIsId = null;
        boolean keyIsId = false, keyIsValue = false, valueIsId = false, valueIsValue = false;
        if (type.equals(MappingType.AIRPORTS)) {
            keyIsId = airports.containsKey(key);
            keyIsValue = airports.containsValue(key);

            valueIsId = airports.containsKey(val);
            valueIsValue = airports.containsValue(val);
        } else if (type.equals(MappingType.SPECIES)) {
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

    @SuppressWarnings("unchecked")
    private <T, K, V> void replaceIncidentInList(MappingType type, Boolean isIdToNameMap, Map<String, Set<String>> multiCodeCorrelation,
                                                 Map<String, String> mapping, T incidentSummaryList, K i, V incidentSummary,
                                                 CommandUtils.SettingFunction<T, K, V> func) {
        for (String k : multiCodeCorrelation.keySet()) {
            String largestCode = "";
            boolean alreadyReplaced = false;
            for (String v : multiCodeCorrelation.get(k).stream().toList()) {
                if (mapping.containsKey(v) && mapping.containsValue(v)) {
                    incidentSummary = (V)switchIdsAndNames(type, isIdToNameMap,
                            (IncidentSummary)incidentSummary, mapping, v);
                    func.set(incidentSummaryList, i, incidentSummary);
                    alreadyReplaced = true;
                } else {
                    if (v.length() > largestCode.length()) {
                        largestCode = v;
                    }
                }
            }

            if (!alreadyReplaced) {  // Don't replace values that have already been replaced
                incidentSummary = (V)syncIdsOrNames(type, isIdToNameMap,
                        (IncidentSummary)incidentSummary, k, largestCode);
                func.set(incidentSummaryList, i, incidentSummary);
            }
        }
    }

    private IncidentSummary switchIdsAndNames(MappingType type, Boolean isIdToNameMap, IncidentSummary incidentSummary,
                                              Map<String, String> mapping, String val) {
        String newKey = null;
        String newValue = null;
        if (isIdToNameMap
                && ((type.equals(MappingType.AIRPORTS) && incidentSummary.getAirportName().equals(val))
                    || type.equals(MappingType.SPECIES) && incidentSummary.getSpeciesName().equals(val))) {  // val is id, treated as a name
            String entry = mapping.get(val);  // Find the actual name to replace val
            if (entry != null) {
                newKey = val;
                newValue = entry;
            }
        } else if (!isIdToNameMap
                && ((type.equals(MappingType.AIRPORTS) && incidentSummary.getAirportId().equals(val))
                    || (type.equals(MappingType.SPECIES) && incidentSummary.getSpeciesId().equals(val)))) {  // val is name, treated as an id
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

    private IncidentSummary syncIdsOrNames(MappingType type, Boolean isIdToNameMap, IncidentSummary incidentSummary,
                                           String key, String val) {
        String newKey = null;
        String newValue = null;
        String oldVal = null;
        if (isIdToNameMap && type.equals(MappingType.AIRPORTS) && incidentSummary.getAirportId().equals(key)) {  // val is airport name
            oldVal = incidentSummary.getAirportName();
            newValue = val;
        } else if (isIdToNameMap && type.equals(MappingType.SPECIES) && incidentSummary.getSpeciesId().equals(key)) {  // val is species name
            oldVal = incidentSummary.getSpeciesName();
            newValue = val;
        } else if (!isIdToNameMap && type.equals(MappingType.AIRPORTS) && incidentSummary.getAirportName().equals(key)) {  // val is airport id
            oldVal = incidentSummary.getAirportId();
            newKey = val;
        } else if (!isIdToNameMap && type.equals(MappingType.SPECIES) && incidentSummary.getSpeciesName().equals(key)) {  // val is species id
            oldVal = incidentSummary.getSpeciesId();
            newKey = val;
        }
        if ((newKey != null || newValue != null) && !oldVal.equals(val)) {
            incidentSummary = replaceIncidentValues(type, incidentSummary, newKey, newValue);
            logger.info("Replaced {} \"{}\" with \"{}\" in {} - {}", (isIdToNameMap ? "name" : "id"),
                    oldVal, (isIdToNameMap ? newValue : newKey), type, incidentSummary.toString());
        }
        return incidentSummary;
    }

    private IncidentSummary replaceIncidentValues(MappingType type, IncidentSummary input, String id, String name) {
        String airportId = input.getAirportId();
        String airportName = input.getAirportName();
        String speciesId = input.getSpeciesId();
        String speciesName = input.getSpeciesName();
        if (type.equals(MappingType.AIRPORTS)) {
            airportId = (id != null ? id : airportId);
            airportName = (name != null ? name : airportName);
        } else if (type.equals(MappingType.SPECIES)) {
            speciesId = (id != null ? id : speciesId);
            speciesName = (name != null ? name : speciesName);
        }
        return new IncidentSummary(input.getRecordId(), input.getYear(),
                input.getMonth(), input.getDay(), input.getAircraft(),
                input.getAircraftMass(), input.getEngines(), airportId, airportName,
                input.getState(), input.getFaaRegion(), input.getWarningIssued(), input.getFlightPhase(),
                speciesId, speciesName, input.getSpeciesQuantityMin(), input.getSpeciesQuantityMax(),
                input.getFatalities(), input.getInjuries(), input.getAircraftDamage());
    }
}
