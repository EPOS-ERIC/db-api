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

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebServiceAPI extends AbstractAPI<org.epos.eposdatamodel.WebService> {

    public WebServiceAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(WebService obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Capture if fields were explicitly set BEFORE any processing
        boolean categoryExplicitlySet = isFieldExplicitlySet(obj, "category");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");
        boolean documentationExplicitlySet = isFieldExplicitlySet(obj, "documentation");
        boolean identifierExplicitlySet = isFieldExplicitlySet(obj, "identifier");
        boolean spatialExtentExplicitlySet = isFieldExplicitlySet(obj, "spatialExtent");
        boolean temporalExtentExplicitlySet = isFieldExplicitlySet(obj, "temporalExtent");
        boolean distributionExplicitlySet = isFieldExplicitlySet(obj, "distribution");
        boolean supportedOperationExplicitlySet = isFieldExplicitlySet(obj, "supportedOperation");
        boolean webserviceRelationExplicitlySet = isFieldExplicitlySet(obj, "webserviceRelation");
        boolean providerExplicitlySet = isFieldExplicitlySet(obj, "provider");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();

        List<Webservice> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Webservice selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Webservice item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());

            if (previousObj == null) {
                previousObj = retrieve(selectedEntity.getInstanceId());
            }
        }

        obj = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

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

        if (obj.getDatePublished() != null) edmobj.setDatapublished(obj.getDatePublished());
        if (obj.getDateModified() != null) edmobj.setDatamodified(obj.getDateModified());

        getDbaccess().updateObject(edmobj);

        /** PROVIDER **/
        if (providerExplicitlySet || !isNewVersion) {
            if (obj.getProvider() != null) {
                if (relationFromUpdate != null && obj.getProvider().equals(relationFromUpdate)) {
                    obj.setProvider(relationToUpdate);
                }
                Organization organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, obj.getProvider(), overrideStatus, Organization.class, true);
                if (organization1 != null) {
                    edmobj.setProvider(organization1.getInstanceId());
                    getDbaccess().updateObject(edmobj);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyProviderFromPreviousVersion(oldInstanceId, edmobj);
        }

        /** CATEGORY **/
        if (categoryExplicitlySet || !isNewVersion) {
            if (obj.getCategory() != null && !obj.getCategory().isEmpty()) {
                CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyWebserviceCategoryRelations(oldInstanceId, edmobj);
        }

        /** CONTACTPOINT **/
        if (contactPointExplicitlySet || !isNewVersion) {
            if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
                ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyWebserviceContactPointRelations(oldInstanceId, edmobj);
        }

        /** DOCUMENTATION **/
        if (documentationExplicitlySet || !isNewVersion) {
            if (obj.getDocumentation() != null && !obj.getDocumentation().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getDocumentation(), relationFromUpdate, relationToUpdate,
                        WebserviceElement.class, Element.class,
                        "webserviceInstance", WebserviceElement::getElementInstance, WebserviceElement::setWebserviceInstance, WebserviceElement::setElementInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceElement.class, Element.class,
                    "webserviceInstance", WebserviceElement::getElementInstance, WebserviceElement::setWebserviceInstance, WebserviceElement::setElementInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** IDENTIFIER **/
        if (identifierExplicitlySet || !isNewVersion) {
            if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                        WebserviceIdentifier.class, Identifier.class,
                        "webserviceInstance", WebserviceIdentifier::getIdentifierInstance, WebserviceIdentifier::setWebserviceInstance, WebserviceIdentifier::setIdentifierInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceIdentifier.class, Identifier.class,
                    "webserviceInstance", WebserviceIdentifier::getIdentifierInstance, WebserviceIdentifier::setWebserviceInstance, WebserviceIdentifier::setIdentifierInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** SPATIAL **/
        if (spatialExtentExplicitlySet || !isNewVersion) {
            if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                        WebserviceSpatial.class, Spatial.class,
                        "webserviceInstance", WebserviceSpatial::getSpatialInstance, WebserviceSpatial::setWebserviceInstance, WebserviceSpatial::setSpatialInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceSpatial.class, Spatial.class,
                    "webserviceInstance", WebserviceSpatial::getSpatialInstance, WebserviceSpatial::setWebserviceInstance, WebserviceSpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** TEMPORAL **/
        if (temporalExtentExplicitlySet || !isNewVersion) {
            if (obj.getTemporalExtent() != null && !obj.getTemporalExtent().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                        WebserviceTemporal.class, Temporal.class,
                        "webserviceInstance", WebserviceTemporal::getTemporalInstance, WebserviceTemporal::setWebserviceInstance, WebserviceTemporal::setTemporalInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceTemporal.class, Temporal.class,
                    "webserviceInstance", WebserviceTemporal::getTemporalInstance, WebserviceTemporal::setWebserviceInstance, WebserviceTemporal::setTemporalInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** DISTRIBUTION **/
        if (distributionExplicitlySet || !isNewVersion) {
            if (obj.getDistribution() != null && !obj.getDistribution().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getDistribution(), relationFromUpdate, relationToUpdate,
                        WebserviceDistribution.class, model.Distribution.class,
                        "webserviceInstance", WebserviceDistribution::getDistributionInstance, WebserviceDistribution::setWebserviceInstance, WebserviceDistribution::setDistributionInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceDistribution.class, model.Distribution.class,
                    "webserviceInstance", WebserviceDistribution::getDistributionInstance, WebserviceDistribution::setWebserviceInstance, WebserviceDistribution::setDistributionInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** SUPPORTED OPERATION **/
        if (supportedOperationExplicitlySet || !isNewVersion) {
            if (obj.getSupportedOperation() != null && !obj.getSupportedOperation().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getSupportedOperation(), relationFromUpdate, relationToUpdate,
                        OperationWebservice.class, Operation.class,
                        "webserviceInstance", OperationWebservice::getOperationInstance, OperationWebservice::setWebserviceInstance, OperationWebservice::setOperationInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OperationWebservice.class, Operation.class,
                    "webserviceInstance", OperationWebservice::getOperationInstance, OperationWebservice::setWebserviceInstance, OperationWebservice::setOperationInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        /** WEBSERVICE RELATION **/
        if (webserviceRelationExplicitlySet || !isNewVersion) {
            // Delete existing relations if updating same version (to avoid duplicates)
            if (isUpdate && !isNewVersion) {
                deleteWebserviceRelations(edmobj.getInstanceId());
            }

            if (obj.getWebserviceRelation() != null && !obj.getWebserviceRelation().isEmpty()) {
                for (LinkedEntity le : obj.getWebserviceRelation()) {
                    // Get the related Webservice entity
                    List<Webservice> relatedWsList = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceId(le.getInstanceId(), Webservice.class);

                    if (relatedWsList == null || relatedWsList.isEmpty()) {
                        // Try to find by UID if instanceId not found
                        relatedWsList = EposDataModelDAO.getInstance()
                                .getOneFromDBByUID(le.getUid(), Webservice.class);
                    }

                    if (relatedWsList != null && !relatedWsList.isEmpty()) {
                        Webservice relatedWs = relatedWsList.get(0);

                        WebserviceRelation wsr = new WebserviceRelation();
                        WebserviceRelationId wsrId = new WebserviceRelationId();
                        wsrId.setWebserviceInstanceId(edmobj.getInstanceId());
                        wsrId.setEntityInstanceId(relatedWs.getInstanceId());
                        wsr.setResourceEntity(le.getEntityType() != null ? le.getEntityType() : EntityNames.WEBSERVICE.name());
                        wsr.setId(wsrId);

                        wsr.setWebserviceInstance(edmobj);

                        getDbaccess().updateObject(wsr);
                    } else {
                        System.out.println("WARNING: Could not find related Webservice for relation: " + le.getUid());
                    }
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyWebserviceRelations(oldInstanceId, edmobj);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.WEBSERVICE.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void copyProviderFromPreviousVersion(String oldInstanceId, Webservice newEdmobj) {
        List<Webservice> oldList = getDbaccess().getOneFromDBByInstanceId(oldInstanceId, Webservice.class);
        if (oldList != null && !oldList.isEmpty()) {
            Webservice oldWs = oldList.get(0);
            if (oldWs.getProvider() != null) {
                newEdmobj.setProvider(oldWs.getProvider());
            }
        }
    }

    private void copyWebserviceCategoryRelations(String oldInstanceId, Webservice newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("webserviceInstance", oldInstanceId, WebserviceCategory.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            WebserviceCategory oldRel = (WebserviceCategory) obj;
            WebserviceCategory newRel = new WebserviceCategory();
            newRel.setWebserviceInstance(newEdmobj);
            newRel.setCategoryInstance(oldRel.getCategoryInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void copyWebserviceContactPointRelations(String oldInstanceId, Webservice newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("webserviceInstance", oldInstanceId, WebserviceContactpoint.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            WebserviceContactpoint oldRel = (WebserviceContactpoint) obj;
            WebserviceContactpoint newRel = new WebserviceContactpoint();
            newRel.setWebserviceInstance(newEdmobj);
            newRel.setContactpointInstance(oldRel.getContactpointInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void copyWebserviceRelations(String oldInstanceId, Webservice newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("webserviceInstance", oldInstanceId, WebserviceRelation.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            WebserviceRelation oldRel = (WebserviceRelation) obj;

            WebserviceRelation newRel = new WebserviceRelation();
            WebserviceRelationId newId = new WebserviceRelationId();
            newId.setWebserviceInstanceId(newEdmobj.getInstanceId());
            newId.setEntityInstanceId(oldRel.getId().getEntityInstanceId());
            newRel.setId(newId);
            newRel.setResourceEntity(oldRel.getResourceEntity());

            newRel.setWebserviceInstance(newEdmobj);

            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value != null;
            }
        } catch (Exception e) {
            // Fallback: assume not explicitly set
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

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("webserviceInstance", instanceId, WebserviceIdentifier.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceTemporal.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceElement.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceSpatial.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceContactpoint.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceDistribution.class);
        deleteRelations("webserviceInstance", instanceId, WebserviceCategory.class);

        for (Object object : getDbaccess().getAllFromDB(WebserviceRelation.class)) {
            WebserviceRelation item = (WebserviceRelation) object;
            if (item.getId().getWebserviceInstanceId().equals(instanceId)) {
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
        List<Object> list = getDbaccess().getJoinEntitiesByParentId(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    private void deleteWebserviceRelations(String instanceId) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("webserviceInstance", instanceId, WebserviceRelation.class);
        if (existingRelations != null) {
            existingRelations.forEach(rel -> EposDataModelDAO.getInstance().deleteObject(rel));
        }
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