package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PersonAPI extends AbstractAPI<org.epos.eposdatamodel.Person> {

    private static final Logger LOG = Logger.getLogger(PersonAPI.class.getName());

    public PersonAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Person obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();

        List<Person> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            Person selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Person item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Person) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Person edmobj = new Person();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setFamilyname(obj.getFamilyName());
        edmobj.setGivenname(obj.getGivenName());
        edmobj.setCvurl(obj.getCVURL());
        edmobj.setQualifications(obj.getQualifications() != null ? String.join(", ", obj.getQualifications()) : null);

        // ADDRESS - FIX: Added null check and Pending Relation creation
        if (obj.getAddress() != null) {
            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
            if (le != null && le.getInstanceId() != null) {
                List<Address> address = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Address.class);
                if (!address.isEmpty()) {
                    edmobj.setAddress(address.get(0));
                } else {
                    createPendingAddressRelation(edmobj.getInstanceId(), obj.getAddress());
                }
            } else {
                createPendingAddressRelation(edmobj.getInstanceId(), obj.getAddress());
            }
        }

        // IDENTIFIER
        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    PersonIdentifier.class, Identifier.class,
                    "personInstance", PersonIdentifier::getIdentifierInstance, PersonIdentifier::setPersonInstance, PersonIdentifier::setIdentifierInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // AFFILIATION
        if (obj.getAffiliation() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getAffiliation(), relationFromUpdate, relationToUpdate,
                    OrganizationAffiliation.class, Organization.class,
                    "personInstance", OrganizationAffiliation::getOrganizationInstance, OrganizationAffiliation::setPersonInstance, OrganizationAffiliation::setOrganizationInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // CONTACTPOINT
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), relationFromUpdate, relationToUpdate,
                    PersonContactpoint.class, Contactpoint.class,
                    "personInstance", PersonContactpoint::getContactpointInstance, PersonContactpoint::setPersonInstance, PersonContactpoint::setContactpointInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // ELEMENTS (telephone, email) - with proper sync
        syncElements(edmobj, obj.getTelephone(), ElementType.TELEPHONE, overrideStatus, isNewVersion);
        syncElements(edmobj, obj.getEmail(), ElementType.EMAIL, overrideStatus, isNewVersion);

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.PERSON.name(), edmobj);
        resolvePendingAddressRelationsForAddress(edmobj.getUid(), edmobj.getInstanceId());

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createPendingAddressRelation(String personInstanceId, LinkedEntity addressLink) {
        if (addressLink == null || addressLink.getUid() == null) return;
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setReviewComment(personInstanceId);
            pending.setUid(addressLink.getUid());
            pending.setMetaId("PERSON_ADDRESS");
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance(EntityNames.PERSON.name());
            pending.setChangeComment("ADDRESS_REF");
            pending.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            EposDataModelDAO.getInstance().createObject(pending);
        } catch (Exception e) {
            LOG.warning("Error creating pending address relation: " + e.getMessage());
        }
    }

    public static void resolvePendingAddressRelationsForAddress(String addressUid, String addressInstanceId) {
        try {
            List<Versioningstatus> candidates = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeySimpleNoCache("uid", addressUid, Versioningstatus.class);
            if (candidates == null) return;

            Address addressEntity = null;

            for (Versioningstatus vs : candidates) {
                if (StatusType.PENDING.name().equals(vs.getStatus()) && "PERSON_ADDRESS".equals(vs.getMetaId())) {
                    String personInstanceId = vs.getReviewComment();
                    List<Person> personList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(personInstanceId, Person.class);

                    if (!personList.isEmpty()) {
                        if (addressEntity == null) {
                            List<Address> addrList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(addressInstanceId, Address.class);
                            if (!addrList.isEmpty()) addressEntity = addrList.get(0);
                        }
                        if (addressEntity != null) {
                            Person p = personList.get(0);
                            p.setAddress(addressEntity);
                            EposDataModelDAO.getInstance().updateObject(p);
                            EposDataModelDAO.getInstance().deleteObject(vs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("Error resolving pending address relations: " + e.getMessage());
        }
    }

    /**
     * Sync elements with proper delete/add logic
     */
    private void syncElements(Person edmobj, List<String> values, ElementType elementType, StatusType overrideStatus, boolean isNewVersion) {
        if (values == null) values = new ArrayList<>();

        // Get existing elements of this type
        Set<String> existingValues = new HashSet<>();
        List<PersonElement> existingRelations = new ArrayList<>();

        List<Object> relations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(), PersonElement.class);

        if (relations != null) {
            for (Object obj : relations) {
                PersonElement relation = (PersonElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null && existingElement.getType().equals(elementType.name())) {
                    existingValues.add(existingElement.getValue());
                    existingRelations.add(relation);
                }
            }
        }

        Set<String> newValues = new HashSet<>(values);

        // Delete elements that are no longer present (only if not creating new version)
        if (!isNewVersion) {
            for (PersonElement relation : existingRelations) {
                String value = relation.getElementInstance().getValue();
                if (!newValues.contains(value)) {
                    EposDataModelDAO.getInstance().deleteObject(relation);
                    // Optionally delete the element itself if orphaned
                }
            }
        }

        // Add elements that don't exist yet
        for (String value : values) {
            if (!existingValues.contains(value)) {
                createInnerElement(elementType, value, edmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Person edmobj, StatusType overrideStatus) {
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

        if(!el.isEmpty()){
            PersonElement ce = new PersonElement();
            ce.setPersonInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("personInstance", instanceId, PersonContactpoint.class);
        deleteRelations("personInstance", instanceId, PersonIdentifier.class);
        deleteRelations("personInstance", instanceId, PersonElement.class);
        deleteRelations("personInstance", instanceId, OrganizationAffiliation.class);

        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        for (Person object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Person retrieve(String instanceId) {
        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Person edmobj = elementList.get(0);
        org.epos.eposdatamodel.Person o = new org.epos.eposdatamodel.Person();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(), PersonIdentifier.class)) {
            PersonIdentifier item = (PersonIdentifier) object;
            LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
            o.addIdentifier(le);
        }

        o.setFamilyName(edmobj.getFamilyname());
        o.setGivenName(edmobj.getGivenname());

        if (edmobj.getAddress() != null) {
            o.setAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(edmobj.getAddress().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(), PersonElement.class)) {
            PersonElement item = (PersonElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
            if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
        }

        o.setQualifications(edmobj.getQualifications() != null ?
                Arrays.stream(edmobj.getQualifications().split(", ")).collect(Collectors.toList())
                : new ArrayList<>());
        o.setCVURL(edmobj.getCvurl());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(), OrganizationAffiliation.class)) {
            OrganizationAffiliation item = (OrganizationAffiliation) object;
            LinkedEntity le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId());
            o.addAffiliation(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(), PersonContactpoint.class)) {
            PersonContactpoint item = (PersonContactpoint) object;
            LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
            o.addContactPoint(le);
        }

        o = (org.epos.eposdatamodel.Person) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Person retrieveByUID(String uid) {
        List<Person> returnList = getDbaccess().getOneFromDBByUID(uid, Person.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Person.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Person.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Person.class, status));
    }
    private List<org.epos.eposdatamodel.Person> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        if (elementList != null && !elementList.isEmpty()) {
            Person edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.PERSON.name());
            return o;
        }
        return null;
    }
}