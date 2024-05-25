package FowlFlightForensics.util.string;

import com.opencsv.bean.AbstractBeanField;

public class UppercaseConverter extends AbstractBeanField<String, String> {
    @Override
    protected String convert(String value) {
        if (value != null) {
            return value.toUpperCase();
        }
        return null;
    }
}
