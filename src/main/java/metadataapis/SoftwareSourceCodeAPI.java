package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareSourceCode;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        edmobj.setSoftwareStatus(obj.getSoftwareStatus());
        edmobj.setSpatial(obj.getSpatial());
        edmobj.setTemporal(obj.getTemporal());
        edmobj.setFilesize(obj.getSize());
        edmobj.setTimerequired(obj.getTimeRequired());

        /** CATEGORY **/
        if (obj.getCategory() != null)
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null)
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus, edmobj.getVersion());
                List<Identifier> identifierList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(),Identifier.class);
                if(!identifierList.isEmpty()) {
                    SoftwaresourcecodeIdentifier pi = new SoftwaresourcecodeIdentifier();
                    pi.setSoftwaresourcecodeInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }


        /** PROGRAMMING LANGUAGE **/
        if(obj.getProgrammingLanguage()!=null){
            for(String returns : obj.getProgrammingLanguage()) {
                createInnerElement(ElementType.PROGRAMMINGLANGUAGE, returns, edmobj, overrideStatus);
            }
        }


        if (obj.getAuthor() != null) {
            for(LinkedEntity owns : obj.getAuthor()) {
                if (owns != null){
                    SoftwaresourcecodeAuthor pi = new SoftwaresourcecodeAuthor();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getContributor() != null) {
            for(LinkedEntity owns : obj.getContributor()) {
                if (owns != null){
                    SoftwaresourcecodeContributor pi = new SoftwaresourcecodeContributor();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getFunder() != null) {
            for(LinkedEntity owns : obj.getFunder()) {
                if (owns != null){
                    SoftwaresourcecodeFunder pi = new SoftwaresourcecodeFunder();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getMaintainer() != null) {
            for(LinkedEntity owns : obj.getMaintainer()) {
                if (owns != null){
                    SoftwaresourcecodeMaintainer pi = new SoftwaresourcecodeMaintainer();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getProvider() != null) {
            for(LinkedEntity owns : obj.getProvider()) {
                if (owns != null){
                    SoftwaresourcecodeProvider pi = new SoftwaresourcecodeProvider();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getPublisher() != null) {
            for(LinkedEntity owns : obj.getPublisher()) {
                if (owns != null){
                    SoftwaresourcecodePublisher pi = new SoftwaresourcecodePublisher();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getCreator() != null) {
            for(LinkedEntity owns : obj.getCreator()) {
                if (owns != null){
                    SoftwaresourcecodeCreator pi = new SoftwaresourcecodeCreator();
                    pi.setSoftwaresourcecode(edmobj);
                    pi.setSoftwaresourcecodeInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(owns.getEntityType());
                    pi.setEntityInstanceId(owns.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }


        if (obj.getCitation() != null) {
            for(String citation : obj.getCitation()) {
                createInnerElement(ElementType.CITATION, citation, edmobj, overrideStatus);
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

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        SoftwaresourcecodeElement ce = new SoftwaresourcecodeElement();
        ce.setSoftwaresourcecodeInstance(edmobj);
        ce.setElementInstance(el.get(0));

        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeContactpoint.class)){
            SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeIdentifier.class)){
            SoftwaresourcecodeIdentifier item = (SoftwaresourcecodeIdentifier) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeCategory.class)){
            SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeElement.class)){
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }


        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeAuthor.class)){
            SoftwaresourcecodeAuthor item = (SoftwaresourcecodeAuthor) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeContributor.class)){
            SoftwaresourcecodeContributor item = (SoftwaresourcecodeContributor) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeFunder.class)){
            SoftwaresourcecodeFunder item = (SoftwaresourcecodeFunder) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeMaintainer.class)){
            SoftwaresourcecodeMaintainer item = (SoftwaresourcecodeMaintainer) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeProvider.class)){
            SoftwaresourcecodeProvider item = (SoftwaresourcecodeProvider) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodePublisher.class)){
            SoftwaresourcecodePublisher item = (SoftwaresourcecodePublisher) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeCreator.class)){
            SoftwaresourcecodeCreator item = (SoftwaresourcecodeCreator) object;
            if(item.getSoftwaresourcecode().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeElement.class)){
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        for(Softwaresourcecode object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.SoftwareSourceCode retrieve(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
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
            o.setSoftwareStatus(edmobj.getSoftwareStatus());
            o.setSpatial(edmobj.getSpatial());
            o.setTemporal(edmobj.getTemporal());
            o.setSize(edmobj.getFilesize());
            o.setTimeRequired(edmobj.getTimerequired());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeCategory.class)) {
                SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
                //if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                //}
            }
            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeContactpoint.class)) {
                SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
                //if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                // }
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeIdentifier.class)) {
                SoftwaresourcecodeIdentifier item = (SoftwaresourcecodeIdentifier) object;
                // if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                // }
            }


            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeElement.class)) {
                SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
                // if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.PROGRAMMINGLANGUAGE.name())) o.addProgrammingLanguage(el.getValue());
                // }
            }


        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeAuthor.class)) {
            SoftwaresourcecodeAuthor item = (SoftwaresourcecodeAuthor) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addAuthor(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addAuthor(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeContributor.class)) {
            SoftwaresourcecodeContributor item = (SoftwaresourcecodeContributor) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addContributor(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addContributor(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeFunder.class)) {
            SoftwaresourcecodeFunder item = (SoftwaresourcecodeFunder) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addFunder(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addFunder(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeMaintainer.class)) {
            SoftwaresourcecodeMaintainer item = (SoftwaresourcecodeMaintainer) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addMaintainer(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addMaintainer(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeProvider.class)) {
            SoftwaresourcecodeProvider item = (SoftwaresourcecodeProvider) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addProvider(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addProvider(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodePublisher.class)) {
            SoftwaresourcecodePublisher item = (SoftwaresourcecodePublisher) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addPublisher(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addPublisher(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecode", edmobj.getInstanceId(),SoftwaresourcecodeCreator.class)) {
            SoftwaresourcecodeCreator item = (SoftwaresourcecodeCreator) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addCreator(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addCreator(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(),SoftwaresourcecodeElement.class)) {
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.CITATION.name())) o.addCitation(el.getValue());
            //}
        }

        o = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Softwaresourcecode.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Softwaresourcecode.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Softwaresourcecode.class, status));
    }

    private List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveEntities(Function<Void, List<Softwaresourcecode>> dbFetcher) {
        List<Softwaresourcecode> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
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
