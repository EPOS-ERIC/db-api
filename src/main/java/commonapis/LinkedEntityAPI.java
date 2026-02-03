package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplicationInputParameter;
import org.epos.eposdatamodel.SoftwareApplicationOutputParameter;

import utilities.ReflectionCache;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LinkedEntityAPI {

    private static final Logger LOG = Logger.getLogger(LinkedEntityAPI.class.getName());

    private static final Map<String, AbstractAPI> apiMap = new HashMap<>();
    private static final Map<String, Class<?>> edmClassMap = new HashMap<>();
    private static final Map<String, Class<?>> modelClassMap = new HashMap<>();

    static {
        apiMap.put(EntityNames.ATTRIBUTION.name(), new AttributionAPI(EntityNames.ATTRIBUTION.name(), Attribution.class));
        apiMap.put(EntityNames.PERSON.name(), new PersonAPI(EntityNames.PERSON.name(), Person.class));
        apiMap.put(EntityNames.MAPPING.name(), new MappingAPI(EntityNames.MAPPING.name(), Mapping.class));
        apiMap.put(EntityNames.CATEGORY.name(), new CategoryAPI(EntityNames.CATEGORY.name(), Category.class));
        apiMap.put(EntityNames.FACILITY.name(), new FacilityAPI(EntityNames.FACILITY.name(), Facility.class));
        apiMap.put(EntityNames.EQUIPMENT.name(), new EquipmentAPI(EntityNames.EQUIPMENT.name(), Equipment.class));
        apiMap.put(EntityNames.OPERATION.name(), new OperationAPI(EntityNames.OPERATION.name(), Operation.class));
        apiMap.put(EntityNames.WEBSERVICE.name(), new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class));
        apiMap.put(EntityNames.DATAPRODUCT.name(), new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class));
        apiMap.put(EntityNames.CONTACTPOINT.name(), new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class));
        apiMap.put(EntityNames.DISTRIBUTION.name(), new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class));
        apiMap.put(EntityNames.ORGANIZATION.name(), new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class));
        apiMap.put(EntityNames.CATEGORYSCHEME.name(), new CategorySchemeAPI(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class));
        apiMap.put(EntityNames.SOFTWARESOURCECODE.name(), new SoftwareSourceCodeAPI(EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATION.name(), new SoftwareApplicationAPI(EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class));
        apiMap.put(EntityNames.ADDRESS.name(), new AddressAPI(EntityNames.ADDRESS.name(), Address.class));
        apiMap.put(EntityNames.ELEMENT.name(), new ElementAPI(EntityNames.ELEMENT.name(), Element.class));
        apiMap.put(EntityNames.LOCATION.name(), new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class));
        apiMap.put(EntityNames.PERIODOFTIME.name(), new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class));
        apiMap.put(EntityNames.IDENTIFIER.name(), new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class));
        apiMap.put(EntityNames.QUANTITATIVEVALUE.name(), new QuantitativeValueAPI(EntityNames.QUANTITATIVEVALUE.name(), Quantitativevalue.class));
        apiMap.put(EntityNames.DOCUMENTATION.name(), new DocumentationAPI(EntityNames.DOCUMENTATION.name(), Element.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), SoftwareApplicationInputParameter.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), SoftwareApplicationOutputParameter.class));
        apiMap.put(EntityNames.PAYLOAD.name(), new PayloadAPI(EntityNames.PAYLOAD.name(), Payload.class));
        apiMap.put(EntityNames.OUTPUTMAPPING.name(), new OutputMappingAPI(EntityNames.OUTPUTMAPPING.name(), OutputMapping.class));

        edmClassMap.put(EntityNames.ATTRIBUTION.name(), org.epos.eposdatamodel.Attribution.class);
        edmClassMap.put(EntityNames.PERSON.name(), org.epos.eposdatamodel.Person.class);
        edmClassMap.put(EntityNames.MAPPING.name(), org.epos.eposdatamodel.Mapping.class);
        edmClassMap.put(EntityNames.CATEGORY.name(), org.epos.eposdatamodel.Category.class);
        edmClassMap.put(EntityNames.FACILITY.name(), org.epos.eposdatamodel.Facility.class);
        edmClassMap.put(EntityNames.EQUIPMENT.name(), org.epos.eposdatamodel.Equipment.class);
        edmClassMap.put(EntityNames.OPERATION.name(), org.epos.eposdatamodel.Operation.class);
        edmClassMap.put(EntityNames.WEBSERVICE.name(), org.epos.eposdatamodel.WebService.class);
        edmClassMap.put(EntityNames.DATAPRODUCT.name(), org.epos.eposdatamodel.DataProduct.class);
        edmClassMap.put(EntityNames.CONTACTPOINT.name(), org.epos.eposdatamodel.ContactPoint.class);
        edmClassMap.put(EntityNames.DISTRIBUTION.name(), org.epos.eposdatamodel.Distribution.class);
        edmClassMap.put(EntityNames.ORGANIZATION.name(), org.epos.eposdatamodel.Organization.class);
        edmClassMap.put(EntityNames.CATEGORYSCHEME.name(), org.epos.eposdatamodel.CategoryScheme.class);
        edmClassMap.put(EntityNames.SOFTWARESOURCECODE.name(), org.epos.eposdatamodel.SoftwareSourceCode.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATION.name(), org.epos.eposdatamodel.SoftwareApplication.class);
        edmClassMap.put(EntityNames.ADDRESS.name(), org.epos.eposdatamodel.Address.class);
        edmClassMap.put(EntityNames.ELEMENT.name(), org.epos.eposdatamodel.Element.class);
        edmClassMap.put(EntityNames.LOCATION.name(), org.epos.eposdatamodel.Location.class);
        edmClassMap.put(EntityNames.PERIODOFTIME.name(), org.epos.eposdatamodel.PeriodOfTime.class);
        edmClassMap.put(EntityNames.IDENTIFIER.name(), org.epos.eposdatamodel.Identifier.class);
        edmClassMap.put(EntityNames.QUANTITATIVEVALUE.name(), org.epos.eposdatamodel.QuantitativeValue.class);
        edmClassMap.put(EntityNames.DOCUMENTATION.name(), org.epos.eposdatamodel.Documentation.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), org.epos.eposdatamodel.SoftwareApplicationInputParameter.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), org.epos.eposdatamodel.SoftwareApplicationOutputParameter.class);
        edmClassMap.put(EntityNames.PAYLOAD.name(), org.epos.eposdatamodel.Payload.class);
        edmClassMap.put(EntityNames.OUTPUTMAPPING.name(), org.epos.eposdatamodel.OutputMapping.class);

        // Model class map for JPA entities
        modelClassMap.put(EntityNames.ATTRIBUTION.name(), Attribution.class);
        modelClassMap.put(EntityNames.PERSON.name(), Person.class);
        modelClassMap.put(EntityNames.MAPPING.name(), Mapping.class);
        modelClassMap.put(EntityNames.CATEGORY.name(), Category.class);
        modelClassMap.put(EntityNames.FACILITY.name(), Facility.class);
        modelClassMap.put(EntityNames.EQUIPMENT.name(), Equipment.class);
        modelClassMap.put(EntityNames.OPERATION.name(), Operation.class);
        modelClassMap.put(EntityNames.WEBSERVICE.name(), Webservice.class);
        modelClassMap.put(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
        modelClassMap.put(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
        modelClassMap.put(EntityNames.DISTRIBUTION.name(), Distribution.class);
        modelClassMap.put(EntityNames.ORGANIZATION.name(), Organization.class);
        modelClassMap.put(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class);
        modelClassMap.put(EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class);
        modelClassMap.put(EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class);
        modelClassMap.put(EntityNames.ADDRESS.name(), Address.class);
        modelClassMap.put(EntityNames.ELEMENT.name(), Element.class);
        modelClassMap.put(EntityNames.LOCATION.name(), Spatial.class);
        modelClassMap.put(EntityNames.PERIODOFTIME.name(), Temporal.class);
        modelClassMap.put(EntityNames.IDENTIFIER.name(), Identifier.class);
        modelClassMap.put(EntityNames.QUANTITATIVEVALUE.name(), Quantitativevalue.class);
        modelClassMap.put(EntityNames.DOCUMENTATION.name(), Element.class);
        modelClassMap.put(EntityNames.PAYLOAD.name(), Payload.class);
        modelClassMap.put(EntityNames.OUTPUTMAPPING.name(), OutputMapping.class);
    }

    public static LinkedEntity createFromLinkedEntity(LinkedEntity obj, StatusType overrideStatus, Versioningstatus parentVersioningstatus, String provenance) {

        AbstractAPI api = apiMap.get(obj.getEntityType().toUpperCase());
        Class<?> edmClass = edmClassMap.get(obj.getEntityType().toUpperCase());
        Class<?> modelClass = modelClassMap.get(obj.getEntityType().toUpperCase());

        if (api == null || edmClass == null) {
            return null;
        }

        if (obj.getInstanceId() != null) {
            List<Versioningstatus> existing = getDbaccess().getOneFromDB(
                    obj.getInstanceId(), null, null, null, Versioningstatus.class);
            if (!existing.isEmpty()) {
                Versioningstatus vs = existing.get(0);
                return createLinkedEntityFromVersioningstatus(vs, obj.getEntityType());
            }
        }

        if (obj.getUid() != null && modelClass != null) {
            List<Object> existingByUid = getDbaccess().getOneFromDBByUID(obj.getUid(), modelClass);
            if (!existingByUid.isEmpty()) {
                // Find the version with matching status (if specified) or the latest
                Object bestMatch = findBestMatchingVersion(existingByUid, overrideStatus);
                if (bestMatch != null) {
                    return extractLinkedEntityFromModel(bestMatch, obj.getEntityType());
                }
            }
        }

        if (obj.getMetaId() != null && obj.getInstanceId() == null) {
            List<Versioningstatus> existingByMeta = getDbaccess().getOneFromDB(
                    null, obj.getMetaId(), null, null, Versioningstatus.class);
            if (!existingByMeta.isEmpty()) {
                // Find version with matching status
                for (Versioningstatus vs : existingByMeta) {
                    if (overrideStatus == null ||
                            (vs.getStatus() != null && vs.getStatus().toString().equals(overrideStatus.toString()))) {
                        return createLinkedEntityFromVersioningstatus(vs, obj.getEntityType());
                    }
                }
                // If no status match, return the first one
                Versioningstatus vs = existingByMeta.get(0);
                return createLinkedEntityFromVersioningstatus(vs, obj.getEntityType());
            }
        }
        return null;
    }

    public static Object retrieveFromLinkedEntity(LinkedEntity obj) {
        AbstractAPI api = apiMap.get(obj.getEntityType().toUpperCase());
        Class<?> modelClass = modelClassMap.get(obj.getEntityType().toUpperCase());

        if (api == null) {
            return null;
        }

        if (obj.getInstanceId() != null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    null,
                    Versioningstatus.class
            );

            if (!returnList.isEmpty()) {
                Versioningstatus edmobj = returnList.get(0);
                return api.retrieve(edmobj.getInstanceId());
            }
        }

        if (obj.getUid() != null && modelClass != null) {
            List<Object> existingByUid = getDbaccess().getOneFromDBByUID(obj.getUid(), modelClass);
            if (!existingByUid.isEmpty()) {
                // Return the first matching entity (could be enhanced to match by status)
                Object modelEntity = existingByUid.get(0);
                String instanceId = extractInstanceId(modelEntity);
                if (instanceId != null) {
                    LOG.fine("Found entity by UID lookup: " + obj.getUid() + " -> instanceId: " + instanceId);
                    return api.retrieve(instanceId);
                }
            }
        }

        if (obj.getMetaId() != null && obj.getInstanceId() == null) {
            List<Versioningstatus> byMeta = getDbaccess().getOneFromDB(
                    null, obj.getMetaId(), null, null, Versioningstatus.class);
            if (!byMeta.isEmpty()) {
                Versioningstatus edmobj = byMeta.get(0);
                return api.retrieve(edmobj.getInstanceId());
            }
        }

        LOG.fine("Entity not found for LinkedEntity: " + obj.getEntityType() + " uid=" + obj.getUid());
        return null;
    }

    private static String extractInstanceId(Object modelEntity) {
        return ReflectionCache.getInstanceId(modelEntity);
    }


    private static Object findBestMatchingVersion(List<Object> versions, StatusType targetStatus) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        // If no target status specified, return the first (usually latest)
        if (targetStatus == null) {
            return versions.get(0);
        }

        // Try to find a version with matching status
        for (Object v : versions) {
            String status = getModelVersionStatus(v);
            if (status != null && status.equals(targetStatus.toString())) {
                return v;
            }
        }

        // No exact match - return the first one
        return versions.get(0);
    }


    private static String getModelVersionStatus(Object modelEntity) {
        return ReflectionCache.getVersionStatus(modelEntity);
    }

    private static LinkedEntity createLinkedEntityFromVersioningstatus(Versioningstatus vs, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(vs.getInstanceId());
        le.setMetaId(vs.getMetaId());
        le.setUid(vs.getUid());
        le.setEntityType(entityType);
        return le;
    }

    private static LinkedEntity extractLinkedEntityFromModel(Object modelEntity, String entityType) {
        if (modelEntity == null) return null;
        
        LinkedEntity le = new LinkedEntity();
        le.setEntityType(entityType);
        
        String instanceId = ReflectionCache.getInstanceId(modelEntity);
        if (instanceId != null) le.setInstanceId(instanceId);
        
        String metaId = ReflectionCache.getMetaId(modelEntity);
        if (metaId != null) le.setMetaId(metaId);
        
        String uid = ReflectionCache.getUid(modelEntity);
        if (uid != null) le.setUid(uid);
        
        return le;
    }

    private static EposDataModelDAO getDbaccess() {
        return EposDataModelDAO.getInstance();
    }
}