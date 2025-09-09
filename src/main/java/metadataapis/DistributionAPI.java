package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Distribution;
import model.Element;
import model.Operation;
import org.epos.eposdatamodel.*;
import relationsapi.RelationChecker;
import usermanagementapis.UserGroupManagementAPI;
import utilities.OperationWebserviceInDistributionSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DistributionAPI extends AbstractAPI<org.epos.eposdatamodel.Distribution> {

    public DistributionAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Distribution obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Distribution> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Distribution) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Distribution edmobj = new Distribution();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setFormat(obj.getFormat());
        edmobj.setLicense(obj.getLicence());
        edmobj.setType(obj.getType());
        edmobj.setDatapolicy(obj.getDataPolicy());
        edmobj.setByteSize(obj.getByteSize());
        edmobj.setMaturity(obj.getMaturity());
        edmobj.setMediaType(obj.getMediaType());

        if (obj.getModified() != null)
            edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null)
            edmobj.setIssued(obj.getIssued());

        /** TITLE **/
        if (obj.getTitle() != null) {
            for(Object object : EposDataModelDAO.getInstance().getAllFromDB(DistributionTitle.class)){
                DistributionTitle title = (DistributionTitle) object;
                if(title.getDistributionInstance().getInstanceId().equals(obj.getInstanceId())){
                    EposDataModelDAO.getInstance().deleteObject(title);
                }
            }
            for(String title : obj.getTitle()){
                DistributionTitle pi = new DistributionTitle();
                pi.setInstanceId(UUID.randomUUID().toString());
                pi.setMetaId(UUID.randomUUID().toString());
                pi.setUid("Title/"+UUID.randomUUID().toString());
                pi.setVersion(null);
                pi.setTitle(title);
                pi.setDistributionInstance(edmobj);
                pi.setLang(null);
                EposDataModelDAO.getInstance().updateObject(pi);
            }
        }

        /** DESCRIPTION **/
        if (obj.getDescription() != null) {
            for(Object object : EposDataModelDAO.getInstance().getAllFromDB(DistributionDescription.class)){
                DistributionDescription title = (DistributionDescription) object;
                if(title.getDistributionInstance().getInstanceId().equals(obj.getInstanceId())){
                    EposDataModelDAO.getInstance().deleteObject(title);
                }
            }
            for(String description : obj.getDescription()){
                DistributionDescription pi = new DistributionDescription();
                pi.setInstanceId(UUID.randomUUID().toString());
                pi.setMetaId(UUID.randomUUID().toString());
                pi.setUid("Description/"+UUID.randomUUID().toString());
                pi.setVersion(null);
                pi.setDescription(description);
                pi.setDistributionInstance(edmobj);
                pi.setLang(null);
                EposDataModelDAO.getInstance().updateObject(pi);
            }
        }


        /** DATAPRODUCT **/
        if (obj.getDataProduct() != null) {
            if(relationFromUpdate!=null && obj.getDataProduct().contains(relationFromUpdate)){
                obj.getDataProduct().remove(relationFromUpdate);
                obj.getDataProduct().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getDataProduct()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class, false);
                if(dataproduct!=null){
                    DistributionDataproduct pi = new DistributionDataproduct();
                    pi.setDistributionInstance(edmobj);
                    pi.setDataproductInstance(dataproduct);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getSupportedOperation() != null ) {
            if(relationFromUpdate!=null && obj.getSupportedOperation().contains(relationFromUpdate)){
                obj.getSupportedOperation().remove(relationFromUpdate);
                obj.getSupportedOperation().add(relationToUpdate);
            }
            for(LinkedEntity supportedOperation : obj.getSupportedOperation()) {
                Operation operation = (Operation) RelationChecker.checkRelation(obj, previousObj, null, supportedOperation, overrideStatus, Operation.class, true);
                if(operation!=null){
                    OperationDistribution pi = new OperationDistribution();
                    pi.setDistributionInstance(edmobj);
                    pi.setOperationInstance(operation);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getAccessService() != null ) {
            if(relationFromUpdate!=null && obj.getAccessService().contains(relationFromUpdate)){
                obj.getAccessService().remove(relationFromUpdate);
                obj.getAccessService().add(relationToUpdate);
            }
            for(LinkedEntity accessService : obj.getAccessService()) {
                Webservice webservice = (Webservice) RelationChecker.checkRelation(obj, previousObj, null, accessService, overrideStatus, Webservice.class, true);
                if(webservice!=null){
                    WebserviceDistribution pi = new WebserviceDistribution();
                    pi.setDistributionInstance(edmobj);
                    pi.setWebserviceInstance(webservice);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if(obj.getAccessURL()!=null){
            for(String accessurl : obj.getAccessURL()) {
                createInnerElement(ElementType.ACCESSURL, accessurl, edmobj, overrideStatus);
            }
        }

        if(obj.getDownloadURL()!=null) {
            for (String downloadurl : obj.getDownloadURL()) {
                createInnerElement(ElementType.DOWNLOADURL, downloadurl, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Distribution edmobj, StatusType overrideStatus){
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
        DistributionElement ce = new DistributionElement();
        ce.setDistributionInstance(edmobj);
        ce.setElementInstance(el.get(0));
        EposDataModelDAO.getInstance().updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(DistributionTitle.class)){
            DistributionTitle item = (DistributionTitle) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DistributionElement.class)){
            DistributionElement item = (DistributionElement) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DistributionDescription.class)){
            DistributionDescription item = (DistributionDescription) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DistributionDataproduct.class)){
            DistributionDataproduct item = (DistributionDataproduct) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationDistribution.class)){
            OperationDistribution item = (OperationDistribution) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceDistribution.class)){
            WebserviceDistribution item = (WebserviceDistribution) object;
            if(item.getDistributionInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
        for(Distribution object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Distribution retrieve(String instanceId) {
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Distribution edmobj = elementList.get(0);
            org.epos.eposdatamodel.Distribution o = new org.epos.eposdatamodel.Distribution();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setType(edmobj.getType());
            o.setFormat(edmobj.getFormat());
            o.setLicence(edmobj.getLicense());
            o.setDataPolicy(edmobj.getDatapolicy());
            o.setIssued(
                    edmobj.getIssued()
            );
            o.setModified(
                    edmobj.getModified()
            );
            o.setType(edmobj.getType());
            o.setByteSize(edmobj.getByteSize());
            o.setMaturity(edmobj.getMaturity());
            o.setMediaType(edmobj.getMediaType());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),DistributionDescription.class)) {
                DistributionDescription item = (DistributionDescription) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    o.addDescription(item.getDescription());
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),DistributionTitle.class)) {
                DistributionTitle item = (DistributionTitle) object;
                if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    o.addTitle(item.getTitle());
                }
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),DistributionDataproduct.class)) {
                DistributionDataproduct item = (DistributionDataproduct) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproductInstance().getInstanceId());
                    o.addDataproduct(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),WebserviceDistribution.class)) {
                WebserviceDistribution item = (WebserviceDistribution) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
                    o.addAccessService(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),OperationDistribution.class)) {
                OperationDistribution item = (OperationDistribution) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    //System.out.println(item.getDistributionInstance().getInstanceId());
                    LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.addSupportedOperation(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(),DistributionElement.class)) {
                DistributionElement item = (DistributionElement) object;
                //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.ACCESSURL.name())) o.addAccessURL(el.getValue());
                    if (el.getType().equals(ElementType.DOWNLOADURL.name())) o.addDownloadURL(el.getValue());
                //}
            }

            o = (org.epos.eposdatamodel.Distribution) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Distribution.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Distribution.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Distribution.class, status));
    }

    private List<org.epos.eposdatamodel.Distribution> retrieveEntities(Function<Void, List<Distribution>> dbFetcher) {
        List<Distribution> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Distribution edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.DISTRIBUTION.name());

            return o;
        }
        return null;
    }

}
