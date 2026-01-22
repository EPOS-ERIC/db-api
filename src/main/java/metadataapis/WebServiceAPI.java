package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Element;
import model.Identifier;
import model.Operation;
import model.Organization;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebServiceAPI extends AbstractAPI<org.epos.eposdatamodel.WebService> {

    public WebServiceAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(WebService obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Webservice> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            Webservice selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Webservice item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Webservice edmobj = new Webservice();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setEntrypoint(obj.getEntryPoint());
        edmobj.setKeywords(String.join("\\|", Optional.ofNullable(obj.getKeywords()).orElse("")));
        edmobj.setLicense(obj.getLicense());
        edmobj.setAaaitypes(obj.getAaaiTypes());

        if (obj.getDatePublished() != null)
            edmobj.setDatapublished(obj.getDatePublished());
        if (obj.getDateModified() != null)
            edmobj.setDatamodified(obj.getDateModified());

        getDbaccess().updateObject(edmobj);

        /** PUBLISHER (Provider) **/
        if (obj.getProvider() != null) {
            if (relationFromUpdate != null && obj.getProvider().equals(relationFromUpdate)) {
                obj.setProvider(relationToUpdate);
            }
            Organization organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, obj.getProvider(), overrideStatus, Organization.class, false);
            if(organization1 != null) {
                edmobj.setProvider(organization1.getInstanceId());
                getDbaccess().updateObject(edmobj); // Update the main entity with provider ID
            }
        }

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus);

        // DOCUMENTATION
        if (obj.getDocumentation() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getDocumentation(), relationFromUpdate, relationToUpdate,
                    WebserviceElement.class, Element.class,
                    "webserviceInstance", WebserviceElement::getElementInstance, WebserviceElement::setWebserviceInstance, WebserviceElement::setElementInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // IDENTIFIER
        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    WebserviceIdentifier.class, Identifier.class,
                    "webserviceInstance", WebserviceIdentifier::getIdentifierInstance, WebserviceIdentifier::setWebserviceInstance, WebserviceIdentifier::setIdentifierInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // SPATIAL
        if (obj.getSpatialExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                    WebserviceSpatial.class, Spatial.class,
                    "webserviceInstance", WebserviceSpatial::getSpatialInstance, WebserviceSpatial::setWebserviceInstance, WebserviceSpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // TEMPORAL
        if (obj.getTemporalExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                    WebserviceTemporal.class, Temporal.class,
                    "webserviceInstance", WebserviceTemporal::getTemporalInstance, WebserviceTemporal::setWebserviceInstance, WebserviceTemporal::setTemporalInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // DISTRIBUTION
        if (obj.getDistribution() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getDistribution(), relationFromUpdate, relationToUpdate,
                    WebserviceDistribution.class, model.Distribution.class,
                    "webserviceInstance", WebserviceDistribution::getDistributionInstance, WebserviceDistribution::setWebserviceInstance, WebserviceDistribution::setDistributionInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // SUPPORTED OPERATION
        if (obj.getSupportedOperation() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSupportedOperation(), relationFromUpdate, relationToUpdate,
                    OperationWebservice.class, Operation.class,
                    "webserviceInstance", OperationWebservice::getOperationInstance, OperationWebservice::setWebserviceInstance, OperationWebservice::setOperationInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // RELATION
        if (obj.getWebserviceRelation() != null) {
            // Polymorphic relation self-reference (Webservice -> Webservice)
            // Manual optimization required as ID structure might differ (WebserviceRelationId)
            // Or simple loop with optimized delete later
            for (LinkedEntity relation : obj.getWebserviceRelation()) {
                Webservice webService = (Webservice) RelationChecker.checkRelation(obj, previousObj, null, relation, overrideStatus, Webservice.class, false);
                if (webService != null) {
                    WebserviceRelationId wid = new WebserviceRelationId();
                    wid.setWebserviceInstanceId(edmobj.getInstanceId());
                    wid.setEntityInstanceId(webService.getInstanceId());

                    WebserviceRelation pi = new WebserviceRelation();
                    pi.setId(wid);
                    pi.setWebserviceInstance(edmobj);
                    pi.setResourceEntity(EntityNames.WEBSERVICE.name());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("webserviceInstance", instanceId, WebserviceIdentifier.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceTemporal.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceElement.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceSpatial.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceContactpoint.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceDistribution.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceCategory.class);

        // WebserviceRelation uses composite key, careful with deleteRelations helper if it relies on specific key name
        // Helper works if passed correct key name. Here key is inside Embeddable ID.
        // Standard deleteRelations might fail if DAO expects simple field.
        // Fallback to loop for this specific one if necessary, or ensure DAO handles "id.webserviceInstanceId"
        for(Object object : getDbaccess().getAllFromDB(WebserviceRelation.class)){
            WebserviceRelation item = (WebserviceRelation) object;
            if(item.getId().getWebserviceInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        for (Webservice object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.WebService retrieve(String instanceId) {
        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Webservice edmobj = elementList.get(0);
        org.epos.eposdatamodel.WebService o = new org.epos.eposdatamodel.WebService();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setDateModified(edmobj.getDatamodified());
        o.setDatePublished(edmobj.getDatapublished());
        o.setDescription(edmobj.getDescription());
        o.setEntryPoint(edmobj.getEntrypoint());
        o.setLicense(edmobj.getLicense());
        o.setName(edmobj.getName());
        o.setAaaiTypes(edmobj.getAaaitypes());
        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isBlank())
            for (String item : edmobj.getKeywords().split("\\|"))
                o.addKeywords(item);

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceCategory.class)) {
            WebserviceCategory item = (WebserviceCategory) object;
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
            o.addCategory(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceContactpoint.class)) {
            WebserviceContactpoint item = (WebserviceContactpoint) object;
            LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
            o.addContactPoint(le);
        }

        if (edmobj.getProvider() != null) {
            o.setProvider(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(edmobj.getProvider()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceElement.class)) {
            WebserviceElement item = (WebserviceElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.DOCUMENTATION.name()))
                o.addDocumentation(retrieveAPI(EntityNames.DOCUMENTATION.name()).retrieveLinkedEntity(el.getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceIdentifier.class)) {
            WebserviceIdentifier item = (WebserviceIdentifier) object;
            LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
            o.addIdentifier(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceSpatial.class)) {
            WebserviceSpatial item = (WebserviceSpatial) object;
            LinkedEntity le = retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
            o.addSpatialExtentItem(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceTemporal.class)) {
            WebserviceTemporal item = (WebserviceTemporal) object;
            LinkedEntity le = retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
            o.addTemporalExtent(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), OperationWebservice.class)) {
            OperationWebservice item = (OperationWebservice) object;
            LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
            o.addSupportedOperation(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(), WebserviceRelation.class)) {
            WebserviceRelation item = (WebserviceRelation) object;
            LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getId().getEntityInstanceId());
            o.addWebserviceRelation(le);
        }

        o = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.WebService retrieveByUID(String uid) {
        List<Webservice> returnList = getDbaccess().getOneFromDBByUID(uid, Webservice.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Webservice.class));
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Webservice.class));
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Webservice.class, status));
    }
    private List<org.epos.eposdatamodel.WebService> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        if (elementList != null && !elementList.isEmpty()) {
            Webservice edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.WEBSERVICE.name());
            return o;
        }
        return null;
    }
}