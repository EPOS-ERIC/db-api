package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PersonAPI extends AbstractAPI<org.epos.eposdatamodel.Person> {

    public PersonAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Person obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Person> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        if(!returnList.isEmpty()){
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Person) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Person edmobj = new Person();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setFamilyname(obj.getFamilyName());
        edmobj.setGivenname(obj.getGivenName());
        edmobj.setCvurl(obj.getCVURL());
        edmobj.setQualifications(obj.getQualifications()!=null? String.join(", ", obj.getQualifications()) : null);

        /** ADDRESS **/
        if (obj.getAddress() != null) {
           LinkedEntityAPI.createFromLinkedEntity(obj.getAddress(), overrideStatus);
        }

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus);
                List<Identifier> identifierList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(),Identifier.class);
                if(!identifierList.isEmpty()) {
                    PersonIdentifier pi = new PersonIdentifier();
                    pi.setPersonInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** AFFILIATION **/
        if (obj.getAffiliation() != null) {
            if(relationFromUpdate!=null && obj.getAffiliation().contains(relationFromUpdate)){
                obj.getAffiliation().remove(relationFromUpdate);
                obj.getAffiliation().add(relationToUpdate);
            }
            for(LinkedEntity organization : obj.getAffiliation()){
                Organization organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, organization, overrideStatus, Organization.class, false);
                if(organization1!=null) {
                    OrganizationAffiliation pi = new OrganizationAffiliation();
                    pi.setPersonInstance(edmobj);
                    pi.setOrganizationInstance(organization1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null) {
            if(relationFromUpdate!=null && obj.getContactPoint().contains(relationFromUpdate)){
                obj.getContactPoint().remove(relationFromUpdate);
                obj.getContactPoint().add(relationToUpdate);
            }
            for(LinkedEntity contactpoint : obj.getContactPoint()){
                Contactpoint contactpoint1 = (Contactpoint) RelationChecker.checkRelation(obj, previousObj, null, contactpoint, overrideStatus, Contactpoint.class, false);
                if(contactpoint1!=null) {
                    PersonContactpoint pi = new PersonContactpoint();
                    pi.setPersonInstance(edmobj);
                    pi.setContactpointInstance(contactpoint1);
                    dbaccess.updateObject(pi);
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

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Person edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        PersonElement ce = new PersonElement();
        ce.setPersonInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(PersonContactpoint.class)){
            PersonContactpoint item = (PersonContactpoint) object;
            if(item.getPersonInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(PersonIdentifier.class)){
            PersonIdentifier item = (PersonIdentifier) object;
            if(item.getPersonInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(PersonElement.class)){
            PersonElement item = (PersonElement) object;
            if(item.getPersonInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        for(Person object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Person retrieve(String instanceId) {
        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Person edmobj = elementList.get(0);
            org.epos.eposdatamodel.Person o = new org.epos.eposdatamodel.Person();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(),PersonIdentifier.class)) {
                PersonIdentifier item = (PersonIdentifier) object;
                //if(item.getPersonInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                //}
            }

            o.setFamilyName(edmobj.getFamilyname());
            o.setGivenName(edmobj.getGivenname());


            if (edmobj.getAddress() != null) {
                o.setAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(edmobj.getAddress().getInstanceId()));
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(),PersonElement.class)) {
                PersonElement item = (PersonElement) object;
                //if(item.getPersonInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.TELEPHONE.name())) o.addTelephone(el.getValue());
                    if (el.getType().equals(ElementType.EMAIL.name())) o.addEmail(el.getValue());
                //}
            }

            o.setQualifications(edmobj.getQualifications() != null ?
                    Arrays.stream(edmobj.getQualifications().split(", ")).collect(Collectors.toList())
                    : new ArrayList<>());

            o.setCVURL(edmobj.getCvurl());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(),OrganizationAffiliation.class)) {
                OrganizationAffiliation item = (OrganizationAffiliation) object;
                //if(item.getPersonInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId());
                    o.addAffiliation(le);
                // }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("personInstance", edmobj.getInstanceId(),PersonContactpoint.class)) {
                PersonContactpoint item = (PersonContactpoint) object;
                //if(item.getPersonInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                //}
            }


            o = (org.epos.eposdatamodel.Person) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Person.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Person.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Person> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Person.class, status));
    }

    private List<org.epos.eposdatamodel.Person> retrieveEntities(Function<Void, List<Person>> dbFetcher) {
        List<Person> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Person> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Person.class);
        if(elementList!=null && !elementList.isEmpty()) {
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
