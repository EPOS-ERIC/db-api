package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrganizationAPI extends AbstractAPI<org.epos.eposdatamodel.Organization> {

    public OrganizationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Organization obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

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

        if (!returnList.isEmpty()) {
            Organization selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Organization item : returnList) {
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

        obj = (org.epos.eposdatamodel.Organization) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Organization edmobj = new Organization();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setLogo(obj.getLogo());
        edmobj.setType(obj.getType());
        edmobj.setAcronym(obj.getAcronym());
        edmobj.setLegalname(obj.getLegalName() != null ? String.join("\\|", obj.getLegalName()) : null);
        edmobj.setLeicode(obj.getLeiCode());
        edmobj.setUrl(obj.getURL());
        edmobj.setMaturity(obj.getMaturity());

        // ADDRESS (Single)
        if (obj.getAddress() != null) {
            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
            List<Address> addressList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Address.class);
            if (!addressList.isEmpty()) {
                edmobj.setAddress(addressList.get(0));
            }
        }

        // CONTACTPOINT
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), relationFromUpdate, relationToUpdate,
                    OrganizationContactpoint.class, Contactpoint.class,
                    "organizationInstance", OrganizationContactpoint::getContactpointInstance, OrganizationContactpoint::setOrganizationInstance, OrganizationContactpoint::setContactpointInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // IDENTIFIER
        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    OrganizationIdentifier.class, Identifier.class,
                    "organizationInstance", OrganizationIdentifier::getIdentifierInstance, OrganizationIdentifier::setOrganizationInstance, OrganizationIdentifier::setIdentifierInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // MEMBER OF (Organization -> Organization)
        if (obj.getMemberOf() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getMemberOf(), relationFromUpdate, relationToUpdate,
                    OrganizationMemberof.class, Organization.class,
                    "organization2Instance", OrganizationMemberof::getOrganization1Instance, OrganizationMemberof::setOrganization2Instance, OrganizationMemberof::setOrganization1Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // OWNS (Polymorphic: Facility/Equipment) - Manual optimization
        if (obj.getOwns() != null) {
            // ... (Custom logic for polymorphic Keep/Add/Delete) ...
            // Simplified for now: existing logic checks LinkedEntityAPI which creates entities.
            // Ideally should check RelationChecker. For now, keeping as is but optimized could involve manual diff.
            // Given complexity of polymorphism, standardizing this is harder.
            // Keeping existing loop but ensuring clean start isn't easy without fetch.
            // Optimized deletion is handled in delete(). Here we append.
            // To make it idempotent manually:
            List<Object> existingOwns = getDbaccess().getOneFromDBBySpecificKey("organization", edmobj.getInstanceId(), OrganizationOwn.class);
            Set<String> processed = new HashSet<>();
            for(LinkedEntity owns : obj.getOwns()) {
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if(le != null && le.getInstanceId() != null){
                    processed.add(le.getInstanceId());
                    // Check existence
                    boolean exists = false;
                    if(existingOwns != null) {
                        for(Object o : existingOwns) {
                            if(((OrganizationOwn)o).getEntityInstanceId().equals(le.getInstanceId())) exists = true;
                        }
                    }
                    if(!exists) {
                        OrganizationOwn pi = new OrganizationOwn();
                        pi.setOrganization(edmobj);
                        pi.setOrganizationInstanceId(edmobj.getInstanceId());
                        pi.setResourceEntity(le.getEntityType());
                        pi.setEntityInstanceId(le.getInstanceId());
                        EposDataModelDAO.getInstance().updateObject(pi);
                    }
                }
            }
            // Delete orphans
            if(existingOwns != null) {
                for(Object o : existingOwns) {
                    OrganizationOwn item = (OrganizationOwn) o;
                    if(!processed.contains(item.getEntityInstanceId())) EposDataModelDAO.getInstance().deleteObject(item);
                }
            }
        }

        // Elements
        if (obj.getTelephone() != null) {
            for (String tel : obj.getTelephone()) createInnerElement(ElementType.TELEPHONE, tel, edmobj, overrideStatus);
        }
        if (obj.getEmail() != null) {
            for (String email : obj.getEmail()) createInnerElement(ElementType.EMAIL, email, edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createInnerElement(ElementType elementType, String value, Organization edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                OrganizationElement relation = (OrganizationElement) obj;
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

        if(!el.isEmpty()) {
            OrganizationElement ce = new OrganizationElement();
            ce.setOrganizationInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("organizationInstance", instanceId, OrganizationContactpoint.class);
        deleteRelations("organization", instanceId, OrganizationOwn.class);
        deleteRelations("organizationInstance", instanceId, DataproductPublisher.class);
        deleteRelations("organization1Instance", instanceId, OrganizationMemberof.class);
        deleteRelations("organizationInstance", instanceId, OrganizationIdentifier.class);
        deleteRelations("organizationInstance", instanceId, OrganizationElement.class);
        deleteRelations("organizationInstance", instanceId, OrganizationAffiliation.class);

        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        for (Organization object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Organization retrieve(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList == null || elementList.isEmpty()) return null;

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

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationIdentifier.class)) {
            OrganizationIdentifier item = (OrganizationIdentifier) object;
            LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
            o.addIdentifier(le);
        }

        if (edmobj.getAddress() != null) {
            o.setAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(edmobj.getAddress().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationContactpoint.class)) {
            OrganizationContactpoint item = (OrganizationContactpoint) object;
            LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
            o.addContactPoint(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationElement.class)) {
            OrganizationElement item = (OrganizationElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
            if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
        }

        if (edmobj.getLegalname() != null && !edmobj.getLegalname().isBlank())
            for (String item : edmobj.getLegalname().split("\\|"))
                o.addLegalName(item);

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organization", edmobj.getInstanceId(), OrganizationOwn.class)) {
            OrganizationOwn item = (OrganizationOwn) object;
            if (item.getResourceEntity().equals(EntityNames.FACILITY.name())) {
                o.addOwns(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if (item.getResourceEntity().equals(EntityNames.EQUIPMENT.name())) {
                o.addOwns(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organization2Instance", edmobj.getInstanceId(), OrganizationMemberof.class)) {
            OrganizationMemberof item = (OrganizationMemberof) object;
            LinkedEntity le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganization1Instance().getInstanceId());
            o.addMemberOf(le);
        }

        o = (org.epos.eposdatamodel.Organization) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Organization retrieveByUID(String uid) {
        List<Organization> returnList = getDbaccess().getOneFromDBByUID(uid, Organization.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
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
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList != null && !elementList.isEmpty()) {
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