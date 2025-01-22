package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ElementAPI extends AbstractAPI<org.epos.eposdatamodel.Element> {

    public ElementAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Element obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Element> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        if (!returnList.isEmpty()) {
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Element) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Element edmobj = new Element();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setType(Optional.ofNullable(obj.getType().toString()).orElse(null));
        edmobj.setValue(Optional.ofNullable(obj.getValue()).orElse(null));

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public org.epos.eposdatamodel.Element retrieve(String instanceId) {
        List<Element> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Element.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Element edmobj = elementList.get(0);
        org.epos.eposdatamodel.Element o = new org.epos.eposdatamodel.Element();

        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(ElementType.valueOf(edmobj.getType()));
        o.setValue(edmobj.getValue());

        return (org.epos.eposdatamodel.Element) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public Boolean delete(String instanceId) {
        // List of element types to delete
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
    public List<org.epos.eposdatamodel.Element> retrieveBunch(List<String> entities) {
        List<Element> list = getDbaccess().getListFromDBByInstanceId(entities, Element.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Element>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Element> retrieveAll() {
        List<Element> list = getDbaccess().getAllFromDB(Element.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Element>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Element> retrieveAllWithStatus(StatusType status) {
        List<Element> list = getDbaccess().getAllFromDBWithStatus(Element.class, status);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Element>> futures = list.stream()
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
        o.setEntityType(EntityNames.ELEMENT.name());

        return o;
    }
}
