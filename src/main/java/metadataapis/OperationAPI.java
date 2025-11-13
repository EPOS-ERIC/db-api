package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.IriTemplate;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        if (obj.getMapping() != null) {
            if(relationFromUpdate!=null && obj.getMapping().contains(relationFromUpdate)){
                obj.getMapping().remove(relationFromUpdate);
                obj.getMapping().add(relationToUpdate);
            }
            for(LinkedEntity mapping : obj.getMapping()){
                Mapping mapping1 = (Mapping) RelationChecker.checkRelation(obj, previousObj, null, mapping, overrideStatus, Mapping.class, false);
                if(mapping1!=null){
                    OperationMapping pi = new OperationMapping();
                    pi.setOperationInstance(edmobj);
                    pi.setMappingInstance(mapping1);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getWebservice() != null) {
            if(relationFromUpdate!=null && obj.getWebservice().contains(relationFromUpdate)){
                obj.getWebservice().remove(relationFromUpdate);
                obj.getWebservice().add(relationToUpdate);
            }
            for(LinkedEntity webService : obj.getWebservice()){
                Webservice webservice = (Webservice) RelationChecker.checkRelation(obj, previousObj, null, webService, overrideStatus, Webservice.class, true);
                if(webservice!=null){
                    OperationWebservice pi = new OperationWebservice();
                    pi.setOperationInstance(edmobj);
                    pi.setWebserviceInstance(webservice);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getPayload() != null) {
            if(relationFromUpdate!=null && obj.getPayload().contains(relationFromUpdate)){
                obj.getPayload().remove(relationFromUpdate);
                obj.getPayload().add(relationToUpdate);
            }
            for(LinkedEntity payload : obj.getPayload()){
                Payload payload1 = (Payload) RelationChecker.checkRelation(obj, previousObj, null, payload, overrideStatus, Payload.class, true);
                if(payload1!=null){
                    OperationPayload pi = new OperationPayload();
                    pi.setOperationInstance(edmobj);
                    pi.setPayloadInstance(payload1);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        /** RETURNS **/
        if(obj.getReturns()!=null){
            for(Object object : getDbaccess().getAllFromDB(OperationElement.class)){
                OperationElement item = (OperationElement) object;
                if(item.getOperationInstance().getInstanceId().equals(obj.getInstanceId())){
                    EposDataModelDAO.getInstance().deleteObject(item);
                }
            }
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

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        OperationElement ce = new OperationElement();
        ce.setOperationInstance(edmobj);
        ce.setElementInstance(el.get(0));
        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(OperationElement.class)){
            OperationElement item = (OperationElement) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationMapping.class)){
            OperationMapping item = (OperationMapping) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationDistribution.class)){
            OperationDistribution item = (OperationDistribution) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationWebservice.class)){
            OperationWebservice item = (OperationWebservice) object;
            if(item.getOperationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        for(Operation object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }


    @Override
    public org.epos.eposdatamodel.Operation retrieve(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Operation edmobj = elementList.get(0);
            org.epos.eposdatamodel.Operation o = new org.epos.eposdatamodel.Operation();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setMethod(edmobj.getMethod());
            o.setTemplate(edmobj.getTemplate());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(),OperationWebservice.class)) {
                OperationWebservice item = (OperationWebservice) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
                    o.addWebservice(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(),OperationPayload.class)) {
                OperationPayload item = (OperationPayload) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.PAYLOAD.name()).retrieveLinkedEntity(item.getPayloadInstance().getInstanceId());
                o.addPayload(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(),OperationMapping.class)) {
                OperationMapping item = (OperationMapping) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.MAPPING.name()).retrieveLinkedEntity(item.getMappingInstance().getInstanceId());
                    o.addMapping(le);
                //}
            }


            IriTemplate iriTemplateObject = new IriTemplate();
            iriTemplateObject.setMappings(o.getMapping());
            iriTemplateObject.setTemplate(o.getTemplate());
            o.setIriTemplateObject(iriTemplateObject);

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(),OperationElement.class)) {
                OperationElement item = (OperationElement) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.RETURNS.name())) o.addReturns(el.getValue());
                //}
            }

            o = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }

    @Override
    public org.epos.eposdatamodel.Operation retrieveByUID(String uid) {
        List<Operation> returnList = getDbaccess().getOneFromDBByUID(uid, Operation.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }


    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Operation.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Operation.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Operation.class, status));
    }

    private List<org.epos.eposdatamodel.Operation> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
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
