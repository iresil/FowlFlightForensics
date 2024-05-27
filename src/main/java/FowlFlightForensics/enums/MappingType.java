package FowlFlightForensics.enums;

import lombok.Getter;

@Getter
public enum MappingType {
    AIRPORTS("airports"),
    SPECIES("species");

    private final String typeKey;

    MappingType(String typeKey) {
        this.typeKey = typeKey;
    }
}
