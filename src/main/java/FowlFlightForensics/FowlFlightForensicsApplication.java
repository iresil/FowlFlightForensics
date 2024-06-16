package FowlFlightForensics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FowlFlightForensicsApplication {
	public static long lastMessageTimeInMillis = -1;

	public static void main(String[] args) {
		SpringApplication.run(FowlFlightForensicsApplication.class, args);
	}

}
