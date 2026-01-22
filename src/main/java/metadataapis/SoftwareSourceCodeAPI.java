package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareSourceCode;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;

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

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            Softwaresourcecode selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Softwaresourcecode item : returnList) {
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

        obj = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Softwaresourcecode edmobj = new Softwaresourcecode();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
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
        edmobj.setSoftwarerequirements(obj.getSoftwareRequirements());

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus);

        // IDENTIFIER
        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    SoftwaresourcecodeIdentifier.class, Identifier.class,
                    "softwaresourcecodeInstance", SoftwaresourcecodeIdentifier::getIdentifierInstance, SoftwaresourcecodeIdentifier::setSoftwaresourcecodeInstance, SoftwaresourcecodeIdentifier::setIdentifierInstance,
                    obj, null, overrideStatus, false
            );
        }

        // PROGRAMMING LANGUAGE
        if (obj.getProgrammingLanguage() != null) {
            for (String returns : obj.getProgrammingLanguage()) {
                createInnerElement(ElementType.PROGRAMMINGLANGUAGE, returns, edmobj, overrideStatus);
            }
        }

        // Polymorphic Relations
        handlePolymorphicRelation(obj.getAuthor(), edmobj, SoftwaresourcecodeAuthor.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getContributor(), edmobj, SoftwaresourcecodeContributor.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getFunder(), edmobj, SoftwaresourcecodeFunder.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getMaintainer(), edmobj, SoftwaresourcecodeMaintainer.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getProvider(), edmobj, SoftwaresourcecodeProvider.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getPublisher(), edmobj, SoftwaresourcecodePublisher.class, overrideStatus, obj.getFileProvenance());
        handlePolymorphicRelation(obj.getCreator(), edmobj, SoftwaresourcecodeCreator.class, overrideStatus, obj.getFileProvenance());

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private <T> void handlePolymorphicRelation(List<LinkedEntity> links, Softwaresourcecode parent, Class<T> joinClass, StatusType overrideStatus, String provenance) {
        if (links == null) return;
        for(LinkedEntity link : links) {
            LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(link, overrideStatus, parent.getVersion(), provenance);
            if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                try {
                    T pi = joinClass.getDeclaredConstructor().newInstance();
                    joinClass.getMethod("setSoftwaresourcecode", Softwaresourcecode.class).invoke(pi, parent);
                    joinClass.getMethod("setSoftwaresourcecodeInstanceId", String.class).invoke(pi, parent.getInstanceId());
                    joinClass.getMethod("setResourceEntity", String.class).invoke(pi, resolvedEntity.getEntityType());
                    joinClass.getMethod("setEntityInstanceId", String.class).invoke(pi, resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Softwaresourcecode edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                SoftwaresourcecodeElement relation = (SoftwaresourcecodeElement) obj;
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
        if(!el.isEmpty()){
            SoftwaresourcecodeElement ce = new SoftwaresourcecodeElement();
            ce.setSoftwaresourcecodeInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeContactpoint.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeIdentifier.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeCategory.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeElement.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeAuthor.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeContributor.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeFunder.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeMaintainer.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeProvider.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodePublisher.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeCreator.class);

        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        for (Softwaresourcecode object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.SoftwareSourceCode retrieve(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList == null || elementList.isEmpty()) return null;

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
        o.setSoftwareRequirements(edmobj.getSoftwarerequirements());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeCategory.class)) {
            SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
            o.addCategory(le);
        }
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeContactpoint.class)) {
            SoftwaresourcecodeContactpoint item = (SoftwaresourcecodeContactpoint) object;
            LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
            o.addContactPoint(le);
        }
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeIdentifier.class)) {
            SoftwaresourcecodeIdentifier item = (SoftwaresourcecodeIdentifier) object;
            LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
            o.addIdentifier(le);
        }
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeElement.class)) {
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.PROGRAMMINGLANGUAGE.name())) o.addProgrammingLanguage(el.getValue());
        }

        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeAuthor.class, "addAuthor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeContributor.class, "addContributor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeFunder.class, "addFunder");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeMaintainer.class, "addMaintainer");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeProvider.class, "addProvider");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodePublisher.class, "addPublisher");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeCreator.class, "addCreator");

        o = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    private void retrievePolymorphicRelations(org.epos.eposdatamodel.SoftwareSourceCode o, String id, Class<?> clazz, String methodName) {
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecode", id, clazz)) {
            try {
                String resourceEntity = (String) clazz.getMethod("getResourceEntity").invoke(object);
                String entityInstanceId = (String) clazz.getMethod("getEntityInstanceId").invoke(object);
                LinkedEntity le = null;
                if (EntityNames.PERSON.name().equals(resourceEntity)) {
                    le = retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(entityInstanceId);
                } else if (EntityNames.ORGANIZATION.name().equals(resourceEntity)) {
                    le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(entityInstanceId);
                }
                if (le != null) {
                    o.getClass().getMethod(methodName, LinkedEntity.class).invoke(o, le);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public org.epos.eposdatamodel.SoftwareSourceCode retrieveByUID(String uid) {
        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDBByUID(uid, Softwaresourcecode.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwaresourcecode.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwaresourcecode.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwaresourcecode.class, status));
    }
    private List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList != null && !elementList.isEmpty()) {
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