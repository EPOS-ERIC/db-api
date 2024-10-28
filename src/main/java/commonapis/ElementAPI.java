package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.Element;
import model.ElementType;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ElementAPI extends AbstractAPI<org.epos.eposdatamodel.Element> {

    public ElementAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Element obj, StatusType overrideStatus) {

        List<Element> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Element) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Element edmobj = new Element();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
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
        if(elementList!=null && !elementList.isEmpty()) {
            Element edmobj = elementList.get(0);
            org.epos.eposdatamodel.Element o = new org.epos.eposdatamodel.Element();


            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setType(ElementType.valueOf(edmobj.getType()));
            o.setValue(edmobj.getValue());

            o = (org.epos.eposdatamodel.Element) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.Element> retrieveAll() {
        List<Element> list = getDbaccess().getAllFromDB(Element.class);
        List<org.epos.eposdatamodel.Element> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Element> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Element.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Element edmobj = elementList.get(0);

            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.ELEMENT.name());

            return o;
        }
        return null;
    }


}
