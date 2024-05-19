package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.fileHandling.CsvReader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CsvReaderService extends BaseComponent {
    @Autowired
    private CsvReader csvReader;

    @PostConstruct
    public void init() {
        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
        logger.info("Successfully retrieved {} incidents.", allIncidents.size());
    }
}
