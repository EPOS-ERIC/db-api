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
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<Element> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Element selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Element::getVersion);

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Documentation) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

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

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.DOCUMENTATION.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    @Override
    public org.epos.eposdatamodel.Documentation retrieve(String instanceId) {
        List<Element> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Element.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Element edmobj = elementList.get(0);

        if (!"DOCUMENTATION".equals(edmobj.getType())) {
            return null;
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
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Element.class, Map.of(
                ContactpointElement.class, "elementInstance",
                DistributionElement.class, "elementInstance",
                WebserviceElement.class, "elementInstance",
                OrganizationElement.class, "elementInstance",
                PersonElement.class, "elementInstance",
                OperationElement.class, "elementInstance",
                MappingElement.class, "elementInstance",
                SoftwaresourcecodeElement.class, "elementInstance",
                EquipmentElement.class, "elementInstance",
                FacilityElement.class, "elementInstance"));
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
        return retrieveBulk(dbEntities, Element.class, entity -> {
            if (!"DOCUMENTATION".equals(entity.getType())) {
                return null;
            }
            JsonObject doc = new Gson().fromJson(entity.getValue(), JsonObject.class);
            org.epos.eposdatamodel.Documentation dto = new org.epos.eposdatamodel.Documentation();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setTitle(doc.has("Title") ? doc.get("Title").getAsString() : null);
            dto.setDescription(doc.has("Description") ? doc.get("Description").getAsString() : null);
            dto.setUri(doc.has("Uri") ? doc.get("Uri").getAsString() : null);
            return dto;
        });
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
