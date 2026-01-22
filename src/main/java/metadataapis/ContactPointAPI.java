package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.ElementAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContactPointAPI extends AbstractAPI<ContactPoint> {

    public ContactPointAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(ContactPoint obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Contactpoint> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Contactpoint selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Contactpoint item : returnList) {
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

        obj = (org.epos.eposdatamodel.ContactPoint) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Contactpoint edmobj = new Contactpoint();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setRole(obj.getRole());

        // LANGUAGE, TELEPHONE, EMAIL (Optimized creation)
        if(obj.getLanguage()!=null) {
            for(String lang : obj.getLanguage()) createInnerElement(ElementType.LANGUAGE, lang, edmobj, overrideStatus);
        }
        if(obj.getTelephone()!=null) {
            for(String tel : obj.getTelephone()) createInnerElement(ElementType.TELEPHONE, tel, edmobj, overrideStatus);
        }
        if(obj.getEmail()!=null) {
            for(String email : obj.getEmail()) createInnerElement(ElementType.EMAIL, email, edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createInnerElement(ElementType elementType, String value, Contactpoint edmobj, StatusType overrideStatus){

        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(), ContactpointElement.class);

        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                ContactpointElement relation = (ContactpointElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null &&
                        existingElement.getType().equals(elementType.name()) &&
                        existingElement.getValue().equals(value)) {
                    return; // Already exists
                }
            }
        }

        // Create new
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            ContactpointElement ce = new ContactpointElement();
            ce.setContactpointInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
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

        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        for(Contactpoint object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if(list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
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

        for(Object contactpointElement : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),ContactpointElement.class)){
            ContactpointElement ce = (ContactpointElement) contactpointElement;
            Element el = ce.getElementInstance();
            if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
            if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
            if (el.getType().equals(ElementType.LANGUAGE.name())) o.addLanguage(el.getValue());
        }

        for(Object organizationContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),OrganizationContactpoint.class)){
            OrganizationContactpoint item = (OrganizationContactpoint) organizationContactpoint;
            o.setOrganization(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId()));
        }

        for(Object personContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),PersonContactpoint.class)){
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
        if(elementList!=null && !elementList.isEmpty()) {
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