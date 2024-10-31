package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareSourceCode;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SoftwareSourceCodeAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareSourceCode> {

    public SoftwareSourceCodeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareSourceCode obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Softwaresourcecode edmobj = new Softwaresourcecode();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setDownloadurl(obj.getDownloadURL());
        edmobj.setKeywords(obj.getKeywords());
        edmobj.setLicenseurl(obj.getLicenseURL());
        edmobj.setMainentityofpage(obj.getMainEntityofPage());
        edmobj.setRuntimeplatform(obj.getRuntimePlatform());
        edmobj.setSoftwareversion(obj.getSoftwareVersion());
        edmobj.setCoderepository(obj.getCodeRepository());

        /** CATEGORY **/
        if (obj.getCategory() != null && !obj.getCategory().isEmpty())
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty())
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus);
                List<Identifier> identifierList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(),Identifier.class);
                if(!identifierList.isEmpty()) {
                    SoftwaresourcecodeIdentifier pi = new SoftwaresourcecodeIdentifier();
                    pi.setSoftwaresourcecodeInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }


        /** PROGRAMMING LANGUAGE **/
        if(obj.getProgrammingLanguage()!=null && !obj.getProgrammingLanguage().isEmpty()){
            for(String returns : obj.getProgrammingLanguage()) {
                createInnerElement(ElementType.PROGRAMMINGLANGUAGE, returns, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Softwaresourcecode edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        ElementAPI api = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);
        LinkedEntity le = api.create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        SoftwaresourcecodeElement ce = new SoftwaresourcecodeElement();
        ce.setSoftwaresourcecodeInstance(edmobj);
        ce.setElementInstance(el.get(0));

        dbaccess.updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeContactpoint.class)){
            SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeIdentifier.class)){
            SoftwaresourcecodeIdentifier item = (SoftwaresourcecodeIdentifier) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeCategory.class)){
            SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeElement.class)){
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        for(Softwaresourcecode object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.SoftwareSourceCode retrieve(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Softwaresourcecode edmobj = elementList.get(0);
            org.epos.eposdatamodel.SoftwareSourceCode o = new org.epos.eposdatamodel.SoftwareSourceCode();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setName(edmobj.getName());
            o.setDescription(edmobj.getDescription());
            o.setDownloadURL(edmobj.getDownloadurl());
            o.addKeywords(edmobj.getKeywords());
            o.setLicenseURL(edmobj.getLicenseurl());
            o.setMainEntityofPage(edmobj.getMainentityofpage());
            o.setRuntimePlatform(edmobj.getRuntimeplatform());
            o.setSoftwareVersion(edmobj.getSoftwareversion());
            o.setCodeRepository(edmobj.getCoderepository());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeCategory.class)) {
                SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
                if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    CategoryAPI api = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                }
            }
            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeContactpoint.class)) {
                SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
                if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    ContactPointAPI api = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeIdentifier.class)) {
                SoftwaresourcecodeIdentifier item = (SoftwaresourcecodeIdentifier) object;
                if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    IdentifierAPI api = new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                }
            }


            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeElement.class)) {
                SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
                if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.PROGRAMMINGLANGUAGE.name())) o.addProgrammingLanguage(el.getValue());
                }
            }

            o = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAll() {
        List<Softwaresourcecode> list = getDbaccess().getAllFromDB(Softwaresourcecode.class);
        List<org.epos.eposdatamodel.SoftwareSourceCode> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Softwaresourcecode edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.SOFTWARESOURCECODE.name());

            return o;
        }
        return null;
    }
}
