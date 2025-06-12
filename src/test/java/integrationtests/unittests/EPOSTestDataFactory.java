package integrationtests.unittests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.epos.eposdatamodel.*;

/**
 * Factory class to create test data objects based on the TTL file content
 * for JUnit testing purposes.
 */
public class EPOSTestDataFactory {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ===============================
    // CATEGORY SCHEMES AND CATEGORIES
    // ===============================

    public static CategoryScheme createDomainTestCategoryScheme() {
        CategoryScheme categoryScheme = new CategoryScheme();
        categoryScheme.setUid("cat1");
        categoryScheme.setTitle("DOMAIN TEST 1");
        categoryScheme.setDescription("TCS Domain");
        categoryScheme.setCode("DOMAIN_TEST");
        categoryScheme.setLogo("assets/img/logo/NFO_logo.png");
        categoryScheme.setHomepage("https://www.epos-eu.org/tcs/near-fault-observatories");
        categoryScheme.setColor("#5e3160");
        categoryScheme.setOrderitemnumber("1");
        return categoryScheme;
    }

    public static CategoryScheme createFacilityCategoryScheme() {
        CategoryScheme categoryScheme = new CategoryScheme();
        categoryScheme.setUid("fac1");
        categoryScheme.setTitle("FACILITY TEST");
        categoryScheme.setDescription("Facility Domain");
        categoryScheme.setCode("FACILITY_TEST");
        categoryScheme.setLogo("assets/img/logo/LABS_logo.png");
        categoryScheme.setColor("#27A468");
        categoryScheme.setOrderitemnumber("1");
        return categoryScheme;
    }

    public static Category createCategoryTest1() {
        Category category = new Category();
        category.setUid("cat2");
        category.setName("CATEGORY TEST 1");
        category.setDescription("TCS Subdomain");
        category.setInScheme(createLinkedEntity("cat1", "CATEGORYSCHEME"));
        return category;
    }

    public static Category createCategoryTest2() {
        Category category = new Category();
        category.setUid("cat3");
        category.setName("CATEGORY TEST 2");
        category.setDescription("TCS Subdomain");
        category.setInScheme(createLinkedEntity("cat1", "CATEGORYSCHEME"));
        category.addBroader(createLinkedEntity("cat2", "CATEGORY"));
        return category;
    }

    public static Category createCategoryTest3() {
        Category category = new Category();
        category.setUid("cat4");
        category.setName("CATEGORY TEST 3");
        category.setDescription("TCS Subdomain");
        category.setInScheme(createLinkedEntity("cat1", "CATEGORYSCHEME"));
        category.addBroader(createLinkedEntity("cat2", "CATEGORY"));
        return category;
    }

    public static Category createFacilityCategory() {
        Category category = new Category();
        category.setUid("facchild1");
        category.setName("FACILITY CATEGORY 1");
        category.setDescription("FACILITY CATEGORY 1");
        category.setInScheme(createLinkedEntity("fac1", "CATEGORYSCHEME"));
        return category;
    }

    // ===============================
    // CONTACT POINTS AND PERSONS
    // ===============================

    public static Person createTestPerson() {
        Person person = new Person();
        person.setUid("http://orcid.org/0000-0001-5805-9337");
        person.setFamilyName("Vinciarelli");
        person.setGivenName("Valerio");
        person.setCVURL("http://orcid.org/0000-0001-5805-9337");
        person.addEmail("valerio.vinciarelli@epos-eric.eu");
        person.addTelephone("+3900000000");
        person.addQualifications("Senior Developer");

        // Add ORCID identifier
        Identifier orcidIdentifier = createIdentifier("orcid", "http://orcid.org/0000-0001-5805-9337");
        person.addIdentifier(createLinkedEntity("identifier6", "IDENTIFIER"));

        // Add address
        Address address = createTestAddress();
        person.setAddress(createLinkedEntity("address1", "ADDRESS"));

        return person;
    }

