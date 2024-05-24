package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.file.CsvReader;
import FowlFlightForensics.util.incident.IncidentValidator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CsvReaderService extends BaseComponent {
    @Autowired
    private CsvReader csvReader;

    @Autowired
    private IncidentValidator incidentValidator;

    @PostConstruct
    public void init() {
        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
        logger.info("Successfully retrieved {} incidents.", allIncidents.size());
        List<IncidentSummary> incidentSummaryList = incidentValidator.validateAndTransformIncidents(allIncidents);
    }
}
