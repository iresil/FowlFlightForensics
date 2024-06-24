package FowlFlightForensics;

import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.util.file.CsvReader;
import FowlFlightForensics.util.incident.IncidentPreprocessor;
import FowlFlightForensics.util.incident.IncidentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Need to mock secondary objects
@SpringBootTest
public class IncidentPreprocessorTests {
    @Test
    public void applyTransformationsParsedInput_ReturnsExpected() {
        CsvReader csvReader = new CsvReader();
        IncidentValidator incidentValidator = new IncidentValidator();
        IncidentPreprocessor incidentPreprocessor = new IncidentPreprocessor();
        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
        incidentValidator.validateAndTransformIncidents(allIncidents);

        List<IncidentSummary> incidentSummaryList = incidentPreprocessor.applyTransformations(incidentValidator.getIncidentSummaryList(),
                incidentValidator.getAirports(), incidentValidator.getSpecies(), incidentValidator.getMultiCodeCorrelations());

        assertThat(incidentSummaryList).isNotEmpty();
        assertThat(incidentSummaryList.size()).isEqualTo(allIncidents.size());
    }
}
