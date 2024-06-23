package FowlFlightForensics.service;

import FowlFlightForensics.FowlFlightForensicsApplication;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.Consts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The {@code AppStopperService} triggers a {@code System.exit(0)}, if enough time has passed since the last message was
 * sent by the {@code Producer}.
 */
@Service
public class AppStopperService extends BaseComponent {
    @Value("${app.stopper.exit.fixed-rate}")
    private int millisecondsToTick;

    /**
     * Checks if application stop is necessary, and triggers a {@code System.exit(0)} if so.
     */
    @Scheduled(fixedRateString = "${app.stopper.exit.fixed-rate}")
    public void stopApplication() {
        if (isAppStopNecessary()) {
            logger.info("Stopping application ...");
            System.exit(0);
        }
    }

    private boolean isAppStopNecessary() {
        long currentTimeInMillis = System.currentTimeMillis();
        return FowlFlightForensicsApplication.lastMessageTimeInMillis != Consts.LAST_MESSAGE_TIME_MILLIS_DEFAULT_VALUE
                && currentTimeInMillis - FowlFlightForensicsApplication.lastMessageTimeInMillis > millisecondsToTick;
    }
}
