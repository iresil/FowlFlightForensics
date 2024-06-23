package FowlFlightForensics.service.file;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.service.DataWiperService;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.file.CsvReader;
import FowlFlightForensics.util.incident.IncidentPreprocessor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The {@code CsvParserService} manages reading input data from the original CSV file and preparing the necessary structures
 * in order to process that data in {@code Kafka}.
 */
@Service
public class CsvParserService extends BaseComponent {
    @Autowired
    private CsvReader csvReader;

    @Autowired
    private DataWiperService wiperService;

    @Autowired
    private IncidentPreprocessor incidentPreprocessor;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    /**
     * Performs initialization actions (which mainly concern transforming the actual CSV to something usable) for the entire
     * application. All the actions performed are:
     * <ul>
     *   <li>cleanup of leftovers from previous executions
     *   <li>unzipping and parsing of the input CSV file
     *   <li>validation of the parsed entries and initialization of validation-related objects
     *   <li>objecting transformations that are necessary before sending data to {@code Kafka}
     * </ul>
     */
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
