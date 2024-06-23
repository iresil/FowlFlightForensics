package FowlFlightForensics.enums;

import lombok.Getter;

/**
 * The {@code MappingType} {@code enum} is used as a key in the {@code Map} which contains airport and species mappings,
 * for which either one single id corresponds to multiple names, or vice-versa.
 */
@Getter
public enum MappingType {
    AIRPORTS("airports"),
    SPECIES("species");

    private final String typeKey;

    MappingType(String typeKey) {
        this.typeKey = typeKey;
    }
}
