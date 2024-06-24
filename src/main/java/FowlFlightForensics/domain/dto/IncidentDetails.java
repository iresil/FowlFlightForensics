package FowlFlightForensics.domain.dto;

import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.string.UppercaseConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;

/**
 * An {@code IncidentDetails} object contains one row of information parsed by the input CSV file, it its raw format.
 * All the available fields have been assigned a specific type, based on what sort of values the CSV file contained.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class IncidentDetails extends BaseComponent {
    @CsvBindByName(column = "Record ID")
    private Integer recordId;

    @CsvBindByName(column = "Incident Year")
    private Integer year;

    @CsvBindByName(column = "Incident Month")
    private Integer month;

    @CsvBindByName(column = "Incident Day")
    private Integer day;

    @CsvBindByName(column = "Operator ID")
    private String operatorId;

    @CsvBindByName(column = "Operator")
    private String operator;

    @CsvBindByName(column = "Aircraft")
    private String aircraft;

    @CsvBindByName(column = "Aircraft Type")
    private String aircraftType;

    @CsvBindByName(column = "Aircraft Make")
    private String aircraftMake;

    @CsvBindByName(column = "Aircraft Model")
    private String aircraftModel;

    @CsvBindByName(column = "Aircraft Mass")
    private Float aircraftMass;

    @CsvBindByName(column = "Engine Make")
    private Integer engineMake;

    @CsvBindByName(column = "Engine Model")
    private String engineModel;

    @CsvBindByName(column = "Engines")
    private Integer engines;

    @CsvBindByName(column = "Engine Type")
    private String engineType;

    @CsvBindByName(column = "Engine1 Position")
    private String engine1_position;

    @CsvBindByName(column = "Engine2 Position")
    private Integer engine2_position;

    @CsvBindByName(column = "Engine3 Position")
    private String engine3_position;

    @CsvBindByName(column = "Engine4 Position")
    private Integer engine4_position;

    @CsvBindByName(column = "Airport ID")
    private String airportId;

    @CsvBindByName(column = "Airport")
    private String airportName;

    @CsvBindByName(column = "State")
    private String state;

    @CsvBindByName(column = "FAA Region")
    private String faaRegion;

    @CsvBindByName(column = "Warning Issued")
    private Boolean warningIssued;

    @CsvBindByName(column = "Flight Phase")
    private String flightPhase;

    @CsvBindByName(column = "Visibility")
    private String visibility;

    @CsvBindByName(column = "Precipitation")
    private String precipitation;

    @CsvBindByName(column = "Height")
    private Float height;

    @CsvBindByName(column = "Speed")
    private Float speed;

    @CsvBindByName(column = "Distance")
    private Float distance;

    @CsvCustomBindByName(column = "Species ID", converter = UppercaseConverter.class)
    private String speciesId;

    @CsvBindByName(column = "Species Name")
    private String speciesName;

    @CsvBindByName(column = "Species Quantity")
    private String speciesQuantity;

    @CsvBindByName(column = "Flight Impact")
    private String flightImpact;

    @CsvBindByName(column = "Fatalities")
    private Integer fatalities;

    @CsvBindByName(column = "Injuries")
    private Integer injuries;

    @CsvBindByName(column = "Aircraft Damage")
    private Boolean aircraftDamage;

    @CsvBindByName(column = "Radome Strike")
    private Boolean radomeStrike;

    @CsvBindByName(column = "Radome Damage")
    private Boolean radomeDamage;

    @CsvBindByName(column = "Windshield Strike")
    private Boolean windshieldStrike;

    @CsvBindByName(column = "Windshield Damage")
    private Boolean windshieldDamage;

    @CsvBindByName(column = "Nose Strike")
    private Boolean noseStrike;

    @CsvBindByName(column = "Nose Damage")
    private Boolean noseDamage;

    @CsvBindByName(column = "Engine1 Strike")
    private Boolean engine1_strike;

    @CsvBindByName(column = "Engine1 Damage")
    private Boolean engine1_damage;

    @CsvBindByName(column = "Engine2 Strike")
    private Boolean engine2_strike;

    @CsvBindByName(column = "Engine2 Damage")
    private Boolean engine2_damage;

    @CsvBindByName(column = "Engine3 Strike")
    private Boolean engine3_strike;

    @CsvBindByName(column = "Engine3 Damage")
    private Boolean engine3_damage;

    @CsvBindByName(column = "Engine4 Strike")
    private Boolean engine4_strike;

    @CsvBindByName(column = "Engine4 Damage")
    private Boolean engine4_damage;

    @CsvBindByName(column = "Engine Ingested")
    private Boolean engineIngested;

    @CsvBindByName(column = "Propeller Strike")
    private Boolean propellerStrike;

    @CsvBindByName(column = "Propeller Damage")
    private Boolean propellerDamage;

    @CsvBindByName(column = "Wing or Rotor Strike")
    private Boolean wing_rotor_strike;

    @CsvBindByName(column = "Wing or Rotor Damage")
    private Boolean wing_rotor_damage;

    @CsvBindByName(column = "Fuselage Strike")
    private Boolean fuselageStrike;

    @CsvBindByName(column = "Fuselage Damage")
    private Boolean fuselageDamage;

    @CsvBindByName(column = "Landing Gear Strike")
    private Boolean landingGearStrike;

    @CsvBindByName(column = "Landing Gear Damage")
    private Boolean landingGearDamage;

    @CsvBindByName(column = "Tail Strike")
    private Boolean tailStrike;

    @CsvBindByName(column = "Tail Damage")
    private Boolean tailDamage;

    @CsvBindByName(column = "Lights Strike")
    private Boolean lightsStrike;

    @CsvBindByName(column = "Lights Damage")
    private Boolean lightsDamage;

    @CsvBindByName(column = "Other Strike")
    private Boolean otherStrike;

    @CsvBindByName(column = "Other Damage")
    private Boolean otherDamage;

    /**
     * Utilizes reflection to retrieve a field's value, provided that the field's name matches the {@code fieldName}
     * parameter.
     * @param fieldName The name of the field whose value we need to retrieve.
     * @return A generic {@code Object} containing the field's value, regardless of its original type.
     */
    public Object getFieldValueByName(String fieldName) {
        Object result = new Object();
        try {
            Field f = getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            result = f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Error attempting to retrieve field value by name: {}", fieldName, e);
        }
        return result;
    }
}
