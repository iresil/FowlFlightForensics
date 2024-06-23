package FowlFlightForensics.enums;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

/**
 * The {@code InvalidIncidentTopic} {@code enum} is used for mapping the
 */
public enum InvalidIncidentTopic {
    SPECIES("app.kafka.topics.invalid-species"),
    QUANTITY("app.kafka.topics.invalid-quantity"),
    OTHER("app.kafka.topics.invalid-generic");

    private final String value;

    private final Logger logger = LogManager.getLogger(getClass());

    InvalidIncidentTopic(String value) {
        this.value = value;
    }

    /**
     * Retrieves the value of the property (from {@code application.properties}) which has a name equal to the value of
     * the enum's currently active constant.
     * @return The value of the property in question, which corresponds to the name of a {@code Kafka} topic.
     */
    public String getAnnotationValue() {
        Properties prop = new Properties();
        try {
            prop.load(getClass().getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            logger.error("Could not retrieve property value from enum", e);
        }
        return prop.getProperty(value);
    }

    /**
     * Generates a constant belonging to this enum, based on the name of a validation rule. {@code QUANTITY} and {@code SPECIES}
     * are considered special cases, whereas every other rule is simply categorized as {@code OTHER}.
     * @param description The name of the validation rule to use for generating the constant.
     * @return One of the constants belonging to this enum.
     */
    public static InvalidIncidentTopic fromString(Object description) {
        if (description.toString().toLowerCase().contains("quantity")) {
            return QUANTITY;
        } else if (description.toString().toLowerCase().contains("species")) {
            return SPECIES;
        } else {
            return OTHER;
        }
    }
}