    public static ContactPoint createScientificContactPoint() {
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setUid("scientificContact");
        contactPoint.addEmail("valerio.vinciarelli@epos-eric.eu");
        contactPoint.addLanguage("en");
        contactPoint.setRole("scientificContact");
        contactPoint.setPerson(createLinkedEntity("http://orcid.org/0000-0001-5805-9337", "PERSON"));
        return contactPoint;
    }

    public static Address createTestAddress() {
        Address address = new Address();
        address.setUid("address1");
        address.setStreet("via di Vigna Murata, 605");
        address.setLocality("Roma");
        address.setPostalCode("00143");
        address.setCountry("Italy");
        address.setCountryCode("IT");
        return address;
    }

    // ===============================
    // IDENTIFIERS
    // ===============================

    public static Identifier createIdentifier(String type, String value) {
        Identifier identifier = new Identifier();
        identifier.setUid("identifier1");
        identifier.setType(type);
        identifier.setIdentifier(value);
        return identifier;
    }

    // ===============================
    // DATA PRODUCTS AND DISTRIBUTIONS
    // ===============================

    public static DataProduct createDatasetTest1() {
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("Dataset/Test/1");
        dataProduct.addTitle("Dataset Title 1");
        dataProduct.addDescription("Dataset Description 1");
        dataProduct.setCreated(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setIssued(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setModified(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setVersionInfo("1.0.0");
        dataProduct.setType("http://purl.org/dc/dcmitype/Collection");
        dataProduct.setAccrualPeriodicity("http://purl.org/cld/freq/continuous");
        dataProduct.setKeywords("keyword1,keyword2,keyword3");
        dataProduct.setDocumentation("QualityAssuranceDocument");

        // Add identifier
        Identifier ddssId = createIdentifier("DDSS-ID", "TEST-1");
        dataProduct.addIdentifier(createLinkedEntity("identifier1", "IDENTIFIER"));

        // Add category
        dataProduct.addCategory(createLinkedEntity("cat2", "CATEGORY"));

        // Add contact point
        dataProduct.addContactPoint(createLinkedEntity("scientificContact", "CONTACTPOINT"));

        // Add publisher
        dataProduct.addPublisher(createLinkedEntity("eposid", "ORGANIZATION"));

        // Add distribution
        dataProduct.addDistribution(createLinkedEntity("Distribution/Test/1", "DISTRIBUTION"));

        // Add spatial and temporal extent
        dataProduct.addSpatialExtent(createLinkedEntity("spatial1", "LOCATION"));
        dataProduct.addTemporalExtent(createLinkedEntity("temporal1", "PERIODOFTIME"));

        return dataProduct;
    }

    public static DataProduct createDatasetTest2() {
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("Dataset/Test/2");
        dataProduct.addTitle("Dataset Title 2");
        dataProduct.addDescription("Dataset Description 2");
        dataProduct.setCreated(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setIssued(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setModified(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        dataProduct.setVersionInfo("1.0.0");
        dataProduct.setType("http://purl.org/dc/dcmitype/Collection");
        dataProduct.setAccrualPeriodicity("http://purl.org/cld/freq/irregular");
        dataProduct.setKeywords("keyword4,keyword5,keyword6");
        dataProduct.setDocumentation("QualityAssuranceDocument");

        // Add identifier
        Identifier ddssId = createIdentifier("DDSS-ID", "TEST-2");
        dataProduct.addIdentifier(createLinkedEntity("identifier2", "IDENTIFIER"));

        // Add category
        dataProduct.addCategory(createLinkedEntity("cat3", "CATEGORY"));

        // Add contact point
        dataProduct.addContactPoint(createLinkedEntity("scientificContact", "CONTACTPOINT"));

        // Add publisher
        dataProduct.addPublisher(createLinkedEntity("eposid", "ORGANIZATION"));

        // Add distribution
        dataProduct.addDistribution(createLinkedEntity("Distribution/Test/2", "DISTRIBUTION"));

        return dataProduct;
    }

    public static Distribution createDistributionTest1() {
        Distribution distribution = new Distribution();
        distribution.setUid("Distribution/Test/1");
        distribution.addTitle("Distribution Title 1");
        distribution.addDescription("Distribution Description 1");
        distribution.setIssued(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        distribution.setModified(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        distribution.setType("http://publications.europa.eu/resource/authority/distribution-type/DOWNLOADABLE_FILE");
        distribution.addAccessURL("https://doron-prize.ch/wp-content/uploads/2020/07/1200px-Test-Logo.svg_.png");
        distribution.addDownloadURL("https://doron-prize.ch/wp-content/uploads/2020/07/1200px-Test-Logo.svg_.png");
        distribution.setFormat("http://publications.europa.eu/resource/authority/file-type/PNG");
        distribution.setLicence("http://creativecommons.org/licenses/by/4.0/");

        // Add data product reference
        distribution.addDataproduct(createLinkedEntity("Dataset/Test/1", "DATAPRODUCT"));

        return distribution;
    }

    public static Distribution createDistributionTest2() {
        Distribution distribution = new Distribution();
        distribution.setUid("Distribution/Test/2");
        distribution.addTitle("Distribution Title 2");
        distribution.addDescription("Distribution Description 2");
        distribution.setIssued(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        distribution.setModified(LocalDateTime.parse("2025-01-01T00:00:00", ISO_FORMATTER));
        distribution.setType("http://publications.europa.eu/resource/authority/distribution-type/WEB_SERVICE");
        distribution.setFormat("http://publications.europa.eu/resource/authority/file-type/XML");
        distribution.setLicence("http://creativecommons.org/licenses/by/4.0/");

        // Add access service
        distribution.addAccessService(createLinkedEntity("WebService/Test/2", "WEBSERVICE"));

        // Add supported operation
        distribution.addSupportedOperation(createLinkedEntity("Operation/Test/2", "OPERATION"));

        // Add data product reference
        distribution.addDataproduct(createLinkedEntity("Dataset/Test/2", "DATAPRODUCT"));

        return distribution;
    }

    // ===============================
    // WEB SERVICES AND OPERATIONS
    // ===============================

    public static WebService createWebServiceTest2() {
        WebService webService = new WebService();
        webService.setUid("WebService/Test/2");
        webService.setName("WebService Title 2");
        webService.setDescription("WebService Description 2");
        webService.setDatePublished(LocalDateTime.parse("2016-01-01T00:00:00", ISO_FORMATTER));
        webService.setDateModified(LocalDateTime.parse("2016-01-01T00:00:00", ISO_FORMATTER));
        webService.setEntryPoint("http://www.orfeus-eu.org/fdsnws/dataselect/1/application.wadl");
        webService.addKeywords("keywordWebService1,keywordWebService2,keywordWebService3");
        webService.setLicense("http://creativecommons.org/licenses/by/4.0/");

        // Add identifier
        Identifier wsIdentifier = createIdentifier("ID", "WebService/Test/2");
        webService.addIdentifier(createLinkedEntity("identifier3", "IDENTIFIER"));

        // Add contact point
        webService.addContactPoint(createLinkedEntity("scientificContact", "CONTACTPOINT"));

        // Add provider
        webService.setProvider(createLinkedEntity("eposidchild", "ORGANIZATION"));

        // Add category
        webService.addCategory(createLinkedEntity("servicetype1", "CATEGORY"));

        // Add supported operation
        webService.addSupportedOperation(createLinkedEntity("Operation/Test/2", "OPERATION"));

        // Add documentation
        webService.addDocumentation(createLinkedEntity("Documentation/Test/2", "DOCUMENTATION"));

        // Add spatial extent
        webService.addSpatialExtent(createLinkedEntity("spatial2", "LOCATION"));

        // Add temporal extent
        webService.addTemporalExtent(createLinkedEntity("temporal2", "PERIODOFTIME"));

        // Add distribution
        webService.getDistribution().add(createLinkedEntity("Distribution/Test/2", "DISTRIBUTION"));

        return webService;
    }

    public static Operation createOperationTest2() {
        Operation operation = new Operation();
        operation.setUid("Operation/Test/2");
        operation.setMethod("GET");
        operation.addReturns("application/xml");
        operation.setTemplate("https://webservices.ingv.it/fdsnws/station/1/query{?starttime, endtime, network, station, location, channel, minlatitude, maxlatitude, minlongitude, maxlongitude, level, format}");

        // Add mappings
        List<Mapping> mappings = createMappingsForOperation();
        for (Mapping mapping : mappings) {
            operation.addMapping(createLinkedEntity("mapping"+ UUID.randomUUID().toString(), "MAPPING"));
        }

        // Add webservice reference
        operation.addWebservice(createLinkedEntity("WebService/Test/2", "WEBSERVICE"));

        return operation;
    }

    private static List<Mapping> createMappingsForOperation() {
        List<Mapping> mappings = new ArrayList<>();

        // Start time mapping
        Mapping startTimeMapping = new Mapping();
        startTimeMapping.setVariable("starttime");
        startTimeMapping.setRange("xsd:dateTime");
        startTimeMapping.setLabel("Start of the timespan");
        startTimeMapping.setProperty("schema:startDate");
        startTimeMapping.setValuePattern("YYYY-MM-DDThh:mm:ss");
        startTimeMapping.setDefaultValue("2010-01-01T00:00:00");
        startTimeMapping.setRequired("false");
        mappings.add(startTimeMapping);

        // End time mapping
        Mapping endTimeMapping = new Mapping();
        endTimeMapping.setVariable("endtime");
        endTimeMapping.setRange("xsd:dateTime");
        endTimeMapping.setLabel("End of the timespan");
        endTimeMapping.setProperty("schema:endDate");
        endTimeMapping.setValuePattern("YYYY-MM-DDThh:mm:ss");
        endTimeMapping.setDefaultValue("2019-03-01T00:00:00");
        endTimeMapping.setRequired("false");
        mappings.add(endTimeMapping);

        // Network mapping
        Mapping networkMapping = new Mapping();
        networkMapping.setVariable("network");
        networkMapping.setRange("xsd:string");
        networkMapping.setLabel("Network code");
        networkMapping.setDefaultValue("_NFOIRPINA");
        networkMapping.setRequired("true");
        mappings.add(networkMapping);

        // Station mapping
        Mapping stationMapping = new Mapping();
        stationMapping.setVariable("station");
        stationMapping.setRange("xsd:string");
        stationMapping.setLabel("Station code");
        stationMapping.setMultipleValues("true");
        stationMapping.setDefaultValue("AND3");
        stationMapping.setRequired("false");
        // Add parameter values
        List<String> stationValues = Arrays.asList("AND3", "AVG3", "BEL3", "BENI", "BSC3", "CGG3", "CLT3", "CMP3", "COL3", "CSG3");
        stationMapping.setParamValue(stationValues);
        mappings.add(stationMapping);

        // Format mapping
        Mapping formatMapping = new Mapping();
        formatMapping.setVariable("format");
        formatMapping.setRange("xsd:string");
        formatMapping.setLabel("Output Format");
        formatMapping.setProperty("schema:encodingFormat");
        formatMapping.setDefaultValue("xml");
        formatMapping.setReadOnlyValue("true");
        formatMapping.setRequired("true");
        formatMapping.addParamValue("xml");
        formatMapping.addParamValue("text");
        mappings.add(formatMapping);

        return mappings;
    }

    // ===============================
    // SOFTWARE COMPONENTS
    // ===============================

    public static SoftwareSourceCode createSoftwareSourceCodeQuakeML() {
        SoftwareSourceCode sourceCode = new SoftwareSourceCode();
        sourceCode.setName("Plugin for the conversion from QuakeML to GeoJSON payload");
        sourceCode.setDescription("Plugin for the conversion from QuakeML to GeoJSON payload");
        sourceCode.setSoftwareVersion("main");
        sourceCode.setCodeRepository("https://gitlab-ci-token:glpat-k4RsseLnMdVc5efsxtN4@epos-ci.brgm.fr/epos/converter-plugins/quakeml-plugin.git");
        sourceCode.setLicenseURL("https://epos-ci.brgm.fr/epos/converter-plugins/quakeml-plugin/-/blob/main/README.md");
        sourceCode.setMainEntityofPage("https://epos-ci.brgm.fr/epos/converter-plugins/quakeml-plugin/-/blob/main/README.md");
        sourceCode.addProgrammingLanguage("Java");
        sourceCode.setRuntimePlatform("java-1.10");

        // Add identifier
        Identifier pluginId = createIdentifier("ID", "Plugin/SoftwareSourceCode/QuakeML");
        sourceCode.addIdentifier(createLinkedEntity("identifier4", "IDENTIFIER"));

        // Add relation to software application
        sourceCode.addRelation(createLinkedEntity("Plugin/SoftwareApplication/QuakeML", "SOFTWAREAPPLICATION"));

        return sourceCode;
    }

    public static SoftwareApplication createSoftwareApplicationQuakeML() {
        SoftwareApplication application = new SoftwareApplication();
        application.setName("QuakeML Converter Application");
        application.setDescription("Application for converting QuakeML to GeoJSON");
        application.setRequirements("Java-Reflection;target/;QuakeML-main-jar-with-dependencies.jar;org.epos.quakeml.core.QuakeMLInvoker");

        // Add identifier
        Identifier appId = createIdentifier("ID", "Plugin/SoftwareApplication/QuakeML");
        application.addIdentifier(createLinkedEntity("identifier5", "IDENTIFIER"));

        // Add related operation
        application.addRelatedOperation(createLinkedEntity("Operation/Test/2", "OPERATION"));

        return application;
    }

    // ===============================
    // LOCATIONS
    // ===============================

    public static Location createGlobalLocation() {
        Location location = new Location();
        location.setUid("global");
        location.setLocation("POLYGON((180.0 -90.0 , -180.0 -90.0, -180.0 90.0 , 180.0 90.0,180.0 -90.0))");
        return location;
    }

    public static Location createPointLocation() {
        Location location = new Location();
        location.setUid("point");
        location.setLocation("POINT(15.085 37.525)");
        return location;
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    public static LinkedEntity createLinkedEntity(String instanceId, String entityType) {
        LinkedEntity linkedEntity = new LinkedEntity();
        linkedEntity.setUid(instanceId);
        linkedEntity.setEntityType(entityType.toUpperCase());
        return linkedEntity;
    }

    public static LinkedEntity createLinkedEntityFromObject(Object object, String entityType) {
        LinkedEntity linkedEntity = new LinkedEntity();
        linkedEntity.setUid(object.toString());
        linkedEntity.setEntityType(entityType.toUpperCase());
        return linkedEntity;
    }

    // ===============================
    // CONVENIENCE METHODS FOR TESTS
    // ===============================

    /**
     * Creates a complete test dataset with all related objects
     */
    public static DataProduct createCompleteTestDataset() {
        DataProduct dataset = createDatasetTest1();

        // You can add any additional setup here that's commonly needed
        // across multiple tests

        return dataset;
    }

    /**
     * Creates a complete web service with all related objects
     */
    public static WebService createCompleteTestWebService() {
        WebService webService = createWebServiceTest2();

        // You can add any additional setup here that's commonly needed
        // across multiple tests

        return webService;
    }

    /**
     * Creates all categories for testing
     */
    public static List<Category> createAllTestCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(createCategoryTest1());
        categories.add(createCategoryTest2());
        categories.add(createCategoryTest3());
        categories.add(createFacilityCategory());
        return categories;
    }

    /**
     * Creates all category schemes for testing
     */
    public static List<CategoryScheme> createAllTestCategorySchemes() {
        List<CategoryScheme> schemes = new ArrayList<>();
        schemes.add(createDomainTestCategoryScheme());
        schemes.add(createFacilityCategoryScheme());
        return schemes;
    }
}