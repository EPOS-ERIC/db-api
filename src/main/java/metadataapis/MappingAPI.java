package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingAPI extends AbstractAPI<org.epos.eposdatamodel.Mapping> {

    public MappingAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Mapping obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Capture if fields were explicitly set BEFORE any processing
        boolean paramValueExplicitlySet = isFieldExplicitlySet(obj, "paramValue");

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Mapping> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Mapping selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Mapping item : returnList) {
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
        }

        obj = (org.epos.eposdatamodel.Mapping) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = obj.getInstanceChangedId() != null;
        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());

        Mapping edmobj = new Mapping();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setLabel(obj.getLabel());
        edmobj.setValuepattern(obj.getValuePattern());
        edmobj.setDefaultvalue(obj.getDefaultValue());
        edmobj.setMaxvalue(obj.getMaxValue());
        edmobj.setMinvalue(obj.getMinValue());
        edmobj.setMultipleValues(obj.getMultipleValues() != null ? obj.getMultipleValues() : "false");
        edmobj.setReadOnlyValue(obj.getReadOnlyValue() != null ? obj.getReadOnlyValue() : "false");
        edmobj.setRequired(Boolean.parseBoolean(obj.getRequired()));
        edmobj.setRange(obj.getRange());
        edmobj.setProperty(obj.getProperty());
        edmobj.setVariable(obj.getVariable());
        edmobj.setHealthcheckvalue(obj.getHealthCheckVariable());

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        // PARAM VALUE (Elements)
        if (paramValueExplicitlySet || !isNewVersion) {
            if (obj.getParamValue() != null && !obj.getParamValue().isEmpty()) {
                List<Object> existingRaw = getDbaccess().getOneFromDBBySpecificKey("mappingInstance", edmobj.getInstanceId(), MappingElement.class);
                if (existingRaw != null) {
                    for (Object o : existingRaw) {
                        MappingElement me = (MappingElement) o;
                        if (me.getElementInstance() != null &&
                                ElementType.PARAMVALUE.name().equals(me.getElementInstance().getType()) &&
                                !obj.getParamValue().contains(me.getElementInstance().getValue())) {
                            EposDataModelDAO.getInstance().deleteObject(me);
                        }
                    }
                }
                for (String paramvalue : obj.getParamValue()) {
                    createInnerElement(ElementType.PARAMVALUE, paramvalue, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.PARAMVALUE, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.MAPPING.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void deleteExistingElements(String mappingInstanceId) {
        List<MappingElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("mappingInstance", mappingInstanceId, MappingElement.class);

        if (existingRelations != null) {
            for (MappingElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Mapping newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("mappingInstance", oldInstanceId, MappingElement.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            MappingElement oldRelation = (MappingElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Mapping edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("mappingInstance", edmobj.getInstanceId(), MappingElement.class);

        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                MappingElement relation = (MappingElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null &&
                        existingElement.getType().equals(elementType.name()) &&
                        existingElement.getValue().equals(value)) {
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
            MappingElement ce = new MappingElement();
            ce.setMappingInstance(edmobj);
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
        deleteRelations("mappingInstance", instanceId, MappingElement.class);
        deleteRelations("mappingInstance", instanceId, OperationMapping.class);

        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        for (Mapping object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Mapping retrieve(String instanceId) {
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Mapping edmobj = elementList.get(0);
        org.epos.eposdatamodel.Mapping o = new org.epos.eposdatamodel.Mapping();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setLabel(edmobj.getLabel());
        o.setValuePattern(edmobj.getValuepattern());
        o.setDefaultValue(edmobj.getDefaultvalue());
        o.setMaxValue(edmobj.getMaxvalue());
        o.setMinValue(edmobj.getMinvalue());
        o.setMultipleValues(edmobj.getMultipleValues());
        o.setReadOnlyValue(edmobj.getReadOnlyValue());
        o.setRequired(edmobj.getRequired() != null ? Boolean.toString(edmobj.getRequired()) : null);
        o.setRange(edmobj.getRange());
        o.setProperty(edmobj.getProperty());
        o.setVariable(edmobj.getVariable());
        o.setHealthCheckVariable(edmobj.getHealthcheckvalue());

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("mappingInstance", edmobj.getInstanceId(), MappingElement.class)) {
            MappingElement item = (MappingElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.PARAMVALUE.name())) {
                o.addParamValue(el.getValue());
            }
        }

        o = (org.epos.eposdatamodel.Mapping) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Mapping retrieveByUID(String uid) {
        List<Mapping> returnList = getDbaccess().getOneFromDBByUID(uid, Mapping.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Mapping.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Mapping.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Mapping.class, status));
    }

    private List<org.epos.eposdatamodel.Mapping> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        if (elementList != null && !elementList.isEmpty()) {
            Mapping edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.MAPPING.name());
            return o;
        }
        return null;
    }
}