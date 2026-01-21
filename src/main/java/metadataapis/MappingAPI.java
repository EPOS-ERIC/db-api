package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingAPI extends AbstractAPI<org.epos.eposdatamodel.Mapping> {

    public MappingAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Mapping obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Mapping> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Mapping selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Mapping item : returnList) {
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

        obj = (org.epos.eposdatamodel.Mapping) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Mapping edmobj = new Mapping();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setLabel(obj.getLabel());
        edmobj.setValuepattern(obj.getValuePattern());
        edmobj.setDefaultvalue(obj.getDefaultValue());
        edmobj.setMaxvalue(obj.getMaxValue());
        edmobj.setMinvalue(obj.getMinValue());
        edmobj.setMultipleValues(obj.getMultipleValues()!=null? obj.getMultipleValues() : "false");
        edmobj.setReadOnlyValue(obj.getReadOnlyValue()!=null? obj.getReadOnlyValue() : "false");
        edmobj.setRequired(Boolean.parseBoolean(obj.getRequired()));
        edmobj.setRange(obj.getRange());
        edmobj.setProperty(obj.getProperty());
        edmobj.setVariable(obj.getVariable());
        edmobj.setHealthcheckvalue(obj.getHealthCheckVariable());


        /** PARAM VALUE **/
        if(obj.getParamValue()!=null){
            for(Object object : getDbaccess().getAllFromDB(MappingElement.class)){
                MappingElement item = (MappingElement) object;
                if(item.getMappingInstance().getInstanceId().equals(obj.getInstanceId())){
                    EposDataModelDAO.getInstance().deleteObject(item);
                }
            }
            for(String paramvalue : obj.getParamValue()) {
                createInnerElement(ElementType.PARAMVALUE, paramvalue, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Mapping edmobj, StatusType overrideStatus){

        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        MappingElement ce = new MappingElement();
        ce.setMappingInstance(edmobj);
        ce.setElementInstance(el.get(0));
        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(MappingElement.class)){
            MappingElement item = (MappingElement) object;
            if(item.getMappingInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationMapping.class)){
            OperationMapping item = (OperationMapping) object;
            if(item.getMappingInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        for(Mapping object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Mapping retrieve(String instanceId) {
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Mapping edmobj = elementList.get(0);
            org.epos.eposdatamodel.Mapping o = new org.epos.eposdatamodel.Mapping();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setLabel(edmobj.getLabel());
            o.setValuePattern(edmobj.getValuepattern());
            o.setDefaultValue(edmobj.getDefaultvalue());
            o.setMaxValue(edmobj.getMaxvalue());
            o.setMinValue(edmobj.getMinvalue());
            o.setMultipleValues(edmobj.getMultipleValues());
            o.setReadOnlyValue(edmobj.getReadOnlyValue());
            o.setRequired(edmobj.getRequired()!=null? Boolean.toString(edmobj.getRequired()) : null );
            o.setRange(edmobj.getRange());
            o.setProperty(edmobj.getProperty());
            o.setVariable(edmobj.getVariable());
            o.setHealthCheckVariable(edmobj.getHealthcheckvalue());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("mappingInstance", edmobj.getInstanceId(),MappingElement.class)) {
                MappingElement item = (MappingElement) object;
                //if(item.getMappingInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.PARAMVALUE.name())) {
                        o.addParamValue(el.getValue());
                    }
                //}
            }

            o = (org.epos.eposdatamodel.Mapping) VersioningStatusAPI.retrieveVersion(o);

            return o;

    }

    @Override
    public org.epos.eposdatamodel.Mapping retrieveByUID(String uid) {
        List<Mapping> returnList = getDbaccess().getOneFromDBByUID(uid, Mapping.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Mapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Mapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Mapping.class, status));
    }

    private List<org.epos.eposdatamodel.Mapping> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Mapping edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.MAPPING.name());

            return o;
        }
        return null;
    }

}
