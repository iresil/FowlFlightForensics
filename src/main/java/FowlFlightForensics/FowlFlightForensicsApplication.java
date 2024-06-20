package FowlFlightForensics;

import FowlFlightForensics.util.Consts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FowlFlightForensicsApplication {
	public static long lastMessageTimeInMillis = Consts.LAST_MESSAGE_TIME_MILLIS_DEFAULT_VALUE;

	public static void main(String[] args) {
		SpringApplication.run(FowlFlightForensicsApplication.class, args);
	}

}
