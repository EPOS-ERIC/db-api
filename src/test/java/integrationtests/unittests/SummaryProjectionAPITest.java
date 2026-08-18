package integrationtests.unittests;

import abstractapis.AbstractAPI;
import commonapis.*;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.OutputMappingAPI;
import model.ElementType;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryProjectionAPITest extends TestcontainersLifecycle {

    @Test
    void summaryReadersUseScalarProjections() {
        Address address = new Address();
        address.setUid("summary-projection-address"); address.setStreet("Street"); address.setCountry("Country");
        address.setPostalCode("10000"); address.setCountryCode("CC"); address.setLocality("Locality");
        address.setStatus(StatusType.PUBLISHED);
        LinkedEntity addressLink = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).create(address, null, null, null);
        Address addressSummary = ((AddressAPI) AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()))
                .retrieveBunchSummary(List.of(addressLink.getInstanceId())).getFirst();
        assertEquals("Street", addressSummary.getStreet()); assertEquals("Country", addressSummary.getCountry());
        assertEquals("10000", addressSummary.getPostalCode()); assertSummaryVersion(addressSummary);

        Element element = new Element();
        element.setUid("summary-projection-element"); element.setType(ElementType.EMAIL); element.setValue("value");
        element.setStatus(StatusType.PUBLISHED);
        LinkedEntity elementLink = AbstractAPI.retrieveAPI(EntityNames.ELEMENT.name()).create(element, null, null, null);
        Element elementSummary = ((ElementAPI) AbstractAPI.retrieveAPI(EntityNames.ELEMENT.name()))
                .retrieveBunchSummary(List.of(elementLink.getInstanceId())).getFirst();
        assertEquals(ElementType.EMAIL, elementSummary.getType()); assertEquals("value", elementSummary.getValue());
        assertSummaryVersion(elementSummary);

        Identifier identifier = new Identifier();
        identifier.setUid("summary-projection-identifier"); identifier.setType("DOI"); identifier.setIdentifier("id-value");
        identifier.setStatus(StatusType.PUBLISHED);
        LinkedEntity identifierLink = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name()).create(identifier, null, null, null);
        Identifier identifierSummary = ((IdentifierAPI) AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name()))
                .retrieveBunchSummary(List.of(identifierLink.getInstanceId())).getFirst();
        assertEquals("DOI", identifierSummary.getType()); assertEquals("id-value", identifierSummary.getIdentifier());
        assertSummaryVersion(identifierSummary);

        Documentation documentation = new Documentation();
        documentation.setUid("summary-projection-documentation"); documentation.setTitle("Title");
        documentation.setDescription("Description"); documentation.setUri("https://example.test/documentation");
        documentation.setStatus(StatusType.PUBLISHED);
        LinkedEntity documentationLink = AbstractAPI.retrieveAPI(EntityNames.DOCUMENTATION.name()).create(documentation, null, null, null);
        Documentation documentationSummary = ((DocumentationAPI) AbstractAPI.retrieveAPI(EntityNames.DOCUMENTATION.name()))
                .retrieveBunchSummary(List.of(documentationLink.getInstanceId())).getFirst();
        assertEquals("Title", documentationSummary.getTitle()); assertEquals("Description", documentationSummary.getDescription());
        assertEquals("https://example.test/documentation", documentationSummary.getUri()); assertSummaryVersion(documentationSummary);

        Location location = new Location();
        location.setUid("summary-projection-location"); location.setLocation("POINT (1 2)"); location.setStatus(StatusType.PUBLISHED);
        LinkedEntity locationLink = AbstractAPI.retrieveAPI(EntityNames.LOCATION.name()).create(location, null, null, null);
        Location locationSummary = ((SpatialAPI) AbstractAPI.retrieveAPI(EntityNames.LOCATION.name()))
                .retrieveBunchSummary(List.of(locationLink.getInstanceId())).getFirst();
        assertEquals("POINT (1 2)", locationSummary.getLocation()); assertSummaryVersion(locationSummary);

        PeriodOfTime period = new PeriodOfTime();
        period.setUid("summary-projection-period"); period.setStartDate("2024-01-01"); period.setEndDate("2024-12-31");
        period.setStatus(StatusType.PUBLISHED);
        LinkedEntity periodLink = AbstractAPI.retrieveAPI(EntityNames.PERIODOFTIME.name()).create(period, null, null, null);
        PeriodOfTime periodSummary = ((TemporalAPI) AbstractAPI.retrieveAPI(EntityNames.PERIODOFTIME.name()))
                .retrieveBunchSummary(List.of(periodLink.getInstanceId())).getFirst();
        assertEquals(java.time.LocalDateTime.parse("2024-01-01T00:00"), periodSummary.getStartDate());
        assertEquals(java.time.LocalDateTime.parse("2024-12-31T00:00"), periodSummary.getEndDate());
        assertSummaryVersion(periodSummary);

        SoftwareApplicationParameter parameter = new SoftwareApplicationParameter();
        parameter.setUid("summary-projection-parameter"); parameter.setEncodingformat("application/json");
        parameter.setConformsto("schema"); parameter.setAction("OBJECT"); parameter.setStatus(StatusType.PUBLISHED);
        LinkedEntity parameterLink = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()).create(parameter, null, null, null);
        SoftwareApplicationParameter parameterSummary = ((ParameterAPI) AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()))
                .retrieveBunchSummary(List.of(parameterLink.getInstanceId())).getFirst();
        assertEquals("application/json", parameterSummary.getEncodingformat()); assertEquals("schema", parameterSummary.getConformsto());
        assertEquals("OBJECT", parameterSummary.getAction()); assertSummaryVersion(parameterSummary);

        QuantitativeValue quantitativeValue = new QuantitativeValue();
        quantitativeValue.setUid("summary-projection-quantitative-value"); quantitativeValue.setUnit("m");
        quantitativeValue.setValue("42"); quantitativeValue.setStatus(StatusType.PUBLISHED);
        LinkedEntity quantitativeValueLink = AbstractAPI.retrieveAPI(EntityNames.QUANTITATIVEVALUE.name()).create(quantitativeValue, null, null, null);
        QuantitativeValue quantitativeValueSummary = ((QuantitativeValueAPI) AbstractAPI.retrieveAPI(EntityNames.QUANTITATIVEVALUE.name()))
                .retrieveBunchSummary(List.of(quantitativeValueLink.getInstanceId())).getFirst();
        assertEquals("m", quantitativeValueSummary.getUnit()); assertEquals("42", quantitativeValueSummary.getValue());
        assertSummaryVersion(quantitativeValueSummary);

        OutputMapping outputMapping = new OutputMapping();
        outputMapping.setUid("summary-projection-output-mapping"); outputMapping.setOutputLabel("label");
        outputMapping.setOutputValuePattern("pattern"); outputMapping.setOutputRequired(null); outputMapping.setOutputRange("range");
        outputMapping.setOutputProperty("property"); outputMapping.setOutputVariable("variable"); outputMapping.setStatus(StatusType.PUBLISHED);
        LinkedEntity outputMappingLink = AbstractAPI.retrieveAPI(EntityNames.OUTPUTMAPPING.name()).create(outputMapping, null, null, null);
        OutputMapping outputMappingSummary = ((OutputMappingAPI) AbstractAPI.retrieveAPI(EntityNames.OUTPUTMAPPING.name()))
                .retrieveBunchSummary(List.of(outputMappingLink.getInstanceId())).getFirst();
        assertEquals("label", outputMappingSummary.getOutputLabel()); assertEquals("pattern", outputMappingSummary.getOutputValuePattern());
        assertNull(outputMappingSummary.getOutputRequired()); assertEquals("range", outputMappingSummary.getOutputRange());
        assertEquals("property", outputMappingSummary.getOutputProperty()); assertEquals("variable", outputMappingSummary.getOutputVariable());
        assertSummaryVersion(outputMappingSummary);
    }

    private void assertSummaryVersion(VersioningAndApproval summary) {
        assertEquals(StatusType.PUBLISHED, summary.getStatus());
        assertTrue(summary.getGroups().isEmpty());
    }
}
