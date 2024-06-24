package FowlFlightForensics;

import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.util.file.CsvReader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CsvParserTests {
    @Test
    public void parseCsv_ReturnsExactNumberOfEntries() {
        CsvReader csvReader = new CsvReader();

        List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();

        assertThat(allIncidents).isNotNull();
        assertThat(allIncidents.size()).isEqualTo(174104);
    }
}
