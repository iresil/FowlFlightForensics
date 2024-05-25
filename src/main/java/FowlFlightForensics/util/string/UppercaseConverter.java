package FowlFlightForensics.util.string;

import com.opencsv.bean.AbstractBeanField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UppercaseConverter extends AbstractBeanField<String, String> {
    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    protected String convert(String value) {
        String modified = null;
        if (value != null) {
            modified = value.toUpperCase();
            if (!value.equals(modified)) {
                logger.info("Converted {} value from {} to {}.", field.getName(), value, modified);
            }
        }
        return modified;
    }
}
