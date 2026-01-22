package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Attribution;
import model.Dataproduct;
import model.Distribution;
import model.Element;
import model.Identifier;
import model.Organization;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataProductAPI extends AbstractAPI<org.epos.eposdatamodel.DataProduct> {

    public DataProductAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(DataProduct obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Dataproduct> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            Dataproduct selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Dataproduct item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Dataproduct edmobj = new Dataproduct();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setKeywords(String.join("\\|", Optional.ofNullable(obj.getKeywords()).orElse("")));
        edmobj.setAccessright(obj.getAccessRight());
        edmobj.setAccrualperiodicity(obj.getAccrualPeriodicity());
        edmobj.setType(obj.getType());
        edmobj.setVersioninfo(obj.getVersionInfo());
        edmobj.setDocumentation(obj.getDocumentation());
        edmobj.setQualityassurance(obj.getQualityAssurance());

        if (obj.getCreated() != null) edmobj.setCreated(obj.getCreated());
        if (obj.getModified() != null) edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null) edmobj.setIssued(obj.getIssued());

        // CATEGORY & CONTACTPOINT (External Helpers)
        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus);

        /** TITLE **/
        if (obj.getTitle() != null) {
            RelationSyncUtil.syncSimpleOneToMany(
                    edmobj, edmobj.getInstanceId(), obj.getTitle(), model.DataproductTitle.class,
                    "dataproductInstance", "Title",
                    model.DataproductTitle::getTitle, model.DataproductTitle::setTitle, model.DataproductTitle::setDataproductInstance
            );
        }

        /** DESCRIPTION **/
        if (obj.getDescription() != null) {
            RelationSyncUtil.syncSimpleOneToMany(
                    edmobj, edmobj.getInstanceId(), obj.getDescription(), model.DataproductDescription.class,
                    "dataproductInstance", "Description",
                    model.DataproductDescription::getDescription, model.DataproductDescription::setDescription, model.DataproductDescription::setDataproductInstance
            );
        }

        /** PROVENANCE **/
        if (obj.getProvenance() != null) {
            RelationSyncUtil.syncSimpleOneToMany(
                    edmobj, edmobj.getInstanceId(), obj.getProvenance(), model.DataproductProvenance.class,
                    "dataproductInstance", "Provenance",
                    model.DataproductProvenance::getProvenance, model.DataproductProvenance::setProvenance, model.DataproductProvenance::setDataproductInstance
            );
        }

        /** PUBLISHER (DataproductPublisher) **/
        if (obj.getPublisher() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getPublisher(), relationFromUpdate, relationToUpdate,
                    DataproductPublisher.class, Organization.class,
                    "dataproductInstance",
                    DataproductPublisher::getOrganizationInstance,
                    DataproductPublisher::setDataproductInstance,
                    DataproductPublisher::setOrganizationInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** DISTRIBUTION (DistributionDataproduct) **/
        if (obj.getDistribution() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getDistribution(), relationFromUpdate, relationToUpdate,
                    DistributionDataproduct.class, Distribution.class,
                    "dataproductInstance",
                    DistributionDataproduct::getDistributionInstance,
                    DistributionDataproduct::setDataproductInstance,
                    DistributionDataproduct::setDistributionInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** QUALIFIED ATTRIBUTION (DataproductAttribution) **/
        if (obj.getQualifiedAttribution() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getQualifiedAttribution(), relationFromUpdate, relationToUpdate,
                    DataproductAttribution.class, Attribution.class,
                    "dataproductInstance",
                    DataproductAttribution::getAttributionInstance,
                    DataproductAttribution::setDataproductInstance,
                    DataproductAttribution::setAttributionInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** SOURCE (DataproductSource) **/
        if (obj.getSource() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSource(), relationFromUpdate, relationToUpdate,
                    DataproductSource.class, Dataproduct.class,
                    "dataproduct1Instance",
                    DataproductSource::getDataproduct2Instance,
                    DataproductSource::setDataproduct1Instance,
                    DataproductSource::setDataproduct2Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** HAS PART (DataproductHaspart) **/
        if (obj.getHasPart() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getHasPart(), relationFromUpdate, relationToUpdate,
                    DataproductHaspart.class, Dataproduct.class,
                    "dataproduct1Instance",
                    DataproductHaspart::getDataproduct2Instance,
                    DataproductHaspart::setDataproduct1Instance,
                    DataproductHaspart::setDataproduct2Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** IS PART OF (DataproductIspartof) **/
        if (obj.getIsPartOf() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIsPartOf(), relationFromUpdate, relationToUpdate,
                    DataproductIspartof.class, Dataproduct.class,
                    "dataproduct2Instance", // Parent (Is Part Of)
                    DataproductIspartof::getDataproduct1Instance,
                    DataproductIspartof::setDataproduct2Instance,
                    DataproductIspartof::setDataproduct1Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** SPATIAL (DataproductSpatial) **/
        if (obj.getSpatialExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                    DataproductSpatial.class, Spatial.class,
                    "dataproductInstance",
                    DataproductSpatial::getSpatialInstance,
                    DataproductSpatial::setDataproductInstance,
                    DataproductSpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** TEMPORAL (DataproductTemporal) **/
        if (obj.getTemporalExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                    DataproductTemporal.class, Temporal.class,
                    "dataproductInstance",
                    DataproductTemporal::getTemporalInstance,
                    DataproductTemporal::setDataproductInstance,
                    DataproductTemporal::setTemporalInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** IDENTIFIER (DataproductIdentifier) **/
        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    DataproductIdentifier.class, Identifier.class,
                    "dataproductInstance",
                    DataproductIdentifier::getIdentifierInstance,
                    DataproductIdentifier::setDataproductInstance,
                    DataproductIdentifier::setIdentifierInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        if (obj.getReferencedBy() != null) {
            for (String val : obj.getReferencedBy()) createInnerElement(ElementType.REFERENCEDBY, val, edmobj, overrideStatus);
        }
        if (obj.getLandingPage() != null) {
            for (String val : obj.getLandingPage()) createInnerElement(ElementType.LANDINGPAGE, val, edmobj, overrideStatus);
        }
        if (obj.getVariableMeasured() != null) {
            for (String val : obj.getVariableMeasured()) createInnerElement(ElementType.VARIABLEMEASURED, val, edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Dataproduct edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                DataproductElement relation = (DataproductElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null && existingElement.getType().equals(elementType.name()) && existingElement.getValue().equals(value)) {
                    return;
                }
            }
        }
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        if (edmobj.getVersion().getEditorId() != null)
            element.setEditorId(edmobj.getVersion().getEditorId());
        if (edmobj.getVersion().getProvenance() != null)
            element.setFileProvenance(edmobj.getVersion().getProvenance());
        if (edmobj.getVersion().getChangeComment() != null)
            element.setChangeComment(edmobj.getVersion().getChangeComment());
        if (edmobj.getVersion().getChangeTimestamp() != null)
            element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            DataproductElement ce = new DataproductElement();
            ce.setDataproductInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("dataproductInstance", instanceId, DataproductAttribution.class);
        deleteRelations("dataproductInstance", instanceId, DataproductElement.class);
        deleteRelations("dataproduct1Instance", instanceId, DataproductSource.class); // Source (as 1)
        deleteRelations("dataproductInstance", instanceId, DataproductContactpoint.class);
        deleteRelations("dataproductInstance", instanceId, DataproductCategory.class);
        deleteRelations("dataproduct1Instance", instanceId, DataproductIspartof.class); // IsPartOf (as 1)
        deleteRelations("dataproductInstance", instanceId, DataproductIdentifier.class);
        deleteRelations("dataproductInstance", instanceId, DataproductTitle.class);
        deleteRelations("dataproductInstance", instanceId, DataproductTemporal.class);
        deleteRelations("dataproductInstance", instanceId, DataproductSpatial.class);
        deleteRelations("dataproduct", instanceId, DataproductRelation.class);
        deleteRelations("dataproductInstance", instanceId, DataproductPublisher.class);
        deleteRelations("dataproductInstance", instanceId, DataproductProvenance.class);
        deleteRelations("dataproduct1Instance", instanceId, DataproductHaspart.class); // HasPart (as 1)
        deleteRelations("dataproductInstance", instanceId, DataproductDescription.class);
        deleteRelations("dataproductInstance", instanceId, DistributionDataproduct.class);

        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        for (Dataproduct object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.DataProduct retrieve(String instanceId) {
        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Dataproduct edmobj = elementList.get(0);
        org.epos.eposdatamodel.DataProduct o = new org.epos.eposdatamodel.DataProduct();

        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setAccrualPeriodicity(edmobj.getAccrualperiodicity());
        o.setCreated(edmobj.getCreated());
        o.setIssued(edmobj.getIssued());
        o.setModified(edmobj.getModified());
        o.setType(edmobj.getType());
        o.setVersionInfo(edmobj.getVersioninfo());
        o.setDocumentation(edmobj.getDocumentation());
        o.setQualityAssurance(edmobj.getQualityassurance());
        o.setAccessRight(edmobj.getAccessright());

        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isBlank())
            for (String item : edmobj.getKeywords().split("\\|"))
                o.addKeywords(item);

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductAttribution.class)) {
            DataproductAttribution item = (DataproductAttribution) object;
            o.addQualifiedAttribution(retrieveAPI(EntityNames.ATTRIBUTION.name()).retrieveLinkedEntity(item.getAttributionInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductCategory.class)) {
            DataproductCategory item = (DataproductCategory) object;
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductContactpoint.class)) {
            DataproductContactpoint item = (DataproductContactpoint) object;
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductDescription.class)) {
            DataproductDescription item = (DataproductDescription) object;
            o.addDescription(item.getDescription());
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductTitle.class)) {
            DataproductTitle item = (DataproductTitle) object;
            o.addTitle(item.getTitle());
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductIdentifier.class)) {
            DataproductIdentifier item = (DataproductIdentifier) object;
            o.addIdentifier(retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(item.getIdentifierInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(), DataproductSource.class)) {
            DataproductSource item = (DataproductSource) object;
            o.addSource(retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(), DataproductHaspart.class)) {
            DataproductHaspart item = (DataproductHaspart) object;
            o.addHasPart(retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproduct1Instance", edmobj.getInstanceId(), DataproductIspartof.class)) {
            DataproductIspartof item = (DataproductIspartof) object;
            o.addHasPart(retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductProvenance.class)) {
            DataproductProvenance item = (DataproductProvenance) object;
            o.addDescription(item.getProvenance()); // Check mapping: PROVENANCE field added to description?
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductPublisher.class)) {
            DataproductPublisher item = (DataproductPublisher) object;
            o.addPublisher(retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(item.getOrganizationInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DistributionDataproduct.class)) {
            DistributionDataproduct item = (DistributionDataproduct) object;
            o.addDistribution(retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveLinkedEntity(item.getDistributionInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductSpatial.class)) {
            DataproductSpatial item = (DataproductSpatial) object;
            o.addSpatialExtentItem(retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductTemporal.class)) {
            DataproductTemporal item = (DataproductTemporal) object;
            o.addTemporalExtent(retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductElement.class)) {
            DataproductElement item = (DataproductElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.REFERENCEDBY.name())) o.addReferencedBy(el.getValue());
            if (el.getType().equals(ElementType.LANDINGPAGE.name())) o.addLandingPage(el.getValue());
            if (el.getType().equals(ElementType.VARIABLEMEASURED.name())) o.addVariableMeasured(el.getValue());
        }

        o = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.DataProduct retrieveByUID(String uid) {
        List<Dataproduct> returnList = getDbaccess().getOneFromDBByUID(uid, Dataproduct.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<DataProduct> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Dataproduct.class));
    }
    @Override
    public List<DataProduct> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Dataproduct.class));
    }
    @Override
    public List<DataProduct> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Dataproduct.class, status));
    }
    private List<DataProduct> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Dataproduct.class);
        if (elementList != null && !elementList.isEmpty()) {
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