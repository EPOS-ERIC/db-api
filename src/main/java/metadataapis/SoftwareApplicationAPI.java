package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Address;
import model.Element;
import model.Person;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SoftwareApplicationAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplication> {

    public SoftwareApplicationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplication obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Softwareapplication> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Softwareapplication selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Softwareapplication item : returnList) {
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
        edmobj.setSoftwareStatus(obj.getSoftwareStatus());
        edmobj.setFileSize(obj.getFileSize());
        edmobj.setSpatial(obj.getSpatial());
        edmobj.setTemporal(obj.getTemporal());
        edmobj.setMemoryrequirements(obj.getMemoryrequirements());
        edmobj.setProcessorRequirements(obj.getProcessorRequirements());
        edmobj.setStorageRequirements(obj.getStorageRequirements());
        edmobj.setTimeRequired(obj.getTimeRequired());

        /** CATEGORY **/
        if (obj.getCategory() != null)
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null)
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                List<model.Identifier> identifierList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), model.Identifier.class);
                if(!identifierList.isEmpty()) {
                    SoftwareapplicationIdentifier pi = new SoftwareapplicationIdentifier();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getParameter() != null) {
            for(org.epos.eposdatamodel.LinkedEntity parameter : obj.getParameter()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(parameter, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                List<model.Parameter> parameterList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), model.Parameter.class);
                if(!parameterList.isEmpty()) {
                    SoftwareapplicationParameter pi = new SoftwareapplicationParameter();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setParameterInstance(parameterList.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getRelatedOperation() != null) {
            for(LinkedEntity relation : obj.getRelatedOperation()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(relation, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                List<model.Operation> relationList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), model.Operation.class);
                if(!relationList.isEmpty()) {
                    SoftwareapplicationOperation pi = new SoftwareapplicationOperation();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setOperationInstance(relationList.get(0));
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getCitation() != null) {
            for(String citation : obj.getCitation()) {
                createInnerElement(ElementType.CITATION, citation, edmobj, overrideStatus);
            }
        }

        if (obj.getOperatingSystem() != null) {
            for(String operatingSystem : obj.getOperatingSystem()) {
                createInnerElement(ElementType.OPERATINGSYSTEM, operatingSystem, edmobj, overrideStatus);
            }
        }

        if (obj.getAuthor() != null) {
            for(LinkedEntity owns : obj.getAuthor()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationAuthor pi = new SoftwareapplicationAuthor();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getContributor() != null) {
            for(LinkedEntity owns : obj.getContributor()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationContributor pi = new SoftwareapplicationContributor();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getFunder() != null) {
            for(LinkedEntity owns : obj.getFunder()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationFunder pi = new SoftwareapplicationFunder();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getMaintainer() != null) {
            for(LinkedEntity owns : obj.getMaintainer()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationMaintainer pi = new SoftwareapplicationMaintainer();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getProvider() != null) {
            for(LinkedEntity owns : obj.getProvider()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationProvider pi = new SoftwareapplicationProvider();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getPublisher() != null) {
            for(LinkedEntity owns : obj.getPublisher()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationPublisher pi = new SoftwareapplicationPublisher();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getCreator() != null) {
            for(LinkedEntity owns : obj.getCreator()) {
                LinkedEntity resolvedEntity = LinkedEntityAPI.createFromLinkedEntity(owns, overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
                if (resolvedEntity != null && resolvedEntity.getInstanceId() != null) {
                    SoftwareapplicationCreator pi = new SoftwareapplicationCreator();
                    pi.setSoftwareapplication(edmobj);
                    pi.setSoftwareapplicationInstanceId(edmobj.getInstanceId());
                    pi.setResourceEntity(resolvedEntity.getEntityType());
                    pi.setEntityInstanceId(resolvedEntity.getInstanceId());
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Softwareapplication edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        ElementAPI api = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);
        LinkedEntity le = api.create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        SoftwareapplicationElement ce = new SoftwareapplicationElement();
        ce.setSoftwareapplicationInstance(edmobj);
        ce.setElementInstance(el.get(0));
        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationContactpoint.class)){
            SoftwareapplicationContactpoint item = (SoftwareapplicationContactpoint) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationIdentifier.class)){
            SoftwareapplicationIdentifier item = (SoftwareapplicationIdentifier) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationOperation.class)){
            SoftwareapplicationOperation item = (SoftwareapplicationOperation) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationParameter.class)){
            SoftwareapplicationParameter item = (SoftwareapplicationParameter) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationCategory.class)){
            SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationElement.class)){
            SoftwareapplicationElement item = (SoftwareapplicationElement) object;
            if(item.getSoftwareapplicationInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationAuthor.class)){
            SoftwareapplicationAuthor item = (SoftwareapplicationAuthor) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationContributor.class)){
            SoftwareapplicationContributor item = (SoftwareapplicationContributor) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationFunder.class)){
            SoftwareapplicationFunder item = (SoftwareapplicationFunder) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationMaintainer.class)){
            SoftwareapplicationMaintainer item = (SoftwareapplicationMaintainer) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationProvider.class)){
            SoftwareapplicationProvider item = (SoftwareapplicationProvider) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationPublisher.class)){
            SoftwareapplicationPublisher item = (SoftwareapplicationPublisher) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationCreator.class)){
            SoftwareapplicationCreator item = (SoftwareapplicationCreator) object;
            if(item.getSoftwareapplication().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }

        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        for(Softwareapplication object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.SoftwareApplication retrieve(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
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
            o.setSoftwareStatus(edmobj.getSoftwareStatus());
            o.setFileSize(edmobj.getFileSize());
            o.setSpatial(edmobj.getSpatial());
            o.setTemporal(edmobj.getTemporal());
            o.setMemoryrequirements(edmobj.getMemoryrequirements());
            o.setProcessorRequirements(edmobj.getProcessorRequirements());
            o.setStorageRequirements(edmobj.getStorageRequirements());
            o.setTimeRequired(edmobj.getTimeRequired());


            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationCategory.class)) {
                SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
                // if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                // }
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationContactpoint.class)) {
                SoftwareapplicationContactpoint item = (SoftwareapplicationContactpoint) object;
                //if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationIdentifier.class)) {
                SoftwareapplicationIdentifier item = (SoftwareapplicationIdentifier) object;
                //if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationParameter.class)) {
                SoftwareapplicationParameter item = (SoftwareapplicationParameter) object;
                //if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    if(item.getParameterInstance().getAction()!=null && item.getParameterInstance().getAction().equals("OBJECT"))
                        o.addInputParameter(retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()).retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
                    if(item.getParameterInstance().getAction()!=null && item.getParameterInstance().getAction().equals("RESULT"))
                        o.addOutputParameter(retrieveAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name()).retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationOperation.class)) {
                SoftwareapplicationOperation item = (SoftwareapplicationOperation) object;
                //if(item.getSoftwareapplicationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le =  retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.addRelatedOperation(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(),SoftwareapplicationElement.class)) {
                SoftwareapplicationElement item = (SoftwareapplicationElement) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                Element el = item.getElementInstance();
                if (el.getType().equals(ElementType.CITATION.name())) o.addCitation(el.getValue());
                if (el.getType().equals(ElementType.OPERATINGSYSTEM.name())) o.addOperatingSystem(el.getValue());
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationAuthor.class)) {
                SoftwareapplicationAuthor item = (SoftwareapplicationAuthor) object;
                if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                    o.addAuthor(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
                if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                    o.addAuthor(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationContributor.class)) {
                SoftwareapplicationContributor item = (SoftwareapplicationContributor) object;
                if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                    o.addContributor(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
                if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                    o.addContributor(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationFunder.class)) {
                SoftwareapplicationFunder item = (SoftwareapplicationFunder) object;
                if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                    o.addFunder(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
                if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                    o.addFunder(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                }
            }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationMaintainer.class)) {
            SoftwareapplicationMaintainer item = (SoftwareapplicationMaintainer) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addMaintainer(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addMaintainer(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationProvider.class)) {
            SoftwareapplicationProvider item = (SoftwareapplicationProvider) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addProvider(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addProvider(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationPublisher.class)) {
            SoftwareapplicationPublisher item = (SoftwareapplicationPublisher) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addPublisher(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addPublisher(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("softwareapplication", edmobj.getInstanceId(),SoftwareapplicationCreator.class)) {
            SoftwareapplicationCreator item = (SoftwareapplicationCreator) object;
            if(item.getResourceEntity().equals(EntityNames.PERSON.name())){
                o.addCreator(retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(item.getResourceEntity().equals(EntityNames.ORGANIZATION.name())){
                o.addCreator(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        o = (org.epos.eposdatamodel.SoftwareApplication) VersioningStatusAPI.retrieveVersion(o);

            return o;

    }


    @Override
    public org.epos.eposdatamodel.SoftwareApplication retrieveByUID(String uid) {
        List<Softwareapplication> returnList = getDbaccess().getOneFromDBByUID(uid, Softwareapplication.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwareapplication.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwareapplication.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwareapplication.class, status));
    }

    private List<org.epos.eposdatamodel.SoftwareApplication> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
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
