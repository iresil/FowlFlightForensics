package FowlFlightForensics;

import FowlFlightForensics.domain.dto.IncidentDetails;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.file.CsvReader;
import FowlFlightForensics.util.incident.IncidentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

// TODO: Need to mock secondary objects
@SpringBootTest
class IncidentValidatorTests {
	@Test
	public void validateAndTransformIncidentsNullInput_ThrowsException() throws NoSuchMethodException {
		IncidentValidator incidentValidator = new IncidentValidator();

		Method method = incidentValidator.getClass().getDeclaredMethod("validateAndTransformIncidents", List.class);
		method.setAccessible(true);

		Throwable thrown = catchThrowable(() -> method.invoke(incidentValidator, new Object[]{ null }));

		assertThat(thrown).isInstanceOf(InvocationTargetException.class);
	}

	@Test
	public void validateAndTransformIncidentsEmptyList_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		List<IncidentDetails> allIncidents = new ArrayList<>();

		incidentValidator.validateAndTransformIncidents(allIncidents);

		assertThat(incidentValidator.incidentSummaryList).isEmpty();
		assertThat(incidentValidator.invalidIncidentsTrimmedMap).isEmpty();
		assertThat(incidentValidator.getSummaryValidationRules()).isEmpty(); // Because invalidIncidentsTrimmedMap is empty
		assertThat(incidentValidator.airports).isEmpty();
		assertThat(incidentValidator.species).isEmpty();
		assertThat(incidentValidator.unknownSpeciesIds).isEmpty();
		assertThat(incidentValidator.unknownSpeciesNames).isEmpty();
		assertThat(incidentValidator.multiCodeCorrelations).isEmpty();
	}

	@Test
	public void validateAndTransformIncidentsSingleValidInput_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		IncidentDetails incidentDetails = new IncidentDetails(2259, 1990, 1, 11, "MIL",
				"MILITARY", "F-16", "A", "561", "", null, null,
				"", null, "", "", null, "", null,
				"KFSM", "FORT SMITH REGIONAL ARPT", "AR", "ASW", null, "CLIMB",
				"DAY", "", 1400f, 200f, null, "YH004", "HORNED LARK",
				"1", "", null, null, false, false, false,
				false, false, false, false, true, false,
				false, false, false, false, false, false,
				true, false, false, false, false, false,
				false, false, false, false, false, false,
				false, false, false);
		List<IncidentDetails> allIncidents = List.of(incidentDetails);

		incidentValidator.validateAndTransformIncidents(allIncidents);

		assertThat(incidentValidator.incidentSummaryList).isNotEmpty();
		assertThat(incidentValidator.invalidIncidentsTrimmedMap).isEmpty();
		assertThat(incidentValidator.getSummaryValidationRules()).isEmpty(); // Because invalidIncidentsTrimmedMap is empty
		assertThat(incidentValidator.airports).isNotEmpty();
		assertThat(incidentValidator.species).isNotEmpty();
		assertThat(incidentValidator.unknownSpeciesIds).isEmpty();
		assertThat(incidentValidator.unknownSpeciesNames).isEmpty();
		assertThat(incidentValidator.multiCodeCorrelations).isEmpty();
	}

	@Test
	public void validateAndTransformIncidentsSingleInvalidInput_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		IncidentDetails incidentDetails = new IncidentDetails(2259, 1990, 1, 11, "MIL",
				"MILITARY", "F-16", "A", "561", "", null, null,
				"", null, "", "", null, "", null,
				"KFSM", "FORT SMITH REGIONAL ARPT", "AR", "ASW", null, "CLIMB",
				"DAY", "", 1400f, 200f, null, "100000000000", "UNKNOWN",
				"", "", null, null, false, false, false,
				false, false, false, false, true, false,
				false, false, false, false, false, false,
				true, false, false, false, false, false,
				false, false, false, false, false, false,
				false, false, false);
		List<IncidentDetails> allIncidents = List.of(incidentDetails);

		incidentValidator.validateAndTransformIncidents(allIncidents);

		assertThat(incidentValidator.incidentSummaryList).isNotEmpty();
		assertThat(incidentValidator.invalidIncidentsTrimmedMap).isEmpty();
		assertThat(incidentValidator.getSummaryValidationRules()).isEmpty(); // Because invalidIncidentsTrimmedMap is empty
		assertThat(incidentValidator.airports).isNotEmpty();
		assertThat(incidentValidator.species).isNotEmpty();
		assertThat(incidentValidator.unknownSpeciesIds).isEmpty();
		assertThat(incidentValidator.unknownSpeciesNames).isNotEmpty();
		assertThat(incidentValidator.multiCodeCorrelations).isEmpty();
	}

	@Test
	public void validateAndTransformIncidentsParsedInput_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		CsvReader csvReader = new CsvReader();
		List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();

		incidentValidator.validateAndTransformIncidents(allIncidents);

		assertThat(incidentValidator.incidentSummaryList).isNotEmpty();
		assertThat(incidentValidator.incidentSummaryList.size()).isEqualTo(allIncidents.size());
		assertThat(incidentValidator.invalidIncidentsTrimmedMap).isNotEmpty();
		assertThat(incidentValidator.getSummaryValidationRules()).isNotEmpty();
		assertThat(incidentValidator.getSummaryValidationRules().size()).isEqualTo(incidentValidator.invalidIncidentsTrimmedMap.size() + 1);
		assertThat(incidentValidator.airports).isNotEmpty();
		assertThat(incidentValidator.airports.size()).isEqualTo(2228);
		assertThat(incidentValidator.species).isNotEmpty();
		assertThat(incidentValidator.species.size()).isEqualTo(717);
		assertThat(incidentValidator.unknownSpeciesIds).isNotEmpty();
		assertThat(incidentValidator.unknownSpeciesIds.size()).isEqualTo(7);
		assertThat(incidentValidator.unknownSpeciesNames).isNotEmpty();
		assertThat(incidentValidator.unknownSpeciesNames.size()).isEqualTo(6);
		assertThat(incidentValidator.multiCodeCorrelations).isNotEmpty();
		assertThat(incidentValidator.multiCodeCorrelations.size()).isEqualTo(2);
	}

	@Test
	public void validateIncidentSummaryNullInput_ThrowsException() throws NoSuchMethodException {
		IncidentValidator incidentValidator = new IncidentValidator();

		Method method = incidentValidator.getClass().getDeclaredMethod("validateIncidentSummary", IncidentSummary.class);
		method.setAccessible(true);

		Throwable thrown = catchThrowable(() -> method.invoke(incidentValidator, new Object[]{ null }));

		assertThat(thrown).isInstanceOf(InvocationTargetException.class);
	}

	@Test
	public void validateIncidentSummaryValidInput_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		CsvReader csvReader = new CsvReader();
		List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
		incidentValidator.validateAndTransformIncidents(allIncidents);
		IncidentSummary incidentSummary = new IncidentSummary(111, 2015, 6, 24, "T-38A",
				4f, 2, "KIWA", "PHOENIX-MESA GATEWAY", "AZ", "AWP",
				null, "CLIMB", "K5105", "MERLIN", 2, 10,
				null, null, false);

		Set<InvalidIncidentTopic> invalidIncidentTopics = incidentValidator.validateIncidentSummary(incidentSummary);

		assertThat(invalidIncidentTopics).isEmpty();
	}

	@Test
	public void validateIncidentSummaryInvalidInput_ReturnsExpected() {
		IncidentValidator incidentValidator = new IncidentValidator();
		CsvReader csvReader = new CsvReader();
		List<IncidentDetails> allIncidents = csvReader.zippedCsvToListOfObjects();
		incidentValidator.validateAndTransformIncidents(allIncidents);
		IncidentSummary incidentSummary = new IncidentSummary(111, 2015, 6, 24, "T-38A",
				null, 2, "KIWA", "PHOENIX-MESA GATEWAY", "AZ", "AWP",
				null, "CLIMB", "K5105", "UNKNOWN", null, null,
				null, null, false);

		Set<InvalidIncidentTopic> invalidIncidentTopics = incidentValidator.validateIncidentSummary(incidentSummary);

		assertThat(invalidIncidentTopics).isNotEmpty();
		assertThat(invalidIncidentTopics.size()).isEqualTo(3); // Invalid aircraft mass, species name & quantity
	}
}
