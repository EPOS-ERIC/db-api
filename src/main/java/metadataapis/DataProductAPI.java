package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DataProductAPI extends AbstractAPI<org.epos.eposdatamodel.DataProduct> {

    public DataProductAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(DataProduct obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Dataproduct> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Dataproduct edmobj = new Dataproduct();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setKeywords(String.join("\\|", Optional.ofNullable(obj.getKeywords()).orElse("")));
        edmobj.setAccessright(obj.getAccessRight());
        edmobj.setAccrualperiodicity(obj.getAccrualPeriodicity());
        edmobj.setType(obj.getType());
        edmobj.setVersioninfo(obj.getVersionInfo());
        edmobj.setDocumentation(obj.getDocumentation());
        edmobj.setQualityassurance(obj.getQualityAssurance());
        edmobj.setHasQualityAnnotation(obj.getHasQualityAnnotation());
        edmobj.setAccessright(obj.getAccessRight());

        if (obj.getCreated() != null)
            edmobj.setCreated(obj.getCreated());
        if (obj.getModified() != null)
            edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null)
            edmobj.setIssued(obj.getIssued());

        /** CATEGORY **/
        if (obj.getCategory() != null && !obj.getCategory().isEmpty())
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty())
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** TITLE **/
        if (obj.getTitle() != null && !obj.getTitle().isEmpty()) {

            for(String title : obj.getTitle()){
                DataproductTitle pi = new DataproductTitle();
                pi.setInstanceId(UUID.randomUUID().toString());
                pi.setMetaId(UUID.randomUUID().toString());
                pi.setUid("Title/"+UUID.randomUUID().toString());
                pi.setVersion(null); //TODO: Fix version
                pi.setTitle(title);
                pi.setDataproductInstance(edmobj);
                pi.setLang(null);

                dbaccess.updateObject(pi);
            }
        }

        /** DESCRIPTION **/
        if (obj.getDescription() != null && !obj.getDescription().isEmpty()) {

            for(String description : obj.getDescription()){
                DataproductDescription pi = new DataproductDescription();
                pi.setInstanceId(UUID.randomUUID().toString());
                pi.setMetaId(UUID.randomUUID().toString());
                pi.setUid("Description/"+UUID.randomUUID().toString());
                pi.setVersion(null);
                pi.setDescription(description);
                pi.setDataproductInstance(edmobj);
                pi.setLang(null);
                dbaccess.updateObject(pi);
            }
        }

        /** HASPART **/
        if (obj.getHasPart() != null && !obj.getHasPart().isEmpty()) {
            if(relationFromUpdate!=null && obj.getHasPart().contains(relationFromUpdate)){
                obj.getHasPart().remove(relationFromUpdate);
                obj.getHasPart().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getHasPart()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class);
                if(dataproduct!=null) {
                    DataproductHaspart pi = new DataproductHaspart();
                    pi.setDataproduct1Instance(edmobj);
                    pi.setDataproduct2Instance(dataproduct);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** ISPARTOF **/
        if (obj.getIsPartOf() != null && !obj.getIsPartOf().isEmpty()) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getIsPartOf()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class);
                if(dataproduct!=null) {
                    DataproductIspartof pi = new DataproductIspartof();
                    pi.setDataproduct1Instance(edmobj);
                    pi.setDataproduct2Instance(dataproduct);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null && !obj.getIdentifier().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity identifier : obj.getIdentifier()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(identifier, overrideStatus);
                List<Identifier> identifierList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(),Identifier.class);
                if(!identifierList.isEmpty()) {
                    DataproductIdentifier pi = new DataproductIdentifier();
                    pi.setDataproductInstance(edmobj);
                    pi.setIdentifierInstance(identifierList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
        /** PROVENANCE **/
        if (obj.getProvenance() != null && !obj.getProvenance().isEmpty()) {
            for(String provenance : obj.getProvenance()){
                DataproductProvenance pi = new DataproductProvenance();
                pi.setInstanceId(UUID.randomUUID().toString());
                pi.setMetaId(UUID.randomUUID().toString());
                pi.setUid("Title/"+UUID.randomUUID().toString());
                pi.setVersion(null);
                pi.setProvenance(provenance);
                pi.setDataproductInstance(edmobj);
                dbaccess.updateObject(pi);
            }
        }

        /** PUBLISHER **/
        if (obj.getPublisher() != null && !obj.getPublisher().isEmpty()) {
            if(relationFromUpdate!=null && obj.getPublisher().contains(relationFromUpdate)){
                obj.getPublisher().remove(relationFromUpdate);
                obj.getPublisher().add(relationToUpdate);
            }
            for(LinkedEntity organization : obj.getPublisher()){
                Organization organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, organization, overrideStatus, Organization.class);
                if(organization1!=null) {
                    DataproductPublisher pi = new DataproductPublisher();
                    pi.setDataproductInstance(edmobj);
                    pi.setOrganizationInstance(organization1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** DISTRIBUTION **/
        if (obj.getDistribution() != null && !obj.getDistribution().isEmpty()) {
            if(relationFromUpdate!=null && obj.getDistribution().contains(relationFromUpdate)){
                obj.getDistribution().remove(relationFromUpdate);
                obj.getDistribution().add(relationToUpdate);
            }
            for(LinkedEntity distribution : obj.getDistribution()){
                Distribution distribution1 = (Distribution) RelationChecker.checkRelation(obj, previousObj, null, distribution, overrideStatus, Distribution.class);
                if(distribution1!=null) {
                    DistributionDataproduct pi = new DistributionDataproduct();
                    pi.setDataproductInstance(edmobj);
                    pi.setDistributionInstance(distribution1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SPATIAL **/
        if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
            if(relationFromUpdate!=null && obj.getSpatialExtent().contains(relationFromUpdate)){
                obj.getSpatialExtent().remove(relationFromUpdate);
                obj.getSpatialExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity location : obj.getSpatialExtent()){
                Spatial spatial = (Spatial) RelationChecker.checkRelation(obj, previousObj, null, location, overrideStatus, Spatial.class);
                if(spatial!=null){
                    DataproductSpatial pi = new DataproductSpatial();
                    pi.setDataproductInstance(edmobj);
                    pi.setSpatialInstance(spatial);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** TEMPORAL **/
        if (obj.getTemporalExtent() != null && !obj.getTemporalExtent().isEmpty()) {
            if(relationFromUpdate!=null && obj.getTemporalExtent().contains(relationFromUpdate)){
                obj.getTemporalExtent().remove(relationFromUpdate);
                obj.getTemporalExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity periodOfTime : obj.getTemporalExtent()){
                Temporal temporal = (Temporal) RelationChecker.checkRelation(obj, previousObj, null, periodOfTime, overrideStatus, Temporal.class);
                if(temporal!=null){
                    DataproductTemporal pi = new DataproductTemporal();
                    pi.setDataproductInstance(edmobj);
                    pi.setTemporalInstance(temporal);
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
        for(Object object : getDbaccess().getAllFromDB(DataproductContactpoint.class)){
            DataproductContactpoint item = (DataproductContactpoint) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductCategory.class)){
            DataproductCategory item = (DataproductCategory) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductIspartof.class)){
            DataproductIspartof item = (DataproductIspartof) object;
            if(item.getDataproduct1Instance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductIdentifier.class)){
            DataproductIdentifier item = (DataproductIdentifier) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductTitle.class)){
            DataproductTitle item = (DataproductTitle) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductTemporal.class)){
            DataproductTemporal item = (DataproductTemporal) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductSpatial.class)){
            DataproductSpatial item = (DataproductSpatial) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductRelation.class)){
            DataproductRelation item = (DataproductRelation) object;
            if(item.getDataproduct().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(DataproductPublisher.class)){
            DataproductPublisher item = (DataproductPublisher) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(DataproductProvenance.class)){
            DataproductProvenance item = (DataproductProvenance) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(DataproductHaspart.class)){
            DataproductHaspart item = (DataproductHaspart) object;
            if(item.getDataproduct1Instance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(DataproductDescription.class)){
            DataproductDescription item = (DataproductDescription) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }


        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        for(Dataproduct object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.DataProduct retrieve(String instanceId) {
        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Dataproduct edmobj = elementList.get(0);
            org.epos.eposdatamodel.DataProduct o = new org.epos.eposdatamodel.DataProduct();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setType(edmobj.getType());
            o.setAccrualPeriodicity(edmobj.getAccrualperiodicity());
            o.setHasQualityAnnotation(edmobj.getHasQualityAnnotation());
            o.setCreated(
                    edmobj.getCreated()
            );
            o.setIssued(
                    edmobj.getIssued()
            );
            o.setModified(
                    edmobj.getModified()
            );
            o.setType(edmobj.getType());
            o.setVersionInfo(edmobj.getVersioninfo());
            o.setDocumentation(edmobj.getDocumentation());
            o.setQualityAssurance(edmobj.getQualityassurance());
            o.setAccessRight(edmobj.getAccessright());

            if(edmobj.getKeywords()!=null && !edmobj.getKeywords().isBlank())
                for(String item : edmobj.getKeywords().split("\\|"))
                    o.addKeywords(item);

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductCategory.class)) {
                DataproductCategory item = (DataproductCategory) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    CategoryAPI api = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductContactpoint.class)) {
                DataproductContactpoint item = (DataproductContactpoint) object;
                System.out.println("RETRIEVE CONTACT POINT: "+item);
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    ContactPointAPI api = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    System.out.println("RETRIEVE CONTACT POINT: "+le);
                    o.addContactPoint(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductDescription.class)) {
                DataproductDescription item = (DataproductDescription) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    o.addDescription(item.getDescription());
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductTitle.class)) {
                DataproductTitle item = (DataproductTitle) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    o.addTitle(item.getTitle());
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductIdentifier.class)) {
                DataproductIdentifier item = (DataproductIdentifier) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    IdentifierAPI api = new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                    o.addIdentifier(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct1_instance_id", edmobj.getInstanceId(),DataproductHaspart.class)) {
                DataproductHaspart item = (DataproductHaspart) object;
                if(item.getDataproduct1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                    DataProductAPI api = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId());
                    o.addHasPart(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct1_instance_id", edmobj.getInstanceId(),DataproductIspartof.class)) {
                DataproductIspartof item = (DataproductIspartof) object;
                if(item.getDataproduct1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                    DataProductAPI api = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId());
                    o.addHasPart(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductProvenance.class)) {
                DataproductProvenance item = (DataproductProvenance) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    o.addDescription(item.getProvenance());
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductPublisher.class)) {
                DataproductPublisher item = (DataproductPublisher) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    OrganizationAPI api = new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId());
                    o.addPublisher(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DistributionDataproduct.class)) {
                DistributionDataproduct item = (DistributionDataproduct) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    DistributionAPI api = new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getDistributionInstance().getInstanceId());
                    o.addDistribution(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductSpatial.class)) {
                DataproductSpatial item = (DataproductSpatial) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    SpatialAPI api = new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct_instance_id", edmobj.getInstanceId(),DataproductTemporal.class)) {
                DataproductTemporal item = (DataproductTemporal) object;
                if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    TemporalAPI api = new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class);
                    LinkedEntity le = api.retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                }
            }

            o = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.DataProduct> retrieveAll() {
        List<Dataproduct> list = getDbaccess().getAllFromDB(Dataproduct.class);
        List<org.epos.eposdatamodel.DataProduct> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Dataproduct edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.DATAPRODUCT.name());

            return o;
        }
        return null;
    }

}
