package FowlFlightForensics.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class IncidentSummary {
    public Integer recordId;
    public Integer year;
    public Integer month;
    public Integer day;
    public String aircraft;
    public Float aircraftMass;
    public Integer engines;
    public String airportId;
    public String airportName;
    public String state;
    public String faaRegion;
    public Boolean warningIssued;
    public String flightPhase;
    public String speciesId;
    public String speciesName;
    public Integer speciesQuantityMin;
    public Integer speciesQuantityMax;
    public Integer fatalities;
    public Integer injuries;
    public Boolean aircraftDamage;

    public IncidentKey getKey() {
        return new IncidentKey(year, month, speciesId, speciesName, aircraftDamage);
    }
}
