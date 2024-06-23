package FowlFlightForensics.util;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code BaseComponent} is an abstract class that provides all its extensions with the {@code Logger} to use, in
 * order to avoid redefining it in each class. It also utilizes {@code @PostConstruct} and {@code @PreDestroy}, to notify
 * about the loading and unloading of components, via {@code TRACE} logs.
 */
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
