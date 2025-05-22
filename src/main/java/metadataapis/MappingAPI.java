package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
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
        List<Mapping> returnList = getDbaccess().getOneFromDB(
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
                    dbaccess.deleteObject(item);
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
        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        MappingElement ce = new MappingElement();
        ce.setMappingInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(MappingElement.class)){
            MappingElement item = (MappingElement) object;
            if(item.getMappingInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationMapping.class)){
            OperationMapping item = (OperationMapping) object;
            if(item.getMappingInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Mapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Mapping.class);
        for(Mapping object : elementList){
            dbaccess.deleteObject(object);
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
            o.setRequired(Boolean.toString(edmobj.getRequired()));
            o.setRange(edmobj.getRange());
            o.setProperty(edmobj.getProperty());
            o.setVariable(edmobj.getVariable());
            o.setHealthCheckVariable(edmobj.getHealthcheckvalue());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("mappingInstance", edmobj.getInstanceId(),MappingElement.class)) {
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
    public List<org.epos.eposdatamodel.Mapping> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Mapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Mapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Mapping> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Mapping.class, status));
    }

    private List<org.epos.eposdatamodel.Mapping> retrieveEntities(Function<Void, List<Mapping>> dbFetcher) {
        List<Mapping> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
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
