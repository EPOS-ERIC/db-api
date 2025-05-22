package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import model.Attribution;
import model.Distribution;
import model.Element;
import model.Identifier;
import model.Organization;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        edmobj.setAccessright(obj.getAccessRight());

        if (obj.getCreated() != null)
            edmobj.setCreated(obj.getCreated());
        if (obj.getModified() != null)
            edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null)
            edmobj.setIssued(obj.getIssued());

        /** CATEGORY **/
        if (obj.getCategory() != null)
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null)
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** TITLE **/
        if (obj.getTitle() != null) {

            for(Object object : dbaccess.getAllFromDB(DataproductTitle.class)){
                DataproductTitle title = (DataproductTitle) object;
                if(title.getDataproductInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(title);
                }
            }

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
        if (obj.getDescription() != null) {

            for(Object object : dbaccess.getAllFromDB(DataproductDescription.class)){
                DataproductDescription description = (DataproductDescription) object;
                if(description.getDataproductInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(description);
                }
            }

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

        /** QUALIFIEDATTRIBUTION **/
        if (obj.getQualifiedAttribution() != null) {
            if(relationFromUpdate!=null && obj.getQualifiedAttribution().contains(relationFromUpdate)){
                obj.getQualifiedAttribution().remove(relationFromUpdate);
                obj.getQualifiedAttribution().add(relationToUpdate);
            }
            for(LinkedEntity attributionLE : obj.getQualifiedAttribution()){
                Attribution attribution = (Attribution) RelationChecker.checkRelation(obj, previousObj, null, attributionLE, overrideStatus, Attribution.class, false);
                if(attribution!=null) {
                    DataproductAttribution pi = new DataproductAttribution();
                    pi.setDataproductInstance(edmobj);
                    pi.setAttributionInstance(attribution);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SOURCE **/
        if (obj.getSource() != null) {
            if(relationFromUpdate!=null && obj.getSource().contains(relationFromUpdate)){
                obj.getSource().remove(relationFromUpdate);
                obj.getSource().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getSource()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class, false);
                if(dataproduct!=null) {
                    DataproductSource pi = new DataproductSource();
                    pi.setDataproduct1Instance(edmobj);
                    pi.setDataproduct2Instance(dataproduct);
                    dbaccess.updateObject(pi);
                }
            }
        }


        /** HASPART **/
        if (obj.getHasPart() != null) {
            if(relationFromUpdate!=null && obj.getHasPart().contains(relationFromUpdate)){
                obj.getHasPart().remove(relationFromUpdate);
                obj.getHasPart().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getHasPart()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class, false);
                if(dataproduct!=null) {
                    DataproductHaspart pi = new DataproductHaspart();
                    pi.setDataproduct1Instance(edmobj);
                    pi.setDataproduct2Instance(dataproduct);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** ISPARTOF **/
        if (obj.getIsPartOf() != null) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }
            for(LinkedEntity dataProduct : obj.getIsPartOf()){
                Dataproduct dataproduct = (Dataproduct) RelationChecker.checkRelation(obj, previousObj, null, dataProduct, overrideStatus, Dataproduct.class, false);
                if(dataproduct!=null) {
                    DataproductIspartof pi = new DataproductIspartof();
                    pi.setDataproduct1Instance(dataproduct);
                    pi.setDataproduct2Instance(edmobj);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** IDENTIFIER **/
        if (obj.getIdentifier() != null) {
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
        if (obj.getProvenance() != null) {
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
        if (obj.getPublisher() != null) {
            if(relationFromUpdate!=null && obj.getPublisher().contains(relationFromUpdate)){
                obj.getPublisher().remove(relationFromUpdate);
                obj.getPublisher().add(relationToUpdate);
            }
            for (LinkedEntity organization : obj.getPublisher()) {
                Organization organization1 = null;

                if (obj.getStatus()!=null && obj.getStatus().equals(StatusType.DRAFT)) {
                    List<Organization> tempOrganization = (List<Organization>) getDbaccess().getOneFromDBByLinkedEntity(organization, Organization.class);
                    if (tempOrganization != null && !tempOrganization.isEmpty()) {
                        organization1 = tempOrganization.get(0);
                    }
                }

                if (organization1 == null) {
                    organization1 = (Organization) RelationChecker.checkRelation(obj, previousObj, null, organization, overrideStatus, Organization.class, false);
                }

                if (organization1 != null) {
                    DataproductPublisher pi = new DataproductPublisher();
                    pi.setDataproductInstance(edmobj);
                    pi.setOrganizationInstance(organization1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** DISTRIBUTION **/
        if (obj.getDistribution() != null) {
            if(relationFromUpdate!=null && obj.getDistribution().contains(relationFromUpdate)){
                obj.getDistribution().remove(relationFromUpdate);
                obj.getDistribution().add(relationToUpdate);
            }
            for(LinkedEntity distribution : obj.getDistribution()){
                Distribution distribution1 = (Distribution) RelationChecker.checkRelation(obj, previousObj, null, distribution, overrideStatus, Distribution.class, false);
                if(distribution1!=null) {
                    DistributionDataproduct pi = new DistributionDataproduct();
                    pi.setDataproductInstance(edmobj);
                    pi.setDistributionInstance(distribution1);
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
                    DataproductSpatial pi = new DataproductSpatial();
                    pi.setDataproductInstance(edmobj);
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
                    DataproductTemporal pi = new DataproductTemporal();
                    pi.setDataproductInstance(edmobj);
                    pi.setTemporalInstance(temporal);
                    dbaccess.updateObject(pi);
                }
            }
        }

        if(obj.getReferencedBy()!=null){
            for(String accessurl : obj.getReferencedBy()) {
                createInnerElement(ElementType.REFERENCEDBY, accessurl, edmobj, overrideStatus);
            }
        }

        if(obj.getLandingPage()!=null){
            for(String accessurl : obj.getLandingPage()) {
                createInnerElement(ElementType.LANDINGPAGE, accessurl, edmobj, overrideStatus);
            }
        }

        if(obj.getVariableMeasured()!=null){
            for(String accessurl : obj.getVariableMeasured()) {
                createInnerElement(ElementType.VARIABLEMEASURED, accessurl, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Dataproduct edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        ElementAPI api = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);
        LinkedEntity le = api.create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        DataproductElement ce = new DataproductElement();
        ce.setDataproductInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }


    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(DataproductAttribution.class)){
            DataproductAttribution item = (DataproductAttribution) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductElement.class)){
            DataproductElement item = (DataproductElement) object;
            if(item.getDataproductInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductSource.class)){
            DataproductSource item = (DataproductSource) object;
            if(item.getDataproduct1Instance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
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

        for(Object object : getDbaccess().getAllFromDB(DistributionDataproduct.class)){
            DistributionDataproduct item = (DistributionDataproduct) object;
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
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }

        Dataproduct edmobj = elementList.get(0);

        org.epos.eposdatamodel.DataProduct o = new org.epos.eposdatamodel.DataProduct();

        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setAccrualPeriodicity(edmobj.getAccrualperiodicity());
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

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductAttribution.class)) {
            DataproductAttribution item = (DataproductAttribution) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
            LinkedEntity le = retrieveAPI(EntityNames.ATTRIBUTION.name()).retrieveLinkedEntity(item.getAttributionInstance().getInstanceId());
            o.addQualifiedAttribution(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductCategory.class)) {
            DataproductCategory item = (DataproductCategory) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                o.addCategory(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductContactpoint.class)) {
            DataproductContactpoint item = (DataproductContactpoint) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                o.addContactPoint(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductDescription.class)) {
            DataproductDescription item = (DataproductDescription) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                o.addDescription(item.getDescription());
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductTitle.class)) {
            DataproductTitle item = (DataproductTitle) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                o.addTitle(item.getTitle());
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductIdentifier.class)) {
            DataproductIdentifier item = (DataproductIdentifier) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId());
                o.addIdentifier(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(),DataproductSource.class)) {
            DataproductSource item = (DataproductSource) object;
            // if(item.getDataproduct1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
            LinkedEntity le = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId());
            o.addSource(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(),DataproductHaspart.class)) {
            DataproductHaspart item = (DataproductHaspart) object;
            // if(item.getDataproduct1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId());
                o.addHasPart(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(),DataproductIspartof.class)) {
            DataproductIspartof item = (DataproductIspartof) object;
            //if(item.getDataproduct1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId());
                o.addHasPart(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductProvenance.class)) {
            DataproductProvenance item = (DataproductProvenance) object;
            if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                o.addDescription(item.getProvenance());
            }
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductPublisher.class)) {
            DataproductPublisher item = (DataproductPublisher) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId());
                o.addPublisher(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DistributionDataproduct.class)) {
            DistributionDataproduct item = (DistributionDataproduct) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveLinkedEntity(item.getDistributionInstance().getInstanceId());
                o.addDistribution(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductSpatial.class)) {
            DataproductSpatial item = (DataproductSpatial) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                o.addSpatialExtentItem(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductTemporal.class)) {
            DataproductTemporal item = (DataproductTemporal) object;
            //if(item.getDataproductInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
                o.addTemporalExtent(le);
            //}
        }

        for (Object object : dbaccess.getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(),DataproductElement.class)) {
            DataproductElement item = (DataproductElement) object;
            //if(item.getDistributionInstance().getInstanceId().equals(edmobj.getInstanceId())) {
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.REFERENCEDBY.name())) o.addReferencedBy(el.getValue());
            if (el.getType().equals(ElementType.LANDINGPAGE.name())) o.addLandingPage(el.getValue());
            if (el.getType().equals(ElementType.VARIABLEMEASURED.name())) o.addVariableMeasured(el.getValue());
            //}
        }

        o = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.retrieveVersion(o);

        return o;
    }


    @Override
    public List<DataProduct> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Dataproduct.class));
    }
    @Override
    public List<DataProduct> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Dataproduct.class));
    }
    @Override
    public List<DataProduct> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Dataproduct.class, status));
    }

    private List<DataProduct> retrieveEntities(Function<Void, List<Dataproduct>> dbFetcher) {
        List<Dataproduct> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
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
