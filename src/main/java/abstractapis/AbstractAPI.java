package abstractapis;

import commonapis.*;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractAPI<T> {

    protected final Logger LOG = Logger.getLogger(getClass().getName());

    protected Class<?> edmClass;
    protected String entityName;

    // protected EposDataModelDAO dbaccess = EposDataModelDAO.getInstance();

    public AbstractAPI(String entityName, Class<?> edmClass) {
        this.edmClass = edmClass;
        this.entityName = entityName;
    }

    public EposDataModelDAO getDbaccess() {
        return EposDataModelDAO.getInstance();
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

    protected void logCreateStart(T obj, StatusType overrideStatus) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "==> [CREATE START] Entity Type: {0}, EDM Class: {1}", 
                    new Object[]{entityName, edmClass != null ? edmClass.getSimpleName() : "null"});
        }
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[MEMORY STATE] Used: {0}MB, Free: {1}MB, Total: {2}MB, Max: {3}MB", 
                    new Object[]{usedMemory / (1024 * 1024), runtime.freeMemory() / (1024 * 1024), runtime.totalMemory() / (1024 * 1024), runtime.maxMemory() / (1024 * 1024)});
        }

        if (obj == null) {
            LOG.log(Level.WARNING, "[VALIDATION WARNING] Incoming object for {0} is NULL!", entityName);
            return;
        }

        try {
            List<String> emptyFields = new java.util.ArrayList<>();
            List<String> populatedFields = new java.util.ArrayList<>();
            
            for (Method method : obj.getClass().getMethods()) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0 && !method.getName().equals("getClass")) {
                    String fieldName = method.getName().substring(3);
                    fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                    try {
                        Object val = method.invoke(obj);
                        if (val == null) {
                            emptyFields.add(fieldName);
                        } else if (val instanceof String && ((String) val).trim().isEmpty()) {
                            emptyFields.add(fieldName);
                        } else if (val instanceof java.util.Collection && ((java.util.Collection<?>) val).isEmpty()) {
                            emptyFields.add(fieldName);
                        } else {
                            populatedFields.add(fieldName + "=" + truncateString(val.toString(), 120));
                        }
                    } catch (Exception e) {
                        // ignore reflection errors during logging
                    }
                }
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "[VALIDATION] Populated fields for {0}: {1}", new Object[]{entityName, populatedFields});
                LOG.log(Level.FINE, "[VALIDATION] Null/Empty fields for {0}: {1}", new Object[]{entityName, emptyFields});
            }
            
            // Highlight missing key identifiers which could lead to empty or broken draft creations
            if (emptyFields.contains("instanceId") && emptyFields.contains("uid") && emptyFields.contains("metaId")) {
                LOG.log(Level.WARNING, "[VALIDATION WARNING] {0} object has NO identifiers set! (instanceId, uid, and metaId are all null or empty)", entityName);
            }
            
            // Check for other entity-specific crucial empty fields
            if ("Operation".equalsIgnoreCase(entityName) || "metadataapis.OperationAPI".contains(getClass().getSimpleName())) {
                if (emptyFields.contains("method") || emptyFields.contains("template")) {
                    LOG.log(Level.WARNING, "[VALIDATION WARNING] Operation has missing method/template! Method empty: {0}, Template empty: {1}", 
                            new Object[]{emptyFields.contains("method"), emptyFields.contains("template")});
                }
            } else if ("Mapping".equalsIgnoreCase(entityName) || "metadataapis.MappingAPI".contains(getClass().getSimpleName())) {
                if (emptyFields.contains("variable") || emptyFields.contains("property")) {
                    LOG.log(Level.WARNING, "[VALIDATION WARNING] Mapping has missing variable/property! Variable empty: {0}, Property empty: {1}", 
                            new Object[]{emptyFields.contains("variable"), emptyFields.contains("property")});
                }
            } else if ("Distribution".equalsIgnoreCase(entityName) || "metadataapis.DistributionAPI".contains(getClass().getSimpleName())) {
                if (emptyFields.contains("format") || emptyFields.contains("licence") || emptyFields.contains("title")) {
                    LOG.log(Level.WARNING, "[VALIDATION WARNING] Distribution has missing details! Format empty: {0}, Licence empty: {1}, Title empty: {2}", 
                            new Object[]{emptyFields.contains("format"), emptyFields.contains("licence"), emptyFields.contains("title")});
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[LOGGING ERROR] Reflection-based field inspection failed", e);
        }
    }

    private String truncateString(String s, int limit) {
        if (s == null) return "null";
        if (s.length() <= limit) return s;
        return s.substring(0, limit) + "...";
    }

    protected void logCreateEnd(LinkedEntity result, Throwable error) {
        if (error != null) {
            LOG.log(Level.SEVERE, "<== [CREATE ERROR] Entity Type: " + entityName + " creation failed with exception", error);
        } else if (result != null) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "<== [CREATE SUCCESS] Entity Type: {0}, LinkedEntity Result -> instanceId: {1}, metaId: {2}, uid: {3}", 
                        new Object[]{entityName, result.getInstanceId(), result.getMetaId(), result.getUid()});
            }
        } else {
            LOG.log(Level.WARNING, "<== [CREATE WARNING] Entity Type: " + entityName + " returned a null LinkedEntity!");
        }
    }

    protected boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                boolean isSet = value != null;
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "[DEPENDENCY CHECK] Field: {0} in {1}, explicitlySet={2}, value={3}", 
                            new Object[]{fieldName, obj.getClass().getSimpleName(), isSet, value != null ? truncateString(value.toString(), 100) : "null"});
                }
                return isSet;
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "[DEPENDENCY CHECK] Field {0} not found in class {1}", 
                            new Object[]{fieldName, obj.getClass().getName()});
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[DEPENDENCY CHECK ERROR] Error checking field: " + fieldName + " on " + obj.getClass().getSimpleName(), e);
        }
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public abstract T retrieve(String instanceId);

    public abstract T retrieveByUID(String uid);

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
        ENTITY_CLASSES.put(EntityNames.ATTRIBUTION.name(), Attribution.class);
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
        API_CLASSES.put(EntityNames.ATTRIBUTION.name(), new AttributionAPI(EntityNames.ATTRIBUTION.name(), Attribution.class));
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
