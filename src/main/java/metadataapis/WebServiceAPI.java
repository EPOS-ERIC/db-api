package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import model.Element;
import model.Identifier;
import model.Operation;
import model.Organization;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebServiceAPI extends AbstractAPI<org.epos.eposdatamodel.WebService> {

    public WebServiceAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(WebService obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Webservice> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Webservice edmobj = new Webservice();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setEntrypoint(obj.getEntryPoint());
        edmobj.setKeywords(String.join("\\|", Optional.ofNullable(obj.getKeywords()).orElse("")));
        edmobj.setLicense(obj.getLicense());
        edmobj.setAaaitypes(obj.getAaaiTypes());

        if (obj.getDatePublished() != null)
            edmobj.setDatapublished(obj.getDatePublished());
        if (obj.getDateModified() != null)
            edmobj.setDatamodified(obj.getDateModified());


        getDbaccess().updateObject(edmobj);

        /** PUBLISHER **/
        if (obj.getProvider() != null) {
            if(relationFromUpdate!=null && obj.getProvider().equals(relationFromUpdate)){
                obj.setProvider(relationToUpdate);
            }

            Organization organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, obj.getProvider(), overrideStatus, Organization.class, false);

            edmobj.setProvider(organization1.getInstanceId());
        }

        /** CATEGORY **/
        if (obj.getCategory() != null)
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null)
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** DOCUMENTATION **/
        if (obj.getDocumentation() != null) {
            if(relationFromUpdate!=null && obj.getDocumentation().contains(relationFromUpdate)){
                obj.getDocumentation().remove(relationFromUpdate);
                obj.getDocumentation().add(relationToUpdate);
            }
            for(LinkedEntity documentation : obj.getDocumentation()){
                Element documentation1 = (Element) RelationChecker.checkRelation(obj, previousObj, null, documentation, overrideStatus, Element.class, false);
                if(documentation1!=null) {
                    List<Element> el = dbaccess.getOneFromDBByInstanceId(documentation1.getInstanceId(), Element.class);
                    WebserviceElement pi = new WebserviceElement();
                    pi.setWebserviceInstance(edmobj);
                    pi.setElementInstance(el.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getIdentifier() != null) {
            if(relationFromUpdate!=null && obj.getIdentifier().contains(relationFromUpdate)){
                obj.getIdentifier().remove(relationFromUpdate);
                obj.getIdentifier().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                Identifier identifier1 = (Identifier) RelationChecker.checkRelation(obj, previousObj, null, identifier, overrideStatus, Identifier.class, false);
                if(identifier1!=null) {
                    WebserviceIdentifier pi = new WebserviceIdentifier();
                    pi.setWebserviceInstance(edmobj);
                    pi.setIdentifierInstance(identifier1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SPATIAL **/
        if (obj.getSpatialExtent() != null) {
            if(relationFromUpdate!=null && obj.getSpatialExtent().contains(relationFromUpdate)){
                obj.getSpatialExtent().remove(relationFromUpdate);
                obj.getSpatialExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity location : obj.getSpatialExtent()){
                Spatial spatial = (Spatial) RelationChecker.checkRelation(obj, previousObj, null, location, overrideStatus, Spatial.class, false);
                if(spatial!=null){
                    WebserviceSpatial pi = new WebserviceSpatial();
                    pi.setWebserviceInstance(edmobj);
                    pi.setSpatialInstance(spatial);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** TEMPORAL **/
        if (obj.getTemporalExtent() != null) {
            if(relationFromUpdate!=null && obj.getTemporalExtent().contains(relationFromUpdate)){
                obj.getTemporalExtent().remove(relationFromUpdate);
                obj.getTemporalExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity periodOfTime : obj.getTemporalExtent()){
                Temporal temporal = (Temporal) RelationChecker.checkRelation(obj, previousObj, null, periodOfTime, overrideStatus, Temporal.class, false);
                if(temporal!=null){
                    WebserviceTemporal pi = new WebserviceTemporal();
                    pi.setWebserviceInstance(edmobj);
                    pi.setTemporalInstance(temporal);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getDistribution() != null) {
            if(relationFromUpdate!=null && obj.getSupportedOperation().contains(relationFromUpdate)){
                obj.getSupportedOperation().remove(relationFromUpdate);
                obj.getSupportedOperation().add(relationToUpdate);
            }
            for(LinkedEntity distribution : obj.getDistribution()){
                model.Distribution distribution1 = (model.Distribution) RelationChecker.checkRelation(obj, previousObj, null, distribution, overrideStatus, model.Distribution.class, false);
                if(distribution1!=null) {
                    WebserviceDistribution pi = new WebserviceDistribution();
                    pi.setWebserviceInstance(edmobj);
                    pi.setDistributionInstance(distribution1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getSupportedOperation() != null) {
            if(relationFromUpdate!=null && obj.getSupportedOperation().contains(relationFromUpdate)){
                obj.getSupportedOperation().remove(relationFromUpdate);
                obj.getSupportedOperation().add(relationToUpdate);
            }
            for(LinkedEntity operation : obj.getSupportedOperation()){
                Operation operation1 = (Operation) RelationChecker.checkRelation(obj, previousObj, null, operation, overrideStatus, Operation.class, true);
                if(operation1!=null) {
                    OperationWebservice pi = new OperationWebservice();
                    pi.setWebserviceInstance(edmobj);
                    pi.setOperationInstance(operation1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getWebserviceRelation() != null) {
            for(LinkedEntity relation : obj.getWebserviceRelation()){
                WebserviceRelation pi = new WebserviceRelation();
                pi.setWebservice(edmobj);
                pi.setEntityInstanceId(relation.getInstanceId());
                pi.setResourceEntity(EntityNames.valueOf(relation.getEntityType()).name());
                pi.setWebserviceInstanceId(edmobj.getInstanceId());
                dbaccess.updateObject(pi);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(WebserviceIdentifier.class)){
            WebserviceIdentifier item = (WebserviceIdentifier) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceTemporal.class)){
            WebserviceTemporal item = (WebserviceTemporal) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceElement.class)){
            WebserviceElement item = (WebserviceElement) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceSpatial.class)){
            WebserviceSpatial item = (WebserviceSpatial) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceContactpoint.class)){
            WebserviceContactpoint item = (WebserviceContactpoint) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceRelation.class)){
            WebserviceRelation item = (WebserviceRelation) object;
            if(item.getWebservice().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceDistribution.class)){
            WebserviceDistribution item = (WebserviceDistribution) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceCategory.class)){
            WebserviceCategory item = (WebserviceCategory) object;
            if(item.getWebserviceInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        for(Webservice object : elementList){
            dbaccess.deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.WebService retrieve(String instanceId) {
        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Webservice edmobj = elementList.get(0);
            org.epos.eposdatamodel.WebService o = new org.epos.eposdatamodel.WebService();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setDateModified(
                    edmobj.getDatamodified()
            );
            o.setDatePublished(
                    edmobj.getDatapublished()
            );
            o.setDescription(edmobj.getDescription());
            o.setEntryPoint(edmobj.getEntrypoint());
            o.setLicense(edmobj.getLicense());
            o.setName(edmobj.getName());
            o.setAaaiTypes(edmobj.getAaaitypes());
            if(edmobj.getKeywords()!=null && !edmobj.getKeywords().isBlank())
                for(String item : edmobj.getKeywords().split("\\|"))
                    o.addKeywords(item);

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceCategory.class)) {
                WebserviceCategory item = (WebserviceCategory) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceContactpoint.class)) {
                WebserviceContactpoint item = (WebserviceContactpoint) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                //}
            }

            if (edmobj.getProvider() != null) {
                o.setProvider(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(edmobj.getProvider()));
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceElement.class)) {
                WebserviceElement item = (WebserviceElement) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.DOCUMENTATION.name())) o.addDocumentation(retrieveAPI(EntityNames.DOCUMENTATION.name()).retrieveLinkedEntity(el.getInstanceId()));
                // }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceIdentifier.class)) {
                WebserviceIdentifier item = (WebserviceIdentifier) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                // }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceSpatial.class)) {
                WebserviceSpatial item = (WebserviceSpatial) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),WebserviceTemporal.class)) {
                WebserviceTemporal item = (WebserviceTemporal) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
                    o.addTemporalExtent(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstance", edmobj.getInstanceId(),OperationWebservice.class)) {
                OperationWebservice item = (OperationWebservice) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.addSupportedOperation(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webserviceInstanceId", edmobj.getInstanceId(),WebserviceRelation.class)) {
                WebserviceRelation item = (WebserviceRelation) object;
                //if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                if(item.getResourceEntity().equalsIgnoreCase(EntityNames.WEBSERVICE.name())) {
                    LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getEntityInstanceId());
                    o.addSupportedOperation(le);
                }
                //}
        }

            o = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Webservice.class));
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Webservice.class));
    }
    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Webservice.class, status));
    }

    protected List<org.epos.eposdatamodel.WebService> retrieveEntities(Function<Void, List<Webservice>> dbFetcher) {
        List<Webservice> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Webservice edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.WEBSERVICE.name());

            return o;
        }
        return null;
    }

}
