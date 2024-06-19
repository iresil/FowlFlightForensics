package FowlFlightForensics.service;

import FowlFlightForensics.FowlFlightForensicsApplication;
import FowlFlightForensics.util.BaseComponent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AppStopperService extends BaseComponent {
    @Scheduled(fixedRate = 30000)
    public void stopApplication() {
        long currentTimeInMillis = System.currentTimeMillis();
        if (FowlFlightForensicsApplication.lastMessageTimeInMillis != -1
                && currentTimeInMillis - FowlFlightForensicsApplication.lastMessageTimeInMillis > 30000) {
            logger.info("Stopping application ...");
            System.exit(0);
        }
    }
}
