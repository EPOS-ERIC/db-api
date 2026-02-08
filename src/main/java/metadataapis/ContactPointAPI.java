package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.ElementAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContactPointAPI extends AbstractAPI<ContactPoint> {

    public ContactPointAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(ContactPoint obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        boolean languageExplicitlySet = isFieldExplicitlySet(obj, "language");
        boolean telephoneExplicitlySet = isFieldExplicitlySet(obj, "telephone");
        boolean emailExplicitlySet = isFieldExplicitlySet(obj, "email");

        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<Contactpoint> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Contactpoint selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Contactpoint item : returnList) {
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

        obj = (org.epos.eposdatamodel.ContactPoint) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Contactpoint edmobj = new Contactpoint();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setRole(obj.getRole());

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        if (languageExplicitlySet || !isNewVersion) {
            if (obj.getLanguage() != null && !obj.getLanguage().isEmpty()) {
                for (String lang : obj.getLanguage()) {
                    createInnerElement(ElementType.LANGUAGE, lang, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.LANGUAGE, overrideStatus);
        }

        if (telephoneExplicitlySet || !isNewVersion) {
            if (obj.getTelephone() != null && !obj.getTelephone().isEmpty()) {
                for (String tel : obj.getTelephone()) {
                    createInnerElement(ElementType.TELEPHONE, tel, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.TELEPHONE, overrideStatus);
        }

        if (emailExplicitlySet || !isNewVersion) {
            if (obj.getEmail() != null && !obj.getEmail().isEmpty()) {
                for (String email : obj.getEmail()) {
                    createInnerElement(ElementType.EMAIL, email, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.EMAIL, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CONTACTPOINT.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void deleteExistingElements(String contactpointInstanceId) {
        List<ContactpointElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("contactpointInstance", contactpointInstanceId, ContactpointElement.class);

        if (existingRelations != null) {
            for (ContactpointElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Contactpoint newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<ContactpointElement> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("contactpointInstance", oldInstanceId, ContactpointElement.class);

        if (oldRelations == null) return;

        for (ContactpointElement oldRelation : oldRelations) {
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType type, String value, Contactpoint edmobj, StatusType overrideStatus) {
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(type);
        element.setValue(value);
        if(edmobj.getVersion() != null) {
            if (edmobj.getVersion().getEditorId() != null) element.setEditorId(edmobj.getVersion().getEditorId());
            if (edmobj.getVersion().getProvenance() != null) element.setFileProvenance(edmobj.getVersion().getProvenance());
            if (edmobj.getVersion().getChangeComment() != null) element.setChangeComment(edmobj.getVersion().getChangeComment());
            if (edmobj.getVersion().getChangeTimestamp() != null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());
        }
        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        if (!el.isEmpty()) {
            ContactpointElement ce = new ContactpointElement();
            ce.setContactpointInstance(edmobj);
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
        deleteRelations("contactpointInstance", instanceId, ContactpointElement.class);
        deleteRelations("contactpointInstance", instanceId, WebserviceContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, DataproductContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, EquipmentContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, FacilityContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, SoftwaresourcecodeContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, SoftwareapplicationContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, ServiceContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, PersonContactpoint.class);
        deleteRelations("contactpointInstance", instanceId, OrganizationContactpoint.class);
        deleteElementRelations(instanceId);

        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        for (Contactpoint object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteElementRelations(String instanceId) {
        List<ContactpointElement> list = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("contactpointInstance", instanceId, ContactpointElement.class);
        if (list != null) {
            for (ContactpointElement ce : list) {
                if (ce.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(ce.getElementInstance());
                }
                EposDataModelDAO.getInstance().deleteObject(ce);
            }
        }
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getJoinEntitiesByParentId(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.ContactPoint retrieve(String instanceId) {
        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Contactpoint edmobj = elementList.get(0);
        org.epos.eposdatamodel.ContactPoint o = new org.epos.eposdatamodel.ContactPoint();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setRole(edmobj.getRole());

        for (Object cee : EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("contactpointInstance", edmobj.getInstanceId(), ContactpointElement.class)) {
            ContactpointElement ce = (ContactpointElement) cee;
            Element el = ce.getElementInstance();
            if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
            if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
            if (el.getType().equals(ElementType.LANGUAGE.name())) o.addLanguage(el.getValue());
        }

        for (Object organizationContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(), OrganizationContactpoint.class)) {
            OrganizationContactpoint item = (OrganizationContactpoint) organizationContactpoint;
            o.setOrganization(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId()));
        }

        for (Object personContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(), PersonContactpoint.class)) {
            PersonContactpoint item = (PersonContactpoint) personContactpoint;
            o.setPerson(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getPersonInstance().getInstanceId()));
        }

        o = (org.epos.eposdatamodel.ContactPoint) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.ContactPoint retrieveByUID(String uid) {
        List<Contactpoint> returnList = getDbaccess().getOneFromDBByUID(uid, Contactpoint.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.ContactPoint> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Contactpoint.class));
    }

    @Override
    public List<org.epos.eposdatamodel.ContactPoint> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Contactpoint.class));
    }

    @Override
    public List<org.epos.eposdatamodel.ContactPoint> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Contactpoint.class, status));
    }

    private List<org.epos.eposdatamodel.ContactPoint> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    private List<org.epos.eposdatamodel.ContactPoint> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Contactpoint> contactpoints = getDbaccess().batchFetchByInstanceIds(instanceIds, Contactpoint.class);
        if (contactpoints.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(contactpoints.keySet());
        
        Map<String, List<ContactpointElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("contactpointInstance", foundIds, ContactpointElement.class);
        Map<String, List<OrganizationContactpoint>> orgContactpoints = 
                getDbaccess().batchFetchRelationsForMultipleParents("contactpointInstance", foundIds, OrganizationContactpoint.class);
        Map<String, List<PersonContactpoint>> personContactpoints = 
                getDbaccess().batchFetchRelationsForMultipleParents("contactpointInstance", foundIds, PersonContactpoint.class);
        
        Set<String> allOrganizationIds = new HashSet<>();
        Set<String> allPersonIds = new HashSet<>();
        
        orgContactpoints.values().forEach(list -> list.forEach(r -> {
            if (r.getOrganizationInstance() != null) allOrganizationIds.add(r.getOrganizationInstance().getInstanceId());
        }));
        personContactpoints.values().forEach(list -> list.forEach(r -> {
            if (r.getPersonInstance() != null) allPersonIds.add(r.getPersonInstance().getInstanceId());
        }));
        
        Map<String, Organization> organizationMap = allOrganizationIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allOrganizationIds), Organization.class);
        Map<String, Person> personMap = allPersonIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allPersonIds), Person.class);
        
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = contactpoints.values().stream()
                .map(Contactpoint::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        List<org.epos.eposdatamodel.ContactPoint> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Contactpoint edmobj = contactpoints.get(instanceId);
            if (edmobj != null) {
                results.add(assembleContactPoint(instanceId, edmobj, elements, orgContactpoints, personContactpoints,
                        organizationMap, personMap, versioningMap, groupsMap));
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.ContactPoint assembleContactPoint(
            String instanceId, Contactpoint edmobj,
            Map<String, List<ContactpointElement>> elements,
            Map<String, List<OrganizationContactpoint>> orgContactpoints,
            Map<String, List<PersonContactpoint>> personContactpoints,
            Map<String, Organization> organizationMap,
            Map<String, Person> personMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.ContactPoint o = new org.epos.eposdatamodel.ContactPoint();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setRole(edmobj.getRole());
        
        for (ContactpointElement item : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = item.getElementInstance();
            if (el != null) {
                if (ElementType.TELEPHONE.name().equals(el.getType())) o.addTelephone(el.getValue());
                if (ElementType.EMAIL.name().equals(el.getType())) o.addEmail(el.getValue());
                if (ElementType.LANGUAGE.name().equals(el.getType())) o.addLanguage(el.getValue());
            }
        }
        
        for (OrganizationContactpoint rel : orgContactpoints.getOrDefault(instanceId, Collections.emptyList())) {
            Organization target = organizationMap.get(rel.getOrganizationInstance().getInstanceId());
            if (target != null) {
                o.setOrganization(createLinkedEntity(target, EntityNames.ORGANIZATION.name()));
            }
        }
        
        for (PersonContactpoint rel : personContactpoints.getOrDefault(instanceId, Collections.emptyList())) {
            Person target = personMap.get(rel.getPersonInstance().getInstanceId());
            if (target != null) {
                o.setPerson(createLinkedEntity(target, EntityNames.PERSON.name()));
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
            o.setGroups(groups != null ? groups : Collections.emptyList());
        }
        
        return o;
    }

    private LinkedEntity createLinkedEntity(Object entity, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(utilities.ReflectionCache.getInstanceId(entity));
        le.setMetaId(utilities.ReflectionCache.getMetaId(entity));
        le.setUid(utilities.ReflectionCache.getUid(entity));
        le.setEntityType(entityType);
        return le;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        if (elementList != null && !elementList.isEmpty()) {
            Contactpoint edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.CONTACTPOINT.name());
            return o;
        }
        return null;
    }
}