package FowlFlightForensics.util;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseComponent {
    protected final Logger logger = LogManager.getLogger(getClass());

    @PostConstruct
    public void init() {
        logger.trace("Loaded Component {}", getClass());
    }

    @PreDestroy
    public void destroy() {
        logger.trace("Unloading Component {}", getClass());
    }
}
