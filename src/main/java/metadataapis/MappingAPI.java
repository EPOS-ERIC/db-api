package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
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
        logCreateStart(obj, overrideStatus);
        try {


        // Capture if fields were explicitly set BEFORE any processing
        boolean paramValueExplicitlySet = isFieldExplicitlySet(obj, "paramValue");

        String searchInstanceId = obj.getInstanceId();

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

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

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

            List<String> paramValues = deduplicateParamValues(obj.getParamValue());

            // PARAM VALUE (Elements)
            syncParamValues(edmobj, paramValues, isNewVersion, oldInstanceId, overrideStatus);

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.MAPPING.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
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
        List<MappingElement> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("mappingInstance", oldInstanceId, MappingElement.class);
        if (oldRelations == null) return;

        Set<String> copiedValues = new HashSet<>();
        for (Object obj : oldRelations) {
            MappingElement oldRelation = (MappingElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name()) && copiedValues.add(oldElement.getValue())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Mapping edmobj, StatusType overrideStatus) {
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        Versioningstatus version = edmobj.getVersion();
        if (version != null) {
            if (version.getEditorId() != null)
                element.setEditorId(version.getEditorId());
            if (version.getProvenance() != null)
                element.setFileProvenance(version.getProvenance());
            if (version.getChangeComment() != null)
                element.setChangeComment(version.getChangeComment());
            if (version.getChangeTimestamp() != null)
                element.setChangeTimestamp(version.getChangeTimestamp().toLocalDateTime());
        }

        LinkedEntity le = new commonapis.ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            MappingElement ce = new MappingElement();
            ce.setMappingInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    private void syncParamValues(Mapping edmobj, List<String> values, boolean isNewVersion, String oldInstanceId, StatusType overrideStatus) {
        if (isNewVersion && oldInstanceId != null && (values == null || values.isEmpty())) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.PARAMVALUE, overrideStatus);
            return;
        }

        deleteExistingElements(edmobj.getInstanceId());
        if (values != null && !values.isEmpty()) {
            Set<String> existingValues = loadExistingParamValueSet(edmobj.getInstanceId());
            for (String value : values) {
                if (existingValues.add(value)) {
                    createInnerElement(ElementType.PARAMVALUE, value, edmobj, overrideStatus);
                }
            }
        }
    }

    private List<String> deduplicateParamValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return new ArrayList<>(values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private Set<String> loadExistingParamValueSet(String mappingInstanceId) {
        Set<String> existingValues = new HashSet<>();
        List<Object> existingRaw = getDbaccess().getOneFromDBBySpecificKey("mappingInstance", mappingInstanceId, MappingElement.class);
        if (existingRaw == null) {
            return existingValues;
        }

        for (Object o : existingRaw) {
            MappingElement me = (MappingElement) o;
            if (me.getElementInstance() != null && ElementType.PARAMVALUE.name().equals(me.getElementInstance().getType())) {
                existingValues.add(me.getElementInstance().getValue());
            }
        }
        return existingValues;
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
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    private List<org.epos.eposdatamodel.Mapping> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Map<String, Mapping> mappings = getDbaccess().batchFetchByInstanceIds(instanceIds, Mapping.class);
        if (mappings.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        List<String> foundIds = new java.util.ArrayList<>(mappings.keySet());
        
        java.util.Map<String, List<MappingElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("mappingInstance", foundIds, MappingElement.class);
        
        java.util.Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = mappings.values().stream()
                .map(Mapping::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        java.util.Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        List<org.epos.eposdatamodel.Mapping> results = new java.util.ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Mapping edmobj = mappings.get(instanceId);
            if (edmobj != null) {
                results.add(assembleMapping(instanceId, edmobj, elements, versioningMap, groupsMap));
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.Mapping assembleMapping(
            String instanceId, Mapping edmobj,
            java.util.Map<String, List<MappingElement>> elements,
            java.util.Map<String, Versioningstatus> versioningMap,
            java.util.Map<String, List<String>> groupsMap) {
        
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
        
        for (MappingElement item : elements.getOrDefault(instanceId, java.util.Collections.emptyList())) {
            Element el = item.getElementInstance();
            if (el != null && ElementType.PARAMVALUE.name().equals(el.getType())) {
                o.addParamValue(el.getValue());
            }
        }
        
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try { o.setStatus(StatusType.valueOf(vs.getStatus())); } catch (Exception e) {}
            }
            o.setFileProvenance(vs.getProvenance());
        }
        
        // Apply groups from pre-fetched data
        if (o.getMetaId() != null && groupsMap != null) {
            List<String> groups = groupsMap.get(o.getMetaId());
            o.setGroups(groups != null ? groups : java.util.Collections.emptyList());
        }
        
        return o;
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
