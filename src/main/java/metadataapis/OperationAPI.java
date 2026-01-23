package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.IriTemplate;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationAPI extends AbstractAPI<org.epos.eposdatamodel.Operation> {

    public OperationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Operation obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Capture if fields were explicitly set BEFORE any processing
        boolean mappingExplicitlySet = isFieldExplicitlySet(obj, "mapping");
        boolean webserviceExplicitlySet = isFieldExplicitlySet(obj, "webservice");
        boolean payloadExplicitlySet = isFieldExplicitlySet(obj, "payload");
        boolean returnsExplicitlySet = isFieldExplicitlySet(obj, "returns");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Operation> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Operation selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Operation item : returnList) {
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

        obj = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = obj.getInstanceChangedId() != null;
        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());

        Operation edmobj = new Operation();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setMethod(obj.getMethod());
        edmobj.setTemplate(obj.getTemplate());

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        // MAPPING
        if (mappingExplicitlySet || !isNewVersion) {
            if (obj.getMapping() != null && !obj.getMapping().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getMapping(), relationFromUpdate, relationToUpdate,
                        OperationMapping.class, Mapping.class,
                        "operationInstance", OperationMapping::getMappingInstance, OperationMapping::setOperationInstance, OperationMapping::setMappingInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OperationMapping.class, Mapping.class,
                    "operationInstance", OperationMapping::getMappingInstance, OperationMapping::setOperationInstance, OperationMapping::setMappingInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // WEBSERVICE
        if (webserviceExplicitlySet || !isNewVersion) {
            if (obj.getWebservice() != null && !obj.getWebservice().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getWebservice(), relationFromUpdate, relationToUpdate,
                        OperationWebservice.class, Webservice.class,
                        "operationInstance", OperationWebservice::getWebserviceInstance, OperationWebservice::setOperationInstance, OperationWebservice::setWebserviceInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OperationWebservice.class, Webservice.class,
                    "operationInstance", OperationWebservice::getWebserviceInstance, OperationWebservice::setOperationInstance, OperationWebservice::setWebserviceInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // PAYLOAD
        if (payloadExplicitlySet || !isNewVersion) {
            if (obj.getPayload() != null && !obj.getPayload().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getPayload(), relationFromUpdate, relationToUpdate,
                        OperationPayload.class, Payload.class,
                        "operationInstance", OperationPayload::getPayloadInstance, OperationPayload::setOperationInstance, OperationPayload::setPayloadInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OperationPayload.class, Payload.class,
                    "operationInstance", OperationPayload::getPayloadInstance, OperationPayload::setOperationInstance, OperationPayload::setPayloadInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // RETURNS (Elements)
        if (returnsExplicitlySet || !isNewVersion) {
            if (obj.getReturns() != null && !obj.getReturns().isEmpty()) {
                for (String returns : obj.getReturns()) {
                    createInnerElement(ElementType.RETURNS, returns, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.RETURNS, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.OPERATION.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void deleteExistingElements(String operationInstanceId) {
        List<OperationElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("operationInstance", operationInstanceId, OperationElement.class);

        if (existingRelations != null) {
            for (OperationElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                // Also delete the Element entity
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Operation newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("operationInstance", oldInstanceId, OperationElement.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            OperationElement oldRelation = (OperationElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Operation edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                OperationElement relation = (OperationElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null && existingElement.getType().equals(elementType.name()) && existingElement.getValue().equals(value)) {
                    return;
                }
            }
        }

        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        if (edmobj.getVersion().getEditorId() != null)
            element.setEditorId(edmobj.getVersion().getEditorId());
        if (edmobj.getVersion().getProvenance() != null)
            element.setFileProvenance(edmobj.getVersion().getProvenance());
        if (edmobj.getVersion().getChangeComment() != null)
            element.setChangeComment(edmobj.getVersion().getChangeComment());
        if (edmobj.getVersion().getChangeTimestamp() != null)
            element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new commonapis.ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            OperationElement ce = new OperationElement();
            ce.setOperationInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
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
        deleteRelations("operationInstance", instanceId, OperationElement.class);
        deleteRelations("operationInstance", instanceId, OperationMapping.class);
        deleteRelations("operationInstance", instanceId, OperationDistribution.class);
        deleteRelations("operationInstance", instanceId, OperationWebservice.class);
        deleteRelations("operationInstance", instanceId, OperationPayload.class);
        deleteRelations("operationInstance", instanceId, SoftwareapplicationOperation.class);

        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        for (Operation object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Operation retrieve(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Operation edmobj = elementList.get(0);
        org.epos.eposdatamodel.Operation o = new org.epos.eposdatamodel.Operation();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setMethod(edmobj.getMethod());
        o.setTemplate(edmobj.getTemplate());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationWebservice.class)) {
            OperationWebservice item = (OperationWebservice) object;
            LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
            o.addWebservice(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationPayload.class)) {
            OperationPayload item = (OperationPayload) object;
            LinkedEntity le = retrieveAPI(EntityNames.PAYLOAD.name()).retrieveLinkedEntity(item.getPayloadInstance().getInstanceId());
            o.addPayload(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationMapping.class)) {
            OperationMapping item = (OperationMapping) object;
            LinkedEntity le = retrieveAPI(EntityNames.MAPPING.name()).retrieveLinkedEntity(item.getMappingInstance().getInstanceId());
            o.addMapping(le);
        }

        IriTemplate iriTemplateObject = new IriTemplate();
        iriTemplateObject.setMappings(o.getMapping());
        iriTemplateObject.setTemplate(o.getTemplate());
        o.setIriTemplateObject(iriTemplateObject);

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationElement.class)) {
            OperationElement item = (OperationElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.RETURNS.name())) o.addReturns(el.getValue());
        }

        o = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Operation retrieveByUID(String uid) {
        List<Operation> returnList = getDbaccess().getOneFromDBByUID(uid, Operation.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Operation.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Operation.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Operation.class, status));
    }

    private List<org.epos.eposdatamodel.Operation> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if (elementList != null && !elementList.isEmpty()) {
            Operation edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.OPERATION.name());
            return o;
        }
        return null;
    }
}