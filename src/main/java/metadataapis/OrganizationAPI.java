package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrganizationAPI extends AbstractAPI<org.epos.eposdatamodel.Organization> {

    public OrganizationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Organization obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Organization> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Organization selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Organization item : returnList) {
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

        obj = (org.epos.eposdatamodel.Organization) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Organization edmobj = new Organization();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setLogo(obj.getLogo());
        edmobj.setType(obj.getType());
        edmobj.setAcronym(obj.getAcronym());
        edmobj.setLegalname(obj.getLegalName()!=null?String.join("\\|", obj.getLegalName()):null);
        edmobj.setLeicode(obj.getLeiCode());
        edmobj.setUrl(obj.getURL());
        edmobj.setMaturity(obj.getMaturity());

        /** ADDRESS **/
        if (obj.getAddress() != null) {
            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
            List<Address> addressList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Address.class);
            if(!addressList.isEmpty()) {
                edmobj.setAddress(addressList.get(0));
            }
        }

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null) {
            for(LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                List<Contactpoint> contactpoint = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpoint.isEmpty()) {
                    OrganizationContactpoint pi = new OrganizationContactpoint();
                    pi.setOrganizationInstance(edmobj);
                    pi.setContactpointInstance(contactpoint.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                List<Identifier> identifierList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(),Identifier.class);
                if(!identifierList.isEmpty()) {
                    OrganizationIdentifier pi = new OrganizationIdentifier();
                    pi.setOrganizationInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
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

        /** MEMBER OF **/
        if (obj.getMemberOf() != null) {
            for(LinkedEntity organization : obj.getMemberOf()) {
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(organization, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());

                List<Organization> list2 = getDbaccess().getOneFromDBByInstanceId(le.getInstanceId(), Organization.class);

                OrganizationMemberof pi = new OrganizationMemberof();
                pi.setOrganization2Instance(edmobj);
                pi.setOrganization1Instance(list2.get(0));
                EposDataModelDAO.getInstance().updateObject(pi);
            }
        }

        /** OWNS **/
        if (obj.getOwns() != null) {
            for(LinkedEntity owns : obj.getOwns()) {
                if (owns != null){
                    LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                    OrganizationOwn pi = new OrganizationOwn();
                    pi.setOrganization(edmobj);
                    pi.setOrganizationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(le.getEntityType());
                    pi.setEntityInstanceId(le.getInstanceId());
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

    private void createInnerElement(ElementType elementType, String value, Organization edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        OrganizationElement ce = new OrganizationElement();
        ce.setOrganizationInstance(edmobj);
        ce.setElementInstance(el.get(0));
        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(OrganizationContactpoint.class)){
            OrganizationContactpoint item = (OrganizationContactpoint) object;
            if(item.getOrganizationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationOwn.class)){
            OrganizationOwn item = (OrganizationOwn) object;
            if(item.getOrganization().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductPublisher.class)){
            DataproductPublisher item = (DataproductPublisher) object;
            if(item.getOrganizationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationMemberof.class)){
            OrganizationMemberof item = (OrganizationMemberof) object;
            if(item.getOrganization1Instance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationIdentifier.class)){
            OrganizationIdentifier item = (OrganizationIdentifier) object;
            if(item.getOrganizationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationElement.class)){
            OrganizationElement item = (OrganizationElement) object;
            if(item.getOrganizationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationAffiliation.class)){
            OrganizationAffiliation item = (OrganizationAffiliation) object;
            if(item.getOrganizationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        for(Organization object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Organization retrieve(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Organization edmobj = elementList.get(0);
            org.epos.eposdatamodel.Organization o = new org.epos.eposdatamodel.Organization();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setAcronym(edmobj.getAcronym());
            o.setLeiCode(edmobj.getLeicode());
            o.setLogo(edmobj.getLogo());
            o.setURL(edmobj.getUrl());
            o.setType(edmobj.getType());
            o.setMaturity(edmobj.getMaturity());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(),OrganizationIdentifier.class)) {
                OrganizationIdentifier item = (OrganizationIdentifier) object;
                //if(item.getOrganizationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                //}
            }

            if (edmobj.getAddress() != null) {
                o.setAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(edmobj.getAddress().getInstanceId()));
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(),OrganizationContactpoint.class)) {
                OrganizationContactpoint item = (OrganizationContactpoint) object;
                //if(item.getOrganizationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(),OrganizationElement.class)) {
                OrganizationElement item = (OrganizationElement) object;
                //if(item.getOrganizationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
                    if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
                //}
            }

            if(edmobj.getLegalname()!=null && !edmobj.getLegalname().isBlank())
                for(String item : edmobj.getLegalname().split("\\|"))
                    o.addLegalName(item);


            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("organization", edmobj.getInstanceId(),OrganizationOwn.class)) {
                OrganizationOwn item = (OrganizationOwn) object;
                //if(item.getOrganization().getInstanceId().equals(edmobj.getInstanceId())) {
                    if(item.getResourceEntity().equals(EntityNames.FACILITY.name())){
                        o.addOwns(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                    }
                    if(item.getResourceEntity().equals(EntityNames.EQUIPMENT.name())){
                        o.addOwns(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                    }
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("organization2Instance", edmobj.getInstanceId(),OrganizationMemberof.class)) {
                OrganizationMemberof item = (OrganizationMemberof) object;
                //if(item.getOrganization2Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganization1Instance().getInstanceId());
                    o.addMemberOf(le);
                //}
            }

            o = (org.epos.eposdatamodel.Organization) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }

    @Override
    public org.epos.eposdatamodel.Organization retrieveByUID(String uid) {
        List<Organization> returnList = getDbaccess().getOneFromDBByUID(uid, Organization.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.Organization> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Organization.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Organization> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Organization.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Organization> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Organization.class, status));
    }

    private List<org.epos.eposdatamodel.Organization> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Organization edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.ORGANIZATION.name());

            return o;
        }
        return null;
    }

}
