package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrganizationAPI extends AbstractAPI<org.epos.eposdatamodel.Organization> {

    private static final Logger LOG = Logger.getLogger(OrganizationAPI.class.getName());

    public OrganizationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Organization obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        boolean addressExplicitlySet = isFieldExplicitlySet(obj, "address");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");
        boolean identifierExplicitlySet = isFieldExplicitlySet(obj, "identifier");
        boolean memberOfExplicitlySet = isFieldExplicitlySet(obj, "memberOf");
        boolean ownsExplicitlySet = isFieldExplicitlySet(obj, "owns");
        boolean telephoneExplicitlySet = isFieldExplicitlySet(obj, "telephone");
        boolean emailExplicitlySet = isFieldExplicitlySet(obj, "email");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();

        List<Organization> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Organization selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Organization item : returnList) {
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
            if (previousObj == null) previousObj = retrieve(selectedEntity.getInstanceId());
        }

        obj = (org.epos.eposdatamodel.Organization) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = obj.getInstanceChangedId() != null;
        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());

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

        if (isUpdate && !isNewVersion) deleteExistingElements(oldInstanceId);

        // ADDRESS (Single)
        if (addressExplicitlySet || !isNewVersion) {
            if (obj.getAddress() != null) {
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (le != null && le.getInstanceId() != null) {
                    List<Address> addressList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Address.class);
                    if (!addressList.isEmpty()) edmobj.setAddress(addressList.get(0));
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyAddressFromPreviousVersion(oldInstanceId, edmobj);
        }

        // CONTACTPOINT
        if (contactPointExplicitlySet || !isNewVersion) {
            if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getContactPoint(), relationFromUpdate, relationToUpdate,
                        OrganizationContactpoint.class, Contactpoint.class, "organizationInstance",
                        OrganizationContactpoint::getContactpointInstance, OrganizationContactpoint::setOrganizationInstance,
                        OrganizationContactpoint::setContactpointInstance, obj, previousObj, overrideStatus, true);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OrganizationContactpoint.class, Contactpoint.class, "organizationInstance",
                    OrganizationContactpoint::getContactpointInstance, OrganizationContactpoint::setOrganizationInstance,
                    OrganizationContactpoint::setContactpointInstance, obj, previousObj, overrideStatus, true);
        }

        // IDENTIFIER
        if (identifierExplicitlySet || !isNewVersion) {
            if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                        OrganizationIdentifier.class, Identifier.class, "organizationInstance",
                        OrganizationIdentifier::getIdentifierInstance, OrganizationIdentifier::setOrganizationInstance,
                        OrganizationIdentifier::setIdentifierInstance, obj, previousObj, overrideStatus, false);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OrganizationIdentifier.class, Identifier.class, "organizationInstance",
                    OrganizationIdentifier::getIdentifierInstance, OrganizationIdentifier::setOrganizationInstance,
                    OrganizationIdentifier::setIdentifierInstance, obj, previousObj, overrideStatus, false);
        }

        // MEMBER OF
        if (memberOfExplicitlySet || !isNewVersion) {
            if (obj.getMemberOf() != null && !obj.getMemberOf().isEmpty()) {
                for (LinkedEntity rel : obj.getMemberOf()) {
                    LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(rel, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                    if (le != null && le.getInstanceId() != null) {
                        List<Organization> orgList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Organization.class);
                        if (!orgList.isEmpty()) {
                            OrganizationMemberof om = new OrganizationMemberof();
                            om.setOrganization1Instance(orgList.get(0));
                            om.setOrganization2Instance(edmobj);
                            EposDataModelDAO.getInstance().updateObject(om);
                        }
                    }
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyMemberOfFromPreviousVersion(oldInstanceId, edmobj);
        }

        // OWNS (polymorphic - can be Facility or Equipment)
        if (ownsExplicitlySet || !isNewVersion) {
            if (obj.getOwns() != null && !obj.getOwns().isEmpty()) {
                for (LinkedEntity rel : obj.getOwns()) {
                    LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(rel, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                    if (le != null && le.getInstanceId() != null && le.getEntityType() != null) {
                        OrganizationOwn oo = new OrganizationOwn();
                        oo.setOrganization(edmobj);
                        oo.setOrganizationInstanceId(edmobj.getInstanceId());
                        oo.setEntityInstanceId(le.getInstanceId());
                        oo.setResourceEntity(le.getEntityType());
                        EposDataModelDAO.getInstance().updateObject(oo);
                    }
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyOwnsFromPreviousVersion(oldInstanceId, edmobj);
        }

        // TELEPHONE (elements)
        if (telephoneExplicitlySet || !isNewVersion) {
            if (obj.getTelephone() != null && !obj.getTelephone().isEmpty()) {
                for (String tel : obj.getTelephone()) {
                    createInnerElement(ElementType.TELEPHONE, tel, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.TELEPHONE, overrideStatus);
        }

        // EMAIL (elements)
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

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.ORGANIZATION.name(), edmobj);
        AttributionAPI.resolvePendingAgentRelations(edmobj.getUid(), edmobj.getInstanceId());

        return new LinkedEntity().instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid())
                .entityType(EntityNames.ORGANIZATION.name());
    }

    private void deleteExistingElements(String instanceId) {
        List<Object> elements = getDbaccess().getOneFromDBBySpecificKey("organizationInstance", instanceId, OrganizationElement.class);
        if (elements != null) {
            for (Object obj : elements) {
                OrganizationElement oe = (OrganizationElement) obj;

                EposDataModelDAO.getInstance().deleteObject(oe);

                if (oe.getElementInstance() != null) {
                    try {
                        EposDataModelDAO.getInstance().deleteObject(oe.getElementInstance());
                    } catch (Exception e) {
                        // Ignore if element is used elsewhere
                    }
                }
            }
        }
    }

    private void copyAddressFromPreviousVersion(String oldInstanceId, Organization newEdmobj) {
        List<Organization> oldList = getDbaccess().getOneFromDBByInstanceId(oldInstanceId, Organization.class);
        if (oldList != null && !oldList.isEmpty()) {
            Organization old = oldList.get(0);
            if (old.getAddress() != null) {
                newEdmobj.setAddress(old.getAddress());
            }
        }
    }

    private void copyMemberOfFromPreviousVersion(String oldInstanceId, Organization newEdmobj) {
        List<Object> oldMemberOfs = getDbaccess().getOneFromDBBySpecificKey("organization2Instance", oldInstanceId, OrganizationMemberof.class);
        if (oldMemberOfs != null) {
            for (Object obj : oldMemberOfs) {
                OrganizationMemberof old = (OrganizationMemberof) obj;
                OrganizationMemberof copy = new OrganizationMemberof();
                copy.setOrganization1Instance(old.getOrganization1Instance());
                copy.setOrganization2Instance(newEdmobj);
                EposDataModelDAO.getInstance().updateObject(copy);
            }
        }
    }

    private void copyOwnsFromPreviousVersion(String oldInstanceId, Organization newEdmobj) {
        List<Object> oldOwns = getDbaccess().getOneFromDBBySpecificKey("organization", oldInstanceId, OrganizationOwn.class);
        if (oldOwns != null) {
            for (Object obj : oldOwns) {
                OrganizationOwn old = (OrganizationOwn) obj;
                OrganizationOwn copy = new OrganizationOwn();
                copy.setOrganization(newEdmobj);
                copy.setOrganizationInstanceId(newEdmobj.getInstanceId());
                copy.setResourceEntity(old.getResourceEntity());
                copy.setEntityInstanceId(old.getEntityInstanceId());
                EposDataModelDAO.getInstance().updateObject(copy);
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Organization newEdmobj, ElementType type, StatusType overrideStatus) {
        List<Object> oldElements = getDbaccess().getOneFromDBBySpecificKey("organizationInstance", oldInstanceId, OrganizationElement.class);
        if (oldElements != null) {
            for (Object obj : oldElements) {
                OrganizationElement oldOe = (OrganizationElement) obj;
                if (oldOe.getElementInstance() != null && type.name().equals(oldOe.getElementInstance().getType())) {
                    createInnerElement(type, oldOe.getElementInstance().getValue(), newEdmobj, overrideStatus);
                }
            }
        }
    }

    private void createInnerElement(ElementType type, String value, Organization edmobj, StatusType overrideStatus) {
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(type);
        element.setValue(value);
        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        if (!el.isEmpty()) {
            OrganizationElement ce = new OrganizationElement();
            ce.setOrganizationInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    private boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj) != null;
            }
        } catch (Exception e) { }
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    @Override public Boolean delete(String instanceId) {
        deleteRelations("organizationInstance", instanceId, OrganizationContactpoint.class);
        deleteRelations("organization", instanceId, OrganizationOwn.class);
        deleteRelations("organizationInstance", instanceId, DataproductPublisher.class);
        deleteRelations("organization1Instance", instanceId, OrganizationMemberof.class);
        deleteRelations("organizationInstance", instanceId, OrganizationIdentifier.class);
        deleteRelations("organizationInstance", instanceId, OrganizationElement.class);
        deleteRelations("organizationInstance", instanceId, OrganizationAffiliation.class);
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        for (Organization object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override public org.epos.eposdatamodel.Organization retrieve(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList == null || elementList.isEmpty()) return null;
        Organization edmobj = elementList.get(0);
        org.epos.eposdatamodel.Organization o = new org.epos.eposdatamodel.Organization();
        o.setInstanceId(edmobj.getInstanceId()); o.setMetaId(edmobj.getMetaId()); o.setUid(edmobj.getUid());
        o.setAcronym(edmobj.getAcronym()); o.setLeiCode(edmobj.getLeicode()); o.setLogo(edmobj.getLogo());
        o.setURL(edmobj.getUrl()); o.setType(edmobj.getType()); o.setMaturity(edmobj.getMaturity());
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationIdentifier.class))
            o.addIdentifier(retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(((OrganizationIdentifier)object).getIdentifierInstance().getInstanceId()));
        if (edmobj.getAddress() != null) o.setAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(edmobj.getAddress().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationContactpoint.class))
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(((OrganizationContactpoint)object).getContactpointInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organizationInstance", edmobj.getInstanceId(), OrganizationElement.class)) {
            OrganizationElement item = (OrganizationElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
            if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
        }
        if (edmobj.getLegalname() != null && !edmobj.getLegalname().isBlank())
            for (String item : edmobj.getLegalname().split("\\|")) o.addLegalName(item);
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organization", edmobj.getInstanceId(), OrganizationOwn.class)) {
            OrganizationOwn item = (OrganizationOwn) object;
            if (item.getResourceEntity().equals(EntityNames.FACILITY.name())) o.addOwns(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            if (item.getResourceEntity().equals(EntityNames.EQUIPMENT.name())) o.addOwns(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
        }
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("organization2Instance", edmobj.getInstanceId(), OrganizationMemberof.class))
            o.addMemberOf(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(((OrganizationMemberof)object).getOrganization1Instance().getInstanceId()));
        return (org.epos.eposdatamodel.Organization) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override public org.epos.eposdatamodel.Organization retrieveByUID(String uid) {
        List<Organization> returnList = getDbaccess().getOneFromDBByUID(uid, Organization.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.Organization> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Organization.class)); }

    @Override public List<org.epos.eposdatamodel.Organization> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Organization.class)); }

    @Override public List<org.epos.eposdatamodel.Organization> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Organization.class, status)); }

    private List<org.epos.eposdatamodel.Organization> retrieveEntities(Function<Void, List<String>> dbFetcher) { return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList()); }

    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList != null && !elementList.isEmpty()) {
            Organization edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.ORGANIZATION.name());
        }
        return null;
    }
}