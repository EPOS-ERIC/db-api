package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import model.Person;
import org.epos.eposdatamodel.*;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.Identifier;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SoftwareApplicationAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplication> {

    public SoftwareApplicationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplication obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Softwareapplication> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.SoftwareApplication) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Softwareapplication edmobj = new Softwareapplication();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setDownloadurl(obj.getDownloadURL());
        edmobj.setInstallurl(obj.getInstallURL());
        edmobj.setLicenseurl(obj.getLicenseURL());
        edmobj.setMainentityofpage(obj.getMainEntityOfPage());
        edmobj.setRequirements(obj.getRequirements());
        edmobj.setKeywords(obj.getKeywords());
        edmobj.setSoftwareversion(obj.getSoftwareVersion());

        /** CATEGORY **/
        if (obj.getCategory() != null && !obj.getCategory().isEmpty())
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty())
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus);
                List<model.Identifier> identifierList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), model.Identifier.class);
                if(!identifierList.isEmpty()) {
                    SoftwareapplicationIdentifier pi = new SoftwareapplicationIdentifier();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getParameter() != null && !obj.getParameter().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity parameter : obj.getParameter()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(parameter, overrideStatus);
                List<model.Parameter> parameterList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), model.Parameter.class);
                if(!parameterList.isEmpty()) {
                    SoftwareapplicationParameter pi = new SoftwareapplicationParameter();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setParameterInstance(parameterList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }

        if (obj.getRelation() != null && !obj.getRelation().isEmpty()) {
            for(LinkedEntity relation : obj.getRelation()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(relation, overrideStatus);
                List<model.Operation> relationList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), model.Operation.class);
                if(!relationList.isEmpty()) {
                    SoftwareapplicationOperation pi = new SoftwareapplicationOperation();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setOperationInstance(relationList.get(0));
                    dbaccess.updateObject(pi);
                }
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
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationContactpoint.class)){
            SoftwareapplicationContactpoint item = (SoftwareapplicationContactpoint) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationIdentifier.class)){
            SoftwareapplicationIdentifier item = (SoftwareapplicationIdentifier) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationOperation.class)){
            SoftwareapplicationOperation item = (SoftwareapplicationOperation) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationParameter.class)){
            SoftwareapplicationParameter item = (SoftwareapplicationParameter) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationCategory.class)){
            SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        for(Softwareapplication object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.SoftwareApplication retrieve(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Softwareapplication edmobj = elementList.get(0);
            org.epos.eposdatamodel.SoftwareApplication o = new org.epos.eposdatamodel.SoftwareApplication();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setVersionId(edmobj.getVersion().getVersionId());
            o.setName(edmobj.getName());
            o.setDescription(edmobj.getDescription());
            o.setDownloadURL(edmobj.getDownloadurl());
            o.setInstallURL(edmobj.getInstallurl());
            o.addKeywords(edmobj.getKeywords());
            o.setLicenseURL(edmobj.getLicenseurl());
            o.setMainEntityOfPage(edmobj.getMainentityofpage());
            o.setRequirements(edmobj.getRequirements());
            o.setSoftwareVersion(edmobj.getSoftwareversion());


            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationCategory.class)) {
                SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    CategoryAPI api = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationContactpoint.class)) {
                SoftwareapplicationContactpoint item = (SoftwareapplicationContactpoint) object;
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    ContactPointAPI api = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationIdentifier.class)) {
                SoftwareapplicationIdentifier item = (SoftwareapplicationIdentifier) object;
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    IdentifierAPI api = new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationParameter.class)) {
                SoftwareapplicationParameter item = (SoftwareapplicationParameter) object;
                ParameterAPI api = new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), SoftwareapplicationParameter.class); //SAME FOR OUTPUT
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    if(item.getParameterInstance().getAction()!=null && item.getParameterInstance().getAction().equals("OBJECT"))
                        o.addInputParameter(api.retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
                    if(item.getParameterInstance().getAction()!=null && item.getParameterInstance().getAction().equals("RESULT"))
                        o.addOutputParameter(api.retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationOperation.class)) {
                SoftwareapplicationOperation item = (SoftwareapplicationOperation) object;
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    OperationAPI api = new OperationAPI(EntityNames.OPERATION.name(), model.Operation.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.addRelation(le);
                }
            }

            o = (org.epos.eposdatamodel.SoftwareApplication) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveBunch(List<String> entities) {
        List<Softwareapplication> list = getDbaccess().getListFromDBByInstanceId(entities, Softwareapplication.class);
        List<org.epos.eposdatamodel.SoftwareApplication> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAll() {
        List<Softwareapplication> list = getDbaccess().getAllFromDB(Softwareapplication.class);
        List<org.epos.eposdatamodel.SoftwareApplication> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAllWithStatus(StatusType status) {
        List<Softwareapplication> list = getDbaccess().getAllFromDBWithStatus(Softwareapplication.class, status);
        List<org.epos.eposdatamodel.SoftwareApplication> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Softwareapplication edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.SOFTWAREAPPLICATION.name());

            return o;
        }
        return null;
    }

}
