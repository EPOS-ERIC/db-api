package metadataapis;

import abstractapis.AbstractAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.Documentation;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class WebServiceAPI extends AbstractAPI<org.epos.eposdatamodel.WebService> {

    public WebServiceAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.WebService obj, StatusType overrideStatus) {

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

            Organization organization1 = (Organization) RelationChecker.checkRelation(obj.getProvider(), overrideStatus, Organization.class);

            edmobj.setProvider(organization1.getInstanceId());
        }

        /** CATEGORY **/
        if (obj.getCategory() != null && !obj.getCategory().isEmpty())
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty())
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** DOCUMENTATION **/
        if (obj.getDocumentation() != null && !obj.getDocumentation().isEmpty()) {
            for(LinkedEntity documentation : obj.getDocumentation()){
                Documentation documentation1 = (Documentation) RelationChecker.checkRelation(documentation, overrideStatus, Documentation.class);
                if(documentation1!=null) {
                    List<Element> el = dbaccess.getOneFromDBByInstanceId(documentation1.getInstanceId(), Element.class);
                    WebserviceElement pi = new WebserviceElement();
                    pi.setWebserviceInstance(edmobj);
                    pi.setElementInstance(el.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                Identifier identifier1 = (Identifier) RelationChecker.checkRelation(identifier, overrideStatus, Identifier.class);
                if(identifier1!=null) {
                    WebserviceIdentifier pi = new WebserviceIdentifier();
                    pi.setWebserviceInstance(edmobj);
                    pi.setIdentifierInstance(identifier1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SPATIAL **/
        if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity location : obj.getSpatialExtent()){
                Spatial spatial = (Spatial) RelationChecker.checkRelation(location, overrideStatus, Spatial.class);
                if(spatial!=null){
                    WebserviceSpatial pi = new WebserviceSpatial();
                    pi.setWebserviceInstance(edmobj);
                    pi.setSpatialInstance(spatial);
                }
            }
        }

        /** TEMPORAL **/
        if (obj.getTemporalExtent() != null && !obj.getTemporalExtent().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity periodOfTime : obj.getTemporalExtent()){
                Temporal temporal = (Temporal) RelationChecker.checkRelation(periodOfTime, overrideStatus, Temporal.class);
                if(temporal!=null){
                    WebserviceTemporal pi = new WebserviceTemporal();
                    pi.setWebserviceInstance(edmobj);
                    pi.setTemporalInstance(temporal);
                }
            }
        }

        if (obj.getSupportedOperation() != null && !obj.getSupportedOperation().isEmpty()) {
            for(LinkedEntity operation : obj.getSupportedOperation()){
                Operation operation1 = (Operation) RelationChecker.checkRelation(operation, overrideStatus, Operation.class);
                if(operation1!=null) {
                    OperationWebservice pi = new OperationWebservice();
                    pi.setWebserviceInstance(edmobj);
                    pi.setOperationInstance(operation1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getRelation() != null && !obj.getRelation().isEmpty()) {
            for(LinkedEntity relation : obj.getRelation()){
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
    public org.epos.eposdatamodel.WebService retrieve(String instanceId) {
        List<Webservice> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Webservice.class);
        if(elementList!=null && !elementList.isEmpty()) {
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

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceCategory.class)) {
                WebserviceCategory item = (WebserviceCategory) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    CategoryAPI api = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceContactpoint.class)) {
                WebserviceContactpoint item = (WebserviceContactpoint) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    ContactPointAPI api = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                }
            }

            if (edmobj.getProvider() != null) {
                OrganizationAPI api = new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class);
                o.setProvider(api.retrieveLinkedEntity(edmobj.getProvider()));
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceElement.class)) {
                WebserviceElement item = (WebserviceElement) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    DocumentationAPI api = new DocumentationAPI(EntityNames.DOCUMENTATION.name(), Documentation.class);
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.DOCUMENTATION.name())) o.addDocumentation(api.retrieveLinkedEntity(el.getInstanceId()));
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceIdentifier.class)) {
                WebserviceIdentifier item = (WebserviceIdentifier) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    IdentifierAPI api = new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceSpatial.class)) {
                WebserviceSpatial item = (WebserviceSpatial) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    SpatialAPI api = new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),WebserviceTemporal.class)) {
                WebserviceTemporal item = (WebserviceTemporal) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    TemporalAPI api = new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("webservice_instance_id", edmobj.getInstanceId(),OperationWebservice.class)) {
                OperationWebservice item = (OperationWebservice) object;
                if(item.getWebserviceInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    OperationAPI api = new OperationAPI(EntityNames.OPERATION.name(), Operation.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.addSupportedOperation(le);
                }
            }

            /** TODO: RELATION **/

            o = (org.epos.eposdatamodel.WebService) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.WebService> retrieveAll() {
        List<Webservice> list = getDbaccess().getAllFromDB(Webservice.class);
        List<org.epos.eposdatamodel.WebService> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
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
