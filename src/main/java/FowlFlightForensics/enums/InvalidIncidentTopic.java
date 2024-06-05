package FowlFlightForensics.enums;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public enum InvalidIncidentTopic {
    SPECIES("app.kafka.topics.invalid-species"),
    QUANTITY("app.kafka.topics.invalid-quantity"),
    OTHER("app.kafka.topics.invalid-generic");

    private final String value;

    private final Logger logger = LogManager.getLogger(getClass());

    InvalidIncidentTopic(String value) {
        this.value = value;
    }

    public String getAnnotationValue() {
        Properties prop = new Properties();
        try {
            prop.load(getClass().getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            logger.error("Could not retrieve property value from enum.");
        }
        return prop.getProperty(value);
    }

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
