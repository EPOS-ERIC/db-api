package abstractapis;

import commonapis.*;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAPI<T> {

    protected Class<?> edmClass;
    protected String entityName;

    protected EposDataModelDAO dbaccess = EposDataModelDAO.getInstance();

    public AbstractAPI(String entityName, Class<?> edmClass) {
        this.edmClass = edmClass;
        this.entityName = entityName;
    }

    public EposDataModelDAO getDbaccess() {
        return dbaccess;
    }

    public void setEdmClass(Class<?> edmClass) {
        this.edmClass = edmClass;
    }

    public Class<?> getEdmClass() {
        return edmClass;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public abstract LinkedEntity create(T obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate);

    public abstract T retrieve(String instanceId);

    public abstract Boolean delete(String instanceId);

    public abstract List<T> retrieveBunch(List<String> entities);

    public abstract List<T> retrieveAll();

    public abstract List<T> retrieveAllWithStatus(StatusType status);

    public abstract LinkedEntity retrieveLinkedEntity(String instanceId);

    // Simplified retrieveAPI and retrieveClass methods using a Map
    private static final Map<String, Class<?>> ENTITY_CLASSES = new HashMap<>();
    private static final Map<String, AbstractAPI> API_CLASSES = new HashMap<>();

    static {
        // Map entity types to their respective classes
        ENTITY_CLASSES.put(EntityNames.PERSON.name(), Person.class);
        ENTITY_CLASSES.put(EntityNames.MAPPING.name(), Mapping.class);
        ENTITY_CLASSES.put(EntityNames.CATEGORY.name(), Category.class);
        ENTITY_CLASSES.put(EntityNames.FACILITY.name(), Facility.class);
        ENTITY_CLASSES.put(EntityNames.EQUIPMENT.name(), Equipment.class);
        ENTITY_CLASSES.put(EntityNames.OPERATION.name(), Operation.class);
        ENTITY_CLASSES.put(EntityNames.WEBSERVICE.name(), Webservice.class);
        ENTITY_CLASSES.put(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
        ENTITY_CLASSES.put(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
        ENTITY_CLASSES.put(EntityNames.DISTRIBUTION.name(), Distribution.class);
        ENTITY_CLASSES.put(EntityNames.ORGANIZATION.name(), Organization.class);
        ENTITY_CLASSES.put(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class);
        ENTITY_CLASSES.put(EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class);
        ENTITY_CLASSES.put(EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class);
        ENTITY_CLASSES.put(EntityNames.ADDRESS.name(), Address.class);
        ENTITY_CLASSES.put(EntityNames.ELEMENT.name(), Element.class);
        ENTITY_CLASSES.put(EntityNames.LOCATION.name(), Spatial.class);
        ENTITY_CLASSES.put(EntityNames.PERIODOFTIME.name(), Temporal.class);
        ENTITY_CLASSES.put(EntityNames.IDENTIFIER.name(), Identifier.class);
        ENTITY_CLASSES.put(EntityNames.QUANTITATIVEVALUE.name(), Quantitativevalue.class);
        ENTITY_CLASSES.put(EntityNames.DOCUMENTATION.name(), Element.class);
        ENTITY_CLASSES.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), Parameter.class);
        ENTITY_CLASSES.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), Parameter.class);
        ENTITY_CLASSES.put(EntityNames.OUTPUTMAPPING.name(), OutputMapping.class);
        ENTITY_CLASSES.put(EntityNames.PAYLOAD.name(), Payload.class);

        // Map entity types to their respective API classes
        API_CLASSES.put(EntityNames.PERSON.name(), new PersonAPI(EntityNames.PERSON.name(), Person.class));
        API_CLASSES.put(EntityNames.MAPPING.name(), new MappingAPI(EntityNames.MAPPING.name(), Mapping.class));
        API_CLASSES.put(EntityNames.CATEGORY.name(), new CategoryAPI(EntityNames.CATEGORY.name(), Category.class));
        API_CLASSES.put(EntityNames.FACILITY.name(), new FacilityAPI(EntityNames.FACILITY.name(), Facility.class));
        API_CLASSES.put(EntityNames.EQUIPMENT.name(), new EquipmentAPI(EntityNames.EQUIPMENT.name(), Equipment.class));
        API_CLASSES.put(EntityNames.OPERATION.name(), new OperationAPI(EntityNames.OPERATION.name(), Operation.class));
        API_CLASSES.put(EntityNames.WEBSERVICE.name(), new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class));
        API_CLASSES.put(EntityNames.DATAPRODUCT.name(), new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class));
        API_CLASSES.put(EntityNames.CONTACTPOINT.name(), new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class));
        API_CLASSES.put(EntityNames.DISTRIBUTION.name(), new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class));
        API_CLASSES.put(EntityNames.ORGANIZATION.name(), new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class));
        API_CLASSES.put(EntityNames.CATEGORYSCHEME.name(), new CategorySchemeAPI(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class));
        API_CLASSES.put(EntityNames.SOFTWARESOURCECODE.name(), new SoftwareSourceCodeAPI(EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class));
        API_CLASSES.put(EntityNames.SOFTWAREAPPLICATION.name(), new SoftwareApplicationAPI(EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class));
        API_CLASSES.put(EntityNames.ADDRESS.name(), new AddressAPI(EntityNames.ADDRESS.name(), Address.class));
        API_CLASSES.put(EntityNames.ELEMENT.name(), new ElementAPI(EntityNames.ELEMENT.name(), Element.class));
        API_CLASSES.put(EntityNames.LOCATION.name(), new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class));
        API_CLASSES.put(EntityNames.PERIODOFTIME.name(), new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class));
        API_CLASSES.put(EntityNames.IDENTIFIER.name(), new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class));
        API_CLASSES.put(EntityNames.QUANTITATIVEVALUE.name(), new QuantitativeValueAPI(EntityNames.QUANTITATIVEVALUE.name(), Quantitativevalue.class));
        API_CLASSES.put(EntityNames.DOCUMENTATION.name(), new DocumentationAPI(EntityNames.DOCUMENTATION.name(), Element.class));
        API_CLASSES.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), Parameter.class));
        API_CLASSES.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), Parameter.class));
        API_CLASSES.put(EntityNames.OUTPUTMAPPING.name(), new OutputMappingAPI(EntityNames.OUTPUTMAPPING.name(), OutputMapping.class));
        API_CLASSES.put(EntityNames.PAYLOAD.name(), new PayloadAPI(EntityNames.PAYLOAD.name(), Payload.class));
    }

    public static AbstractAPI retrieveAPI(String entityType) {
        return API_CLASSES.get(entityType);
    }

    public static Class<?> retrieveClass(String entityType) {
        return ENTITY_CLASSES.get(entityType);
    }
}