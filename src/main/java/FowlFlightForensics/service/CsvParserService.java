package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.file.CsvReader;
import FowlFlightForensics.util.incident.IncidentPreprocessor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CsvParserService extends BaseComponent {
    @Autowired
    private CsvReader csvReader;

    @Autowired
    private DataWiperService wiperService;

    @Autowired
    private IncidentPreprocessor incidentPreprocessor;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    @PostConstruct
    public void init() {
        wiperService.delete();
        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
        logger.info("Successfully retrieved {} incidents.", allIncidents.size());
        incidentContainer.validateAndTransformIncidents(allIncidents);
        List<IncidentSummary> incidentSummaryList = incidentPreprocessor.applyTransformations(incidentContainer.getIncidentSummaryList(),
                incidentContainer.getAirports(), incidentContainer.getSpecies(), incidentContainer.getMultiCodeCorrelations());
        incidentContainer.setIncidentSummaryList(incidentSummaryList);
    }
}
