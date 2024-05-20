package FowlFlightForensics.util.incidentHandling;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.domain.IncidentSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IncidentValidator {
    public List<IncidentSummary> validateAndTransformIncidents(List<IncidentDetails> incidentDetails) {
        List<IncidentSummary> incidentSummaryList = new ArrayList<>();

        List<IncidentSummary> invalidYears = new ArrayList<>();
        List<IncidentSummary> invalidMonths = new ArrayList<>();
        List<IncidentSummary> invalidDays = new ArrayList<>();
        List<IncidentSummary> invalidAircraft = new ArrayList<>();
        List<IncidentSummary> invalidAircraftMass = new ArrayList<>();
        List<IncidentSummary> invalidEngines = new ArrayList<>();
        List<IncidentSummary> invalidAirportIds = new ArrayList<>();
        List<IncidentSummary> invalidAirportNames = new ArrayList<>();
        List<IncidentSummary> invalidState = new ArrayList<>();
        List<IncidentSummary> invalidFaaRegion = new ArrayList<>();
        List<IncidentSummary> invalidWarningIssued = new ArrayList<>();
        List<IncidentSummary> invalidFlightPhase = new ArrayList<>();
        List<IncidentSummary> invalidSpeciesId = new ArrayList<>();
        List<IncidentSummary> invalidSpeciesName = new ArrayList<>();
        List<IncidentSummary> invalidSpeciesQuantity = new ArrayList<>();
        List<IncidentSummary> invalidFatalities = new ArrayList<>();
        List<IncidentSummary> invalidInjuries = new ArrayList<>();

        for (IncidentDetails incident : incidentDetails) {
            String qty = incident.getSpeciesQuantity();
            Integer qtyMin = null;
            Integer qtyMax = null;
            if (!qty.isEmpty()) {
                qtyMin = (qty.contains("Over ")
                        ? Integer.parseInt(qty.replace("Over ", ""))
                        : (qty.contains("-") ? Integer.parseInt(qty.substring(0, qty.indexOf("-"))) : Integer.parseInt(qty)));
                qtyMax = (incident.getSpeciesQuantity().contains("Over ")
                        ? 10000
                        : (qty.contains("-") ? Integer.parseInt(qty.substring(qty.indexOf("-"))) : Integer.parseInt(qty)));
            }

            IncidentSummary summary = new IncidentSummary(incident.getRecordId(), incident.getIncidentYear(),
                    incident.getIncidentMonth(), incident.getIncidentDay(), incident.getAircraft(),
                    incident.getAircraftMass(), incident.getEngines(), incident.getAirportId(), incident.getAirport(),
                    incident.getState(), incident.getFaaRegion(), incident.getWarningIssued(), incident.getFlightPhase(),
                    incident.getSpeciesId(), incident.getSpeciesName(), qtyMin, qtyMax,
                    incident.getFatalities(), incident.getInjuries(), incident.getAircraftDamage());
            incidentSummaryList.add(summary);

            if (incident.getIncidentYear() < 1990 || incident.getIncidentYear() > 2015) {
                invalidYears.add(summary);
            }
            if (incident.getIncidentMonth() < 1 || incident.getIncidentMonth() > 12) {
                invalidMonths.add(summary);
            }
            if (incident.getIncidentDay() < 1 || incident.getIncidentDay() > 31) {
                invalidDays.add(summary);
            }
            if (incident.getAircraft().equals("UNKNOWN")) {
                invalidAircraft.add(summary);
            }
            if (incident.getAircraftMass() == null) {
                invalidAircraftMass.add(summary);
            }
            if (incident.getEngines() == null) {
                invalidEngines.add(summary);
            }
            if (incident.getAirportId().matches("-?\\d+(\\.\\d+)?")) {
                invalidAirportIds.add(summary);
            }
            if (incident.getAirport().isEmpty()) {  // check for one airport name but two ids
                invalidAirportNames.add(summary);
            }
            if (incident.getState().isEmpty()) {
                invalidState.add(summary);
            }
            if (incident.getFaaRegion().isEmpty()) {
                invalidFaaRegion.add(summary);
            }
            if (incident.getWarningIssued() == null) {
                invalidWarningIssued.add(summary);
            }
            if (incident.getFlightPhase().isEmpty()) {
                invalidFlightPhase.add(summary);
            }
            if (incident.getSpeciesId().equals("100000000000")) {
                invalidSpeciesId.add(summary);
            }
            if (incident.getSpeciesName().isEmpty()) {  // check for one species name but two ids
                invalidSpeciesName.add(summary);
            }
            if (incident.getSpeciesQuantity().isEmpty()) {
                invalidSpeciesQuantity.add(summary);
            }
            if (incident.getFatalities() == null) {
                invalidFatalities.add(summary);
            }
            if (incident.getInjuries() == null) {
                invalidInjuries.add(summary);
            }
        }

        return incidentSummaryList;
    }
}
