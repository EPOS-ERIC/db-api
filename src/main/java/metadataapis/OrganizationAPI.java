package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * OrganizationAPI - Manages Organization entities with proper support for:
 * - Organization-Organization relations (memberOf)
 * - Organization-ContactPoint relations
 * - REFERENCE_ENTITY logic: Organizations are shared entities, always use PUBLISHED versions
 *   for relations unless editorId is "ingestor"
 */
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

        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

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

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        // FIX: Determine if REFERENCE_ENTITY logic should be applied
        boolean useReferenceEntityLogic = shouldApplyReferenceEntityLogic(obj);

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

        // ADDRESS (Single) - with pending relation support for out-of-order creation
        if (addressExplicitlySet || !isNewVersion) {
            if (obj.getAddress() != null) {
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (le != null && le.getInstanceId() != null) {
                    List<Address> addressList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Address.class);
                    if (!addressList.isEmpty()) {
                        edmobj.setAddress(addressList.get(0));
                    } else {
                        // Address entity not found - create pending relation
                        createPendingAddressRelation(edmobj.getInstanceId(), obj.getAddress());
                    }
                } else {
                    // LinkedEntity couldn't be resolved - create pending relation
                    createPendingAddressRelation(edmobj.getInstanceId(), obj.getAddress());
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyAddressFromPreviousVersion(oldInstanceId, edmobj);
        }

        // CONTACTPOINT - Uses RelationSyncUtil which already has REFERENCE_ENTITY logic
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

        // FIX: MEMBER OF - Now uses REFERENCE_ENTITY logic for Organization-to-Organization relations
        if (memberOfExplicitlySet || !isNewVersion) {
            if (obj.getMemberOf() != null && !obj.getMemberOf().isEmpty()) {
                // Clear existing memberOf relations for this entity
                List<Object> existingMemberOf = getDbaccess().getOneFromDBBySpecificKey("organization2Instance", edmobj.getInstanceId(), OrganizationMemberof.class);
                if (existingMemberOf != null) {
                    for (Object o : existingMemberOf) {
                        getDbaccess().deleteObject(o);
                    }
                }

                for (LinkedEntity rel : obj.getMemberOf()) {
                    // FIX: Use findOrCreateOrganizationForRelation instead of LinkedEntityAPI.createFromLinkedEntity
                    Organization parentOrg = findOrCreateOrganizationForRelation(rel, overrideStatus, useReferenceEntityLogic);
                    if (parentOrg != null) {
                        // Prevent self-reference
                        if (parentOrg.getInstanceId().equals(edmobj.getInstanceId())) {
                            LOG.log(Level.WARNING, "[OrganizationAPI] Skipping self-reference in memberOf for organization: " + edmobj.getUid());
                            continue;
                        }

                        OrganizationMemberof om = new OrganizationMemberof();
                        om.setOrganization1Instance(parentOrg);
                        om.setOrganization2Instance(edmobj);

                        LOG.log(Level.FINE, "[OrganizationAPI] Creating memberOf relation: {0} memberOf {1}",
                                new Object[]{edmobj.getUid(), parentOrg.getUid()});

                        try {
                            EposDataModelDAO.getInstance().updateObject(om);
                        } catch (Exception e) {
                            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                                    e.getMessage().contains("already exists"))) {
                                LOG.log(Level.FINE, "[OrganizationAPI] MemberOf relation already exists, skipping");
                            } else {
                                LOG.log(Level.WARNING, "[OrganizationAPI] Error creating memberOf relation: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyMemberOfFromPreviousVersion(oldInstanceId, edmobj);
        }

        // OWNS (polymorphic - can be Facility or Equipment) - Not reference entities, no changes needed
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

    // =========================================================================
    // FIX: REFERENCE_ENTITY LOGIC FOR ORGANIZATION
    // =========================================================================

    /**
     * Determines if REFERENCE_ENTITY logic should be applied.
     * Returns false if editorId is "ingestor" (ingestor mode bypasses reference entity logic).
     */
    private boolean shouldApplyReferenceEntityLogic(EPOSDataModelEntity entity) {
        if (entity == null) return true;
        String editorId = entity.getEditorId();
        if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
            LOG.log(Level.FINE, "[OrganizationAPI] INGESTOR MODE: Bypassing REFERENCE_ENTITY logic");
            return false;
        }
        return true;
    }

    /**
     * FIX: Finds or creates an Organization for relation purposes.
     * For REFERENCE_ENTITY mode: always returns the PUBLISHED version.
     */
    private Organization findOrCreateOrganizationForRelation(LinkedEntity link, StatusType status, boolean useReferenceEntityLogic) {

        // First, try to find by UID
        if (link.getUid() != null) {
            List<Organization> byUid = getDbaccess().getOneFromDBByUID(link.getUid(), Organization.class);
            if (!byUid.isEmpty()) {
                if (useReferenceEntityLogic) {
                    // FIX: Find PUBLISHED version
                    Organization published = findPublishedOrganization(link.getUid());
                    if (published != null) {
                        LOG.log(Level.FINE, "[OrganizationAPI] REFERENCE_ENTITY: Using PUBLISHED Organization for uid=" + link.getUid());
                        return published;
                    }
                    // No PUBLISHED - return first found but log warning
                    LOG.log(Level.WARNING, "[OrganizationAPI] No PUBLISHED Organization found for uid=" + link.getUid() +
                            ", using existing version");
                    return byUid.get(0);
                } else {
                    // Ingestor mode: find matching status or first
                    for (Organization org : byUid) {
                        if (org.getVersion() != null && status != null &&
                                status.name().equals(org.getVersion().getStatus())) {
                            return org;
                        }
                    }
                    return byUid.get(0);
                }
            }
        }

        // Try by instanceId
        if (link.getInstanceId() != null) {
            List<Organization> byInstanceId = getDbaccess().getOneFromDBByInstanceId(link.getInstanceId(), Organization.class);
            if (!byInstanceId.isEmpty()) {
                Organization found = byInstanceId.get(0);
                if (useReferenceEntityLogic && found.getUid() != null) {
                    // Still need to check if there's a PUBLISHED version
                    Organization published = findPublishedOrganization(found.getUid());
                    if (published != null) {
                        return published;
                    }
                }
                return found;
            }
        }

        // Try by LinkedEntity lookup
        List<Organization> found = getDbaccess().getOneFromDBByLinkedEntity(link, Organization.class);
        if (!found.isEmpty()) {
            if (useReferenceEntityLogic && found.get(0).getUid() != null) {
                Organization published = findPublishedOrganization(found.get(0).getUid());
                if (published != null) {
                    return published;
                }
            }
            return found.get(0);
        }

        // Organization doesn't exist - create a stub
        LOG.log(Level.FINE, "[OrganizationAPI] Creating stub Organization for uid=" + link.getUid());
        return createOrganizationStub(link, status, useReferenceEntityLogic);
    }

    /**
     * FIX: Finds the PUBLISHED version of an Organization by UID.
     */
    private Organization findPublishedOrganization(String uid) {
        if (uid == null) return null;

        List<Organization> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, Organization.class);

        for (Organization org : allVersions) {
            if (org.getVersion() != null && StatusType.PUBLISHED.name().equals(org.getVersion().getStatus())) {
                return org;
            }
        }
        return null;
    }

    /**
     * FIX: Creates a stub Organization entity.
     * For REFERENCE_ENTITY mode: always creates with PUBLISHED status.
     */
    private Organization createOrganizationStub(LinkedEntity link, StatusType status, boolean useReferenceEntityLogic) {
        String instanceId = UUID.randomUUID().toString();
        String metaId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();

        // FIX: REFERENCE_ENTITY: Always create as PUBLISHED
        StatusType finalStatus;
        if (useReferenceEntityLogic) {
            finalStatus = StatusType.PUBLISHED;
            LOG.log(Level.FINE, "[OrganizationAPI] REFERENCE_ENTITY: Creating stub Organization as PUBLISHED");
        } else {
            finalStatus = status != null ? status : StatusType.DRAFT;
        }

        model.Versioningstatus vs = new model.Versioningstatus();
        vs.setInstanceId(instanceId);
        vs.setMetaId(metaId);
        vs.setVersionId(versionId);
        vs.setUid(link.getUid());
        vs.setStatus(finalStatus.toString());
        vs.setChangeTimestamp(java.time.OffsetDateTime.now());
        vs.setChangeComment("Auto-created stub for pending relation");

        getDbaccess().createObject(vs);

        Organization stub = new Organization();
        stub.setUid(link.getUid());
        stub.setInstanceId(instanceId);
        stub.setMetaId(metaId);
        stub.setVersion(vs);

        getDbaccess().createObject(stub);

        // Register in entity ID table
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(metaId, EntityNames.ORGANIZATION.name());

        return stub;
    }

    // =========================================================================
    // Remaining methods unchanged
    // =========================================================================

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
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Field access failed for {0}: {1}", 
                    new Object[]{fieldName, e.getMessage()});
        }
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
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
        for (Organization object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
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
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    /**
     * Bulk retrieval implementation that minimizes database queries.
     * Instead of N+1 queries per entity, this fetches all data in batches.
     */
    private List<org.epos.eposdatamodel.Organization> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Batch fetch all Organization entities
        Map<String, Organization> organizations = getDbaccess().batchFetchByInstanceIds(instanceIds, Organization.class);
        
        if (organizations.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(organizations.keySet());
        
        // Step 2: Batch fetch ALL join tables for ALL organizations at once
        Map<String, List<OrganizationIdentifier>> identifiers = 
                getDbaccess().batchFetchRelationsForMultipleParents("organizationInstance", foundIds, OrganizationIdentifier.class);
        Map<String, List<OrganizationContactpoint>> contactPoints = 
                getDbaccess().batchFetchRelationsForMultipleParents("organizationInstance", foundIds, OrganizationContactpoint.class);
        Map<String, List<OrganizationElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("organizationInstance", foundIds, OrganizationElement.class);
        Map<String, List<OrganizationOwn>> owns = 
                getDbaccess().batchFetchRelationsForMultipleParents("organization", foundIds, OrganizationOwn.class);
        Map<String, List<OrganizationMemberof>> memberOfs = 
                getDbaccess().batchFetchRelationsForMultipleParents("organization2Instance", foundIds, OrganizationMemberof.class);
        
        // Step 3: Collect all target entity IDs for batch fetching
        Set<String> allIdentifierIds = new HashSet<>();
        Set<String> allContactPointIds = new HashSet<>();
        Set<String> allAddressIds = new HashSet<>();
        Set<String> allFacilityIds = new HashSet<>();
        Set<String> allEquipmentIds = new HashSet<>();
        Set<String> allParentOrgIds = new HashSet<>();
        
        identifiers.values().forEach(list -> list.forEach(r -> {
            if (r.getIdentifierInstance() != null) allIdentifierIds.add(r.getIdentifierInstance().getInstanceId());
        }));
        contactPoints.values().forEach(list -> list.forEach(r -> {
            if (r.getContactpointInstance() != null) allContactPointIds.add(r.getContactpointInstance().getInstanceId());
        }));
        // Collect addresses from the Organization entities directly
        for (Organization org : organizations.values()) {
            if (org.getAddress() != null) allAddressIds.add(org.getAddress().getInstanceId());
        }
        owns.values().forEach(list -> list.forEach(r -> {
            if (EntityNames.FACILITY.name().equals(r.getResourceEntity())) allFacilityIds.add(r.getEntityInstanceId());
            if (EntityNames.EQUIPMENT.name().equals(r.getResourceEntity())) allEquipmentIds.add(r.getEntityInstanceId());
        }));
        memberOfs.values().forEach(list -> list.forEach(r -> {
            if (r.getOrganization1Instance() != null) allParentOrgIds.add(r.getOrganization1Instance().getInstanceId());
        }));
        
        // Step 4: Batch fetch all target entities
        Map<String, Identifier> identifierMap = allIdentifierIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allIdentifierIds), Identifier.class);
        Map<String, Contactpoint> contactPointMap = allContactPointIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allContactPointIds), Contactpoint.class);
        Map<String, Address> addressMap = allAddressIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allAddressIds), Address.class);
        Map<String, Facility> facilityMap = allFacilityIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allFacilityIds), Facility.class);
        Map<String, Equipment> equipmentMap = allEquipmentIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allEquipmentIds), Equipment.class);
        Map<String, Organization> parentOrgMap = allParentOrgIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allParentOrgIds), Organization.class);
        
        // Step 5: Batch fetch versioning status
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Step 5b: Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = organizations.values().stream()
                .map(Organization::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        // Step 6: Assemble all DTOs from pre-fetched data
        List<org.epos.eposdatamodel.Organization> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Organization edmobj = organizations.get(instanceId);
            if (edmobj != null) {
                org.epos.eposdatamodel.Organization dto = assembleOrganization(
                        instanceId, edmobj,
                        identifiers, contactPoints, elements, owns, memberOfs,
                        identifierMap, contactPointMap, addressMap, facilityMap, equipmentMap, parentOrgMap,
                        versioningMap, groupsMap
                );
                results.add(dto);
            }
        }
        
        return results;
    }

    /**
     * Assembles an Organization DTO from pre-fetched data without additional queries.
     */
    private org.epos.eposdatamodel.Organization assembleOrganization(
            String instanceId,
            Organization edmobj,
            Map<String, List<OrganizationIdentifier>> identifiers,
            Map<String, List<OrganizationContactpoint>> contactPoints,
            Map<String, List<OrganizationElement>> elements,
            Map<String, List<OrganizationOwn>> owns,
            Map<String, List<OrganizationMemberof>> memberOfs,
            Map<String, Identifier> identifierMap,
            Map<String, Contactpoint> contactPointMap,
            Map<String, Address> addressMap,
            Map<String, Facility> facilityMap,
            Map<String, Equipment> equipmentMap,
            Map<String, Organization> parentOrgMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
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
        
        // Parse legalName
        if (edmobj.getLegalname() != null && !edmobj.getLegalname().isBlank()) {
            for (String item : edmobj.getLegalname().split("\\|")) {
                o.addLegalName(item);
            }
        }
        
        // Add identifier relations
        for (OrganizationIdentifier rel : identifiers.getOrDefault(instanceId, Collections.emptyList())) {
            Identifier target = identifierMap.get(rel.getIdentifierInstance().getInstanceId());
            if (target != null) {
                o.addIdentifier(createLinkedEntity(target, EntityNames.IDENTIFIER.name()));
            }
        }
        
        // Add address (single field)
        if (edmobj.getAddress() != null) {
            Address addr = addressMap.get(edmobj.getAddress().getInstanceId());
            if (addr != null) {
                o.setAddress(createLinkedEntity(addr, EntityNames.ADDRESS.name()));
            }
        }
        
        // Add contactPoint relations
        for (OrganizationContactpoint rel : contactPoints.getOrDefault(instanceId, Collections.emptyList())) {
            Contactpoint target = contactPointMap.get(rel.getContactpointInstance().getInstanceId());
            if (target != null) {
                o.addContactPoint(createLinkedEntity(target, EntityNames.CONTACTPOINT.name()));
            }
        }
        
        // Add element data (telephone, email)
        for (OrganizationElement item : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = item.getElementInstance();
            if (el != null) {
                if (ElementType.TELEPHONE.name().equals(el.getType())) o.addTelephone(el.getValue());
                if (ElementType.EMAIL.name().equals(el.getType())) o.addEmail(el.getValue());
            }
        }
        
        // Add owns relations (polymorphic: Facility or Equipment)
        for (OrganizationOwn rel : owns.getOrDefault(instanceId, Collections.emptyList())) {
            if (EntityNames.FACILITY.name().equals(rel.getResourceEntity())) {
                Facility target = facilityMap.get(rel.getEntityInstanceId());
                if (target != null) {
                    o.addOwns(createLinkedEntity(target, EntityNames.FACILITY.name()));
                }
            } else if (EntityNames.EQUIPMENT.name().equals(rel.getResourceEntity())) {
                Equipment target = equipmentMap.get(rel.getEntityInstanceId());
                if (target != null) {
                    o.addOwns(createLinkedEntity(target, EntityNames.EQUIPMENT.name()));
                }
            }
        }
        
        // Add memberOf relations
        for (OrganizationMemberof rel : memberOfs.getOrDefault(instanceId, Collections.emptyList())) {
            Organization parent = parentOrgMap.get(rel.getOrganization1Instance().getInstanceId());
            if (parent != null) {
                o.addMemberOf(createLinkedEntity(parent, EntityNames.ORGANIZATION.name()));
            }
        }
        
        // Apply versioning from pre-fetched data
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) {
                o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            }
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try {
                    o.setStatus(StatusType.valueOf(vs.getStatus()));
                } catch (Exception e) {
                    // Ignore invalid status
                }
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

    /**
     * Creates a LinkedEntity from a JPA entity.
     */
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
        List<Organization> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Organization.class);
        if (elementList != null && !elementList.isEmpty()) {
            Organization edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.ORGANIZATION.name());
        }
        return null;
    }

    // ===== Pending Address Relation Support =====

    /**
     * Creates a pending relation when an Organization references an Address that doesn't exist yet.
     * The relation will be resolved when the Address is created later.
     */
    private void createPendingAddressRelation(String organizationInstanceId, LinkedEntity addressLink) {
        if (addressLink == null || addressLink.getUid() == null) return;
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setReviewComment(organizationInstanceId);
            pending.setUid(addressLink.getUid());
            pending.setMetaId("ORGANIZATION_ADDRESS");
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance(EntityNames.ORGANIZATION.name());
            pending.setChangeComment("ADDRESS_REF");
            pending.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            EposDataModelDAO.getInstance().createObject(pending);
            LOG.log(Level.FINE, "Created pending address relation for Organization {0} -> Address UID {1}",
                    new Object[]{organizationInstanceId, addressLink.getUid()});
        } catch (Exception e) {
            LOG.warning("Error creating pending address relation: " + e.getMessage());
        }
    }

    /**
     * Resolves pending Organization-Address relations when an Address is created.
     * Called by AddressAPI after successfully creating an Address entity.
     * 
     * @param addressUid The UID of the newly created Address
     * @param addressInstanceId The instanceId of the newly created Address
     */
    public static void resolvePendingAddressRelationsForAddress(String addressUid, String addressInstanceId) {
        try {
            List<Versioningstatus> candidates = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeySimpleNoCache("uid", addressUid, Versioningstatus.class);
            if (candidates == null) return;

            Address addressEntity = null;

            for (Versioningstatus vs : candidates) {
                if (StatusType.PENDING.name().equals(vs.getStatus()) && "ORGANIZATION_ADDRESS".equals(vs.getMetaId())) {
                    String organizationInstanceId = vs.getReviewComment();
                    List<Organization> orgList = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceId(organizationInstanceId, Organization.class);

                    if (!orgList.isEmpty()) {
                        // Lazy load address entity only when needed
                        if (addressEntity == null) {
                            List<Address> addrList = EposDataModelDAO.getInstance()
                                    .getOneFromDBByInstanceId(addressInstanceId, Address.class);
                            if (!addrList.isEmpty()) {
                                addressEntity = addrList.get(0);
                            }
                        }
                        if (addressEntity != null) {
                            Organization org = orgList.get(0);
                            org.setAddress(addressEntity);
                            EposDataModelDAO.getInstance().updateObject(org);
                            EposDataModelDAO.getInstance().deleteObject(vs);
                            LOG.log(Level.FINE, "Resolved pending address relation: Organization {0} -> Address {1}",
                                    new Object[]{organizationInstanceId, addressInstanceId});
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("Error resolving pending address relations for Organization: " + e.getMessage());
        }
    }
}