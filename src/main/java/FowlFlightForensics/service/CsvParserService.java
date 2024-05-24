package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.file.CsvReader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CsvParserService extends BaseComponent {
    @Autowired
    private CsvReader csvReader;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    @PostConstruct
    public void init() {
        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
        logger.info("Successfully retrieved {} incidents.", allIncidents.size());
        incidentContainer.validateAndTransformIncidents(allIncidents);
    }
}
