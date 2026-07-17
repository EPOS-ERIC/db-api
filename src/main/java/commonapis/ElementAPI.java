package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
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

public class ElementAPI extends AbstractAPI<org.epos.eposdatamodel.Element> {

    public ElementAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Element obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
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

        obj = (org.epos.eposdatamodel.Element) VersioningStatusAPI.checkVersion(obj, overrideStatus);

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
        edmobj.setType(Optional.ofNullable(obj.getType().toString()).orElse(null));
        edmobj.setValue(Optional.ofNullable(obj.getValue()).orElse(null));

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.ELEMENT.name(), edmobj);

        
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
    public org.epos.eposdatamodel.Element retrieveByUID(String uid) {
        List<Element> returnList = getDbaccess().getOneFromDBByUID(uid, Element.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public List<org.epos.eposdatamodel.Element> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Element.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Element> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Element.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Element> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Element.class, status));
    }

    private List<org.epos.eposdatamodel.Element> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return retrieveBulk(dbEntities, Element.class, entity -> {
            org.epos.eposdatamodel.Element dto = new org.epos.eposdatamodel.Element();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setType(ElementType.valueOf(entity.getType()));
            dto.setValue(entity.getValue());
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
        o.setEntityType(EntityNames.ELEMENT.name());

        return o;
    }
}
