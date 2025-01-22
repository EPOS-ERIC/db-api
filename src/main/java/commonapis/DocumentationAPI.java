package commonapis;

import abstractapis.AbstractAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Documentation;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DocumentationAPI extends AbstractAPI<org.epos.eposdatamodel.Documentation> {

    public DocumentationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(Documentation obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Element> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Element.class);

        if (!returnList.isEmpty()) {
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
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

        // Batch deletion for multiple types of elements
        List<Class<?>> elementTypes = List.of(
                ContactpointElement.class,
                DistributionElement.class,
                WebserviceElement.class,
                OrganizationElement.class,
                PersonElement.class,
                OperationElement.class,
                MappingElement.class,
                SoftwaresourcecodeElement.class,
                EquipmentElement.class,
                FacilityElement.class
        );

        elementTypes.forEach(elementType -> {
            List<?> itemsToDelete = (List<?>) getDbaccess().getAllFromDB(elementType).stream()
                    .filter(item -> ((Element) item).getInstanceId().equals(instanceId))
                    .collect(Collectors.toList());
            dbaccess.deleteListOfObjects(itemsToDelete);
        });

        // Delete Element itself
        List<Element> elementList = getDbaccess().getAllFromDB(Element.class);
        elementList.stream()
                .filter(item -> item.getInstanceId().equals(instanceId))
                .forEach(dbaccess::deleteObject);

        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveBunch(List<String> entities) {
        List<Element> list = getDbaccess().getListFromDBByInstanceId(entities, Element.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Documentation>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveAll() {
        List<Element> list = getDbaccess().getAllFromDB(Element.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Documentation>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Documentation> retrieveAllWithStatus(StatusType status) {
        List<Element> list = getDbaccess().getAllFromDBWithStatus(Element.class, status);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Documentation>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
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
