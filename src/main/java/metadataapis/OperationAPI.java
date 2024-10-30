package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;

import java.util.*;

public class OperationAPI extends AbstractAPI<org.epos.eposdatamodel.Operation> {

    public OperationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Operation obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Operation> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Operation edmobj = new Operation();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setMethod(obj.getMethod());
        edmobj.setTemplate(obj.getTemplate());

        if (obj.getMapping() != null && !obj.getMapping().isEmpty()) {
            if(relationFromUpdate!=null && obj.getMapping().contains(relationFromUpdate)){
                obj.getMapping().remove(relationFromUpdate);
                obj.getMapping().add(relationToUpdate);
            }
            for(LinkedEntity mapping : obj.getMapping()){
                Mapping mapping1 = (Mapping) RelationChecker.checkRelation(obj, previousObj, null, mapping, overrideStatus, Mapping.class);
                if(mapping1!=null){
                    OperationMapping pi = new OperationMapping();
                    pi.setOperationInstance(edmobj);
                    pi.setMappingInstance(mapping1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getWebservice() != null && !obj.getWebservice().isEmpty()) {
            if(relationFromUpdate!=null && obj.getWebservice().contains(relationFromUpdate)){
                obj.getWebservice().remove(relationFromUpdate);
                obj.getWebservice().add(relationToUpdate);
            }
            for(LinkedEntity webService : obj.getWebservice()){
                Webservice webservice = (Webservice) RelationChecker.checkRelation(obj, previousObj, null, webService, overrideStatus, Webservice.class);
                if(webservice!=null){
                    OperationWebservice pi = new OperationWebservice();
                    pi.setOperationInstance(edmobj);
                    pi.setWebserviceInstance(webservice);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** RETURNS **/
        if(obj.getReturns()!=null && !obj.getReturns().isEmpty()){
            for(String returns : obj.getReturns()) {
                createInnerElement(ElementType.RETURNS, returns, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Operation edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        ElementAPI api = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);
        LinkedEntity le = api.create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        OperationElement ce = new OperationElement();
        ce.setOperationInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(OperationElement.class)){
            OperationElement item = (OperationElement) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationMapping.class)){
            OperationMapping item = (OperationMapping) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationDistribution.class)){
            OperationDistribution item = (OperationDistribution) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationWebservice.class)){
            OperationWebservice item = (OperationWebservice) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        for(Operation object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }


    @Override
    public org.epos.eposdatamodel.Operation retrieve(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Operation edmobj = elementList.get(0);
            org.epos.eposdatamodel.Operation o = new org.epos.eposdatamodel.Operation();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setMethod(edmobj.getMethod());
            o.setTemplate(edmobj.getTemplate());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("operation_instance_id", edmobj.getInstanceId(),OperationWebservice.class)) {
                OperationWebservice item = (OperationWebservice) object;
                if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    WebServiceAPI api = new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
                    o.addWebservice(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("operation_instance_id", edmobj.getInstanceId(),OperationMapping.class)) {
                OperationMapping item = (OperationMapping) object;
                if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    MappingAPI api = new MappingAPI(EntityNames.MAPPING.name(), Mapping.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getMappingInstance().getInstanceId());
                    o.addMapping(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("operation_instance_id", edmobj.getInstanceId(),OperationElement.class)) {
                OperationElement item = (OperationElement) object;
                if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.RETURNS.name())) o.addReturns(el.getValue());
                }
            }

            o = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }


    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAll() {
        List<Operation> list = getDbaccess().getAllFromDB(Operation.class);
        List<org.epos.eposdatamodel.Operation> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Operation edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.OPERATION.name());

            return o;
        }
        return null;
    }

}
