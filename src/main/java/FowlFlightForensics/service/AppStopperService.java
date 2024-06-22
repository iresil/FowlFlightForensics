package FowlFlightForensics.service;

import FowlFlightForensics.FowlFlightForensicsApplication;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.Consts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AppStopperService extends BaseComponent {
    @Value("${app.stopper.exit.fixed-rate}")
    private int millisecondsToTick;

    @Scheduled(fixedRateString = "${app.stopper.exit.fixed-rate}")
    public void stopApplication() {
        if (isAppStopNecessary()) {
            logger.info("Stopping application ...");
            System.exit(0);
        }
    }

    public boolean isAppStopNecessary() {
        long currentTimeInMillis = System.currentTimeMillis();
        return FowlFlightForensicsApplication.lastMessageTimeInMillis != Consts.LAST_MESSAGE_TIME_MILLIS_DEFAULT_VALUE
                && currentTimeInMillis - FowlFlightForensicsApplication.lastMessageTimeInMillis > millisecondsToTick;
    }
}
