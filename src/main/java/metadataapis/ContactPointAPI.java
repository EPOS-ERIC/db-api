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

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

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
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
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