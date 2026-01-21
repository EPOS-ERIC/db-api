package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.ElementAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

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
                if (item.getVersion() != null &&
                        targetStatus.toString().equals(item.getVersion().getStatus())) {
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


        /* LANGUAGE */
        if(obj.getLanguage()!=null){
            for(String lang : obj.getLanguage()) {
                createInnerElement(ElementType.LANGUAGE, lang, edmobj, overrideStatus);
            }
        }

        /* TELEPHONE */
        if(obj.getTelephone()!=null){
            for(String tel : obj.getTelephone()) {
                createInnerElement(ElementType.TELEPHONE, tel, edmobj, overrideStatus);
            }
        }

        /* EMAIL */
        if(obj.getEmail()!=null){
            for(String email : obj.getEmail()) {
                createInnerElement(ElementType.EMAIL, email, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Contactpoint edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        ElementAPI api = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);
        LinkedEntity le = api.create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        ContactpointElement ce = new ContactpointElement();
        ce.setContactpointInstance(edmobj);
        ce.setElementInstance(el.get(0));

        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(ContactpointElement.class)){
            ContactpointElement item = (ContactpointElement) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceContactpoint.class)){
            WebserviceContactpoint item = (WebserviceContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductContactpoint.class)){
            DataproductContactpoint item = (DataproductContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentContactpoint.class)){
            EquipmentContactpoint item = (EquipmentContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityContactpoint.class)){
            FacilityContactpoint item = (FacilityContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeContactpoint.class)){
            SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationContactpoint.class)){
            SoftwareapplicationContactpoint item = (SoftwareapplicationContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(ServiceContactpoint.class)){
            ServiceContactpoint item = (ServiceContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(PersonContactpoint.class)){
            PersonContactpoint item = (PersonContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(OrganizationContactpoint.class)){
            OrganizationContactpoint item = (OrganizationContactpoint) object;
            if(item.getContactpointInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        for(Contactpoint object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }


    @Override
    public org.epos.eposdatamodel.ContactPoint retrieve(String instanceId) {
        List<Contactpoint> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Contactpoint.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Contactpoint edmobj = elementList.get(0);
            org.epos.eposdatamodel.ContactPoint o = new org.epos.eposdatamodel.ContactPoint();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setRole(edmobj.getRole());

            for(Object contactpointElement : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),ContactpointElement.class)){
                ContactpointElement ce = (ContactpointElement) contactpointElement;
                //if(ce.getContactpointInstance().getInstanceId().equals(edmobj.getInstanceId())){
                    Element el = ce.getElementInstance();
                    if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
                    if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
                    if (el.getType().equals(ElementType.LANGUAGE.name())) o.addLanguage(el.getValue());
                //}
            }

            for(Object organizationContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),OrganizationContactpoint.class)){
                OrganizationContactpoint item = (OrganizationContactpoint) organizationContactpoint;
                //if(item.getContactpointInstance().getInstanceId().equals(edmobj.getInstanceId())){
                    o.setOrganization(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId()));
                //}
            }

            for(Object personContactpoint : getDbaccess().getOneFromDBBySpecificKey("contactpointInstance", edmobj.getInstanceId(),PersonContactpoint.class)){
                PersonContactpoint item = (PersonContactpoint) personContactpoint;
                //if(item.getContactpointInstance().getInstanceId().equals(edmobj.getInstanceId())){
                    o.setPerson(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getPersonInstance().getInstanceId()));
                //}
            }

            o = (org.epos.eposdatamodel.ContactPoint) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }

    @Override
    public org.epos.eposdatamodel.ContactPoint retrieveByUID(String uid) {
        List<Contactpoint> returnList = getDbaccess().getOneFromDBByUID(uid, Contactpoint.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
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
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
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
