package commonapis;

import abstractapis.AbstractAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Documentation;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentationAPI extends AbstractAPI<org.epos.eposdatamodel.Documentation> {

    public DocumentationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(Documentation obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Element> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Element selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Element item : returnList) {
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

        obj = (org.epos.eposdatamodel.Documentation) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Element edmobj = new Element();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setType(ElementType.DOCUMENTATION.name());

        JsonObject documentationObj = new JsonObject();
        documentationObj.addProperty("Title", obj.getTitle());
        documentationObj.addProperty("Description", obj.getDescription());
        documentationObj.addProperty("Uri", obj.getUri());
        String doc = new Gson().toJson(documentationObj);
        edmobj.setValue(doc);

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public org.epos.eposdatamodel.Documentation retrieve(String instanceId) {
        List<Element> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Element.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Element edmobj = elementList.get(0);

        // Add type validation - only process DOCUMENTATION type Elements
        if (!"DOCUMENTATION".equals(edmobj.getType())) {
            return null; // Skip non-DOCUMENTATION Elements
        }

        org.epos.eposdatamodel.Documentation o = new org.epos.eposdatamodel.Documentation();

        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());

        JsonObject doc = new Gson().fromJson(edmobj.getValue(), JsonObject.class);
        o.setTitle(doc.has("Title") ? doc.get("Title").getAsString() : null);
        o.setDescription(doc.has("Description") ? doc.get("Description").getAsString() : null);
        o.setUri(doc.has("Uri") ? doc.get("Uri").getAsString() : null);

        return (org.epos.eposdatamodel.Documentation) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(ContactpointElement.class)){
            ContactpointElement item = (ContactpointElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DistributionElement.class)){
            DistributionElement item = (DistributionElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceElement.class)){
            WebserviceElement item = (WebserviceElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationElement.class)){
            OrganizationElement item = (OrganizationElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(PersonElement.class)){
            PersonElement item = (PersonElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationElement.class)){
            OperationElement item = (OperationElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(MappingElement.class)){
            MappingElement item = (MappingElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeElement.class)){
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentElement.class)){
            EquipmentElement item = (EquipmentElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityElement.class)){
            FacilityElement item = (FacilityElement) object;
            if(item.getElementInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        // Delete Element itself
        List<Element> elementList = getDbaccess().getAllFromDB(Element.class);
        elementList.stream()
                .filter(item -> item.getInstanceId().equals(instanceId))
                .forEach(item -> EposDataModelDAO.getInstance().deleteObject(item));

        return true;
    }
    @Override
    public org.epos.eposdatamodel.Documentation retrieveByUID(String uid) {
        List<Element> returnList = getDbaccess().getOneFromDBByUID(uid, Element.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Element.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Element.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Element.class, status));
    }

    private List<org.epos.eposdatamodel.Documentation> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Element> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Element.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Element edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.DOCUMENTATION.name());

        return o;
    }
}
