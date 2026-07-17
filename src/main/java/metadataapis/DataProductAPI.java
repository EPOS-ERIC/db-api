package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Attribution;
import model.Category;
import model.Dataproduct;
import model.Distribution;
import model.Element;
import model.Identifier;
import model.Organization;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DataProductAPI extends AbstractAPI<org.epos.eposdatamodel.DataProduct> {

    private static final Logger LOG = Logger.getLogger(DataProductAPI.class.getName());

    public DataProductAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(DataProduct obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Capture if fields were explicitly set BEFORE any processing
        boolean titleExplicitlySet = isFieldExplicitlySet(obj, "title");
        boolean descriptionExplicitlySet = isFieldExplicitlySet(obj, "description");
        boolean provenanceExplicitlySet = isFieldExplicitlySet(obj, "provenance");
        boolean publisherExplicitlySet = isFieldExplicitlySet(obj, "publisher");
        boolean distributionExplicitlySet = isFieldExplicitlySet(obj, "distribution");
        boolean qualifiedAttributionExplicitlySet = isFieldExplicitlySet(obj, "qualifiedAttribution");
        boolean identifierExplicitlySet = isFieldExplicitlySet(obj, "identifier");
        boolean spatialExtentExplicitlySet = isFieldExplicitlySet(obj, "spatialExtent");
        boolean temporalExtentExplicitlySet = isFieldExplicitlySet(obj, "temporalExtent");
        boolean sourceExplicitlySet = isFieldExplicitlySet(obj, "source");
        boolean hasPartExplicitlySet = isFieldExplicitlySet(obj, "hasPart");
        boolean isPartOfExplicitlySet = isFieldExplicitlySet(obj, "isPartOf");
        boolean categoryExplicitlySet = isFieldExplicitlySet(obj, "category");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<Dataproduct> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            String requestedInstanceId = obj.getInstanceId();
            String requestedEditorId = obj.getEditorId();
            Dataproduct selectedEntity = returnList.stream()
                    .filter(item -> targetStatus != StatusType.DRAFT
                            && requestedInstanceId != null
                            && requestedInstanceId.equals(item.getInstanceId()))
                    .findFirst()
                    .orElseGet(() -> VersioningStatusAPI.selectVersion(
                            returnList, requestedEditorId, targetStatus, Dataproduct::getVersion));
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());

            if (previousObj == null) {
                previousObj = retrieve(selectedEntity.getInstanceId());
            }
        }

        String previousVersionInstanceId = obj.getInstanceChangedId();
        obj = (org.epos.eposdatamodel.DataProduct) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        boolean preserveExistingDistribution = false;
        if (isUpdate && obj.getStatus() == StatusType.DRAFT && previousObj instanceof DataProduct previousDataProduct) {
            preserveDraftDistributionLinks(obj, previousDataProduct);
            preserveExistingDistribution = obj.getDistribution() == null;
        }

        String newInstanceId = obj.getInstanceId();

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

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        if (obj.getCreated() != null) edmobj.setCreated(obj.getCreated());
        if (obj.getModified() != null) edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null) edmobj.setIssued(obj.getIssued());

        // CATEGORY
        RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getCategory(), relationFromUpdate, relationToUpdate,
                DataproductCategory.class, Category.class, "dataproductInstance", DataproductCategory::getCategoryInstance,
                DataproductCategory::setDataproductInstance, DataproductCategory::setCategoryInstance, obj, previousObj, overrideStatus, true);

        // CONTACTPOINT
        RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getContactPoint(), relationFromUpdate, relationToUpdate,
                DataproductContactpoint.class, Contactpoint.class, "dataproductInstance", DataproductContactpoint::getContactpointInstance,
                DataproductContactpoint::setDataproductInstance, DataproductContactpoint::setContactpointInstance, obj, previousObj, overrideStatus, true);

        /** TITLE **/
        RelationSyncUtil.syncSimpleOneToManyWithVersionFallback(
                edmobj, edmobj.getInstanceId(), obj.getTitle(), model.DataproductTitle.class,
                "dataproductInstance", "Title",
                model.DataproductTitle::getTitle, model.DataproductTitle::setTitle, model.DataproductTitle::setDataproductInstance,
                obj, oldInstanceId
        );

        /** DESCRIPTION **/
        RelationSyncUtil.syncSimpleOneToManyWithVersionFallback(
                edmobj, edmobj.getInstanceId(), obj.getDescription(), model.DataproductDescription.class,
                "dataproductInstance", "Description",
                model.DataproductDescription::getDescription, model.DataproductDescription::setDescription, model.DataproductDescription::setDataproductInstance,
                obj, oldInstanceId
        );

        /** PROVENANCE **/
        RelationSyncUtil.syncSimpleOneToManyWithVersionFallback(
                edmobj, edmobj.getInstanceId(), obj.getProvenance(), model.DataproductProvenance.class,
                "dataproductInstance", "Provenance",
                model.DataproductProvenance::getProvenance, model.DataproductProvenance::setProvenance, model.DataproductProvenance::setDataproductInstance,
                obj, oldInstanceId
        );

        /** PUBLISHER (DataproductPublisher) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getPublisher(), relationFromUpdate, relationToUpdate,
                DataproductPublisher.class, Organization.class,
                "dataproductInstance",
                DataproductPublisher::getOrganizationInstance,
                DataproductPublisher::setDataproductInstance,
                DataproductPublisher::setOrganizationInstance,
                obj, previousObj, overrideStatus, true
        );

        /** DISTRIBUTION (DistributionDataproduct) **/
        List<LinkedEntity> distributionLinks = obj.getDistribution();
        if (distributionLinks == null && previousObj instanceof DataProduct previousDataProduct) {
            distributionLinks = previousDataProduct.getDistribution();
        }
        if (!preserveExistingDistribution) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), distributionLinks, relationFromUpdate, relationToUpdate,
                    DistributionDataproduct.class, Distribution.class,
                    "dataproductInstance",
                    DistributionDataproduct::getDistributionInstance,
                    DistributionDataproduct::setDataproductInstance,
                    DistributionDataproduct::setDistributionInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** QUALIFIED ATTRIBUTION (DataproductAttribution) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getQualifiedAttribution(), relationFromUpdate, relationToUpdate,
                DataproductAttribution.class, Attribution.class,
                "dataproductInstance",
                DataproductAttribution::getAttributionInstance,
                DataproductAttribution::setDataproductInstance,
                DataproductAttribution::setAttributionInstance,
                obj, previousObj, overrideStatus, true
        );

        /** IDENTIFIER (DataproductIdentifier) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                DataproductIdentifier.class, Identifier.class,
                "dataproductInstance",
                DataproductIdentifier::getIdentifierInstance,
                DataproductIdentifier::setDataproductInstance,
                DataproductIdentifier::setIdentifierInstance,
                obj, previousObj, overrideStatus, false
        );

        /** SPATIAL (DataproductSpatial) **/
        List<LinkedEntity> spatialLinks = obj.getSpatialExtent();
        if (isUpdate && !spatialExtentExplicitlySet) {
            spatialLinks = Collections.emptyList();
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), spatialLinks, relationFromUpdate, relationToUpdate,
                DataproductSpatial.class, Spatial.class,
                "dataproductInstance",
                DataproductSpatial::getSpatialInstance,
                DataproductSpatial::setDataproductInstance,
                DataproductSpatial::setSpatialInstance,
                obj, previousObj, overrideStatus, false
        );

        /** TEMPORAL (DataproductTemporal) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                DataproductTemporal.class, Temporal.class,
                "dataproductInstance",
                DataproductTemporal::getTemporalInstance,
                DataproductTemporal::setDataproductInstance,
                DataproductTemporal::setTemporalInstance,
                obj, previousObj, overrideStatus, false
        );

        /** SOURCE (DataproductSource) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getSource(), relationFromUpdate, relationToUpdate,
                DataproductSource.class, Dataproduct.class,
                "dataproduct1Instance",
                DataproductSource::getDataproduct2Instance,
                DataproductSource::setDataproduct1Instance,
                DataproductSource::setDataproduct2Instance,
                obj, previousObj, overrideStatus, false
        );

        /** HAS PART (DataproductHaspart) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getHasPart(), relationFromUpdate, relationToUpdate,
                DataproductHaspart.class, Dataproduct.class,
                "dataproduct1Instance",
                DataproductHaspart::getDataproduct2Instance,
                DataproductHaspart::setDataproduct1Instance,
                DataproductHaspart::setDataproduct2Instance,
                obj, previousObj, overrideStatus, false
        );

        /** IS PART OF (DataproductIspartof) **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getIsPartOf(), relationFromUpdate, relationToUpdate,
                DataproductIspartof.class, Dataproduct.class,
                "dataproduct1Instance",
                DataproductIspartof::getDataproduct2Instance,
                DataproductIspartof::setDataproduct1Instance,
                DataproductIspartof::setDataproduct2Instance,
                obj, previousObj, overrideStatus, false
        );

        /** ELEMENT-BASED RELATIONS (landingPage, referencedBy, variableMeasured) **/
        handleElementRelations(obj, edmobj, overrideStatus, isNewVersion, oldInstanceId);

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.DATAPRODUCT.name(), edmobj);

        if (StatusType.PUBLISHED.equals(obj.getStatus())
                && previousVersionInstanceId != null
                && !previousVersionInstanceId.equals(obj.getInstanceId())) {
            org.epos.eposdatamodel.DataProduct previousVersion = retrieve(previousVersionInstanceId);
            if (previousVersion != null && previousVersion.getDistribution() != null) {
                for (LinkedEntity distributionLink : previousVersion.getDistribution()) {
                    RelationSyncUtil.archiveVersionByInstanceId(distributionLink.getInstanceId());
                }
            }
        }

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    private void preserveDraftDistributionLinks(DataProduct current, DataProduct previous) {
        if (current.getDistribution() == null || previous.getDistribution() == null) {
            return;
        }

        Map<String, LinkedEntity> previousByUid = new HashMap<>();
        for (LinkedEntity link : previous.getDistribution()) {
            if (link != null && link.getUid() != null) {
                previousByUid.put(link.getUid(), link);
            }
        }

        List<LinkedEntity> normalized = new ArrayList<>(current.getDistribution().size());
        boolean unchangedRelation = true;
        for (LinkedEntity link : current.getDistribution()) {
            if (link != null && link.getUid() != null) {
                LinkedEntity previousLink = previousByUid.get(link.getUid());
                if (previousLink != null && !Objects.equals(link.getInstanceId(), previousLink.getInstanceId())) {
                    normalized.add(previousLink);
                    continue;
                }
                if (previousLink == null) {
                    unchangedRelation = false;
                }
            } else {
                unchangedRelation = false;
            }
            normalized.add(link);
        }
        // A stale published link represents the already persisted logical
        // relation. Null tells the synchronizer to leave that join untouched.
        current.setDistribution(unchangedRelation ? null : normalized);
    }

    private void handleElementRelations(DataProduct obj, Dataproduct edmobj, StatusType overrideStatus, boolean isNewVersion, String oldInstanceId) {
        Set<ElementValue> existingElements = getExistingElementValues(edmobj.getInstanceId());

        // LANDING PAGE
        if (obj.getLandingPage() != null && !obj.getLandingPage().isEmpty()) {
            createInnerElements(ElementType.LANDINGPAGE, obj.getLandingPage(), edmobj, overrideStatus, existingElements);
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.LANDINGPAGE, overrideStatus, existingElements);
        }

        // REFERENCED BY
        if (obj.getReferencedBy() != null && !obj.getReferencedBy().isEmpty()) {
            createInnerElements(ElementType.REFERENCEDBY, obj.getReferencedBy(), edmobj, overrideStatus, existingElements);
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.REFERENCEDBY, overrideStatus, existingElements);
        }

        // VARIABLE MEASURED
        if (obj.getVariableMeasured() != null && !obj.getVariableMeasured().isEmpty()) {
            createInnerElements(ElementType.VARIABLEMEASURED, obj.getVariableMeasured(), edmobj, overrideStatus, existingElements);
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.VARIABLEMEASURED, overrideStatus, existingElements);
        }
    }

    private Set<ElementValue> getExistingElementValues(String dataproductInstanceId) {
        List<DataproductElement> relations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("dataproductInstance", dataproductInstanceId, DataproductElement.class);
        Set<ElementValue> values = new HashSet<>();
        if (relations != null) {
            for (DataproductElement relation : relations) {
                Element element = relation.getElementInstance();
                if (element != null && element.getType() != null) {
                    values.add(new ElementValue(element.getType(), element.getValue()));
                }
            }
        }
        return values;
    }

    private void deleteExistingElements(String dataproductInstanceId) {
        List<DataproductElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("dataproductInstance", dataproductInstanceId, DataproductElement.class);

        if (existingRelations != null) {
            for (DataproductElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                // Also delete the Element entity
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Dataproduct newEdmobj, ElementType elementType,
                                                 StatusType overrideStatus, Set<ElementValue> existingElements) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("dataproductInstance", oldInstanceId, DataproductElement.class);

        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            DataproductElement oldRelation = (DataproductElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus, existingElements);
            }
        }
    }

    // Dead code removed: copyDataproductCategoryRelations and copyDataproductContactPointRelations
    // These were replaced by RelationSyncUtil.syncComplexRelation()

    private void createInnerElements(ElementType elementType, Collection<String> values, Dataproduct edmobj,
                                     StatusType overrideStatus, Set<ElementValue> existingElements) {
        for (String value : values) {
            createInnerElement(elementType, value, edmobj, overrideStatus, existingElements);
        }
    }

    private void createInnerElement(ElementType elementType, String value, Dataproduct edmobj, StatusType overrideStatus,
                                    Set<ElementValue> existingElements) {
        ElementValue elementValue = new ElementValue(elementType.name(), value);
        if (!existingElements.add(elementValue)) {
            return;
        }

        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        Versioningstatus version = edmobj.getVersion();
        if (version != null) {
            if (version.getEditorId() != null) element.setEditorId(version.getEditorId());
            if (version.getProvenance() != null) element.setFileProvenance(version.getProvenance());
            if (version.getChangeComment() != null) element.setChangeComment(version.getChangeComment());
            if (version.getChangeTimestamp() != null) element.setChangeTimestamp(version.getChangeTimestamp().toLocalDateTime());
        }

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            DataproductElement de = new DataproductElement();
            de.setDataproductInstance(edmobj);
            de.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(de);
        } else {
            existingElements.remove(elementValue);
        }
    }

    private record ElementValue(String type, String value) {
    }

    

    

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Dataproduct.class, List.of(
                new EposDataModelDAO.RelationField(DataproductTitle.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductDescription.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductProvenance.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductElement.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductIdentifier.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductSpatial.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductTemporal.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductCategory.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductContactpoint.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductPublisher.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductAttribution.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DistributionDataproduct.class, "dataproductInstance"),
                new EposDataModelDAO.RelationField(DataproductSource.class, "dataproduct1Instance"),
                new EposDataModelDAO.RelationField(DataproductHaspart.class, "dataproduct1Instance"),
                new EposDataModelDAO.RelationField(DataproductIspartof.class, "dataproduct1Instance"),
                new EposDataModelDAO.RelationField(DataproductSource.class, "dataproduct2Instance"),
                new EposDataModelDAO.RelationField(DataproductHaspart.class, "dataproduct2Instance"),
                new EposDataModelDAO.RelationField(DataproductIspartof.class, "dataproduct2Instance")));
    }

    @Override
    public org.epos.eposdatamodel.DataProduct retrieve(String instanceId) {
        List<Dataproduct> elementList = getDbaccess().getOneFromDBByInstanceIdNoCache(instanceId, Dataproduct.class);
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
            o.addIsPartOf(retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproduct2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("dataproductInstance", edmobj.getInstanceId(), DataproductProvenance.class)) {
            DataproductProvenance item = (DataproductProvenance) object;
            o.addProvenance(item.getProvenance());
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
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Use optimized bulk retrieval for better performance
        return retrieveBulkInternal(instanceIds);
    }

    /**
     * Optimized bulk retrieval - fetches all DataProducts and their relations in minimal queries.
     * This dramatically reduces N+1 query problems.
     * 
     * <p><strong>Performance:</strong> For 400 DataProducts, reduces queries from ~12,000 to ~40.</p>
     */
    private List<DataProduct> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        
        // Step 1: Batch fetch all main Dataproduct entities
        Map<String, Dataproduct> dataproducts = getDbaccess().batchFetchByInstanceIds(instanceIds, Dataproduct.class);
        
        if (dataproducts.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get actual found IDs (some might not exist)
        List<String> foundIds = new ArrayList<>(dataproducts.keySet());
        
        // Step 2: Batch fetch ALL join tables for ALL dataproducts at once
        Map<String, List<DataproductAttribution>> attributions = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductAttribution.class);
        Map<String, List<DataproductCategory>> categories = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductCategory.class);
        Map<String, List<DataproductContactpoint>> contactPoints = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductContactpoint.class);
        Map<String, List<DataproductDescription>> descriptions = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductDescription.class);
        Map<String, List<DataproductTitle>> titles = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductTitle.class);
        Map<String, List<DataproductIdentifier>> identifiers = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductIdentifier.class);
        Map<String, List<DataproductSource>> sources = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproduct1Instance", foundIds, DataproductSource.class);
        Map<String, List<DataproductHaspart>> hasParts = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproduct1Instance", foundIds, DataproductHaspart.class);
        Map<String, List<DataproductIspartof>> isPartOfs = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproduct1Instance", foundIds, DataproductIspartof.class);
        Map<String, List<DataproductProvenance>> provenances = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductProvenance.class);
        Map<String, List<DataproductPublisher>> publishers = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductPublisher.class);
        Map<String, List<DistributionDataproduct>> distributions = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DistributionDataproduct.class);
        Map<String, List<DataproductSpatial>> spatials = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductSpatial.class);
        Map<String, List<DataproductTemporal>> temporals = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductTemporal.class);
        Map<String, List<DataproductElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("dataproductInstance", foundIds, DataproductElement.class);
        
        // Step 3: Collect all target entity IDs for batch fetching
        Set<String> allAttributionIds = new HashSet<>();
        Set<String> allCategoryIds = new HashSet<>();
        Set<String> allContactPointIds = new HashSet<>();
        Set<String> allIdentifierIds = new HashSet<>();
        Set<String> allDataProductIds = new HashSet<>(); // For source, hasPart, isPartOf
        Set<String> allOrganizationIds = new HashSet<>();
        Set<String> allDistributionIds = new HashSet<>();
        Set<String> allSpatialIds = new HashSet<>();
        Set<String> allTemporalIds = new HashSet<>();
        
        attributions.values().forEach(list -> list.forEach(r -> {
            if (r.getAttributionInstance() != null) allAttributionIds.add(r.getAttributionInstance().getInstanceId());
        }));
        categories.values().forEach(list -> list.forEach(r -> {
            if (r.getCategoryInstance() != null) allCategoryIds.add(r.getCategoryInstance().getInstanceId());
        }));
        contactPoints.values().forEach(list -> list.forEach(r -> {
            if (r.getContactpointInstance() != null) allContactPointIds.add(r.getContactpointInstance().getInstanceId());
        }));
        identifiers.values().forEach(list -> list.forEach(r -> {
            if (r.getIdentifierInstance() != null) allIdentifierIds.add(r.getIdentifierInstance().getInstanceId());
        }));
        sources.values().forEach(list -> list.forEach(r -> {
            if (r.getDataproduct2Instance() != null) allDataProductIds.add(r.getDataproduct2Instance().getInstanceId());
        }));
        hasParts.values().forEach(list -> list.forEach(r -> {
            if (r.getDataproduct2Instance() != null) allDataProductIds.add(r.getDataproduct2Instance().getInstanceId());
        }));
        isPartOfs.values().forEach(list -> list.forEach(r -> {
            if (r.getDataproduct2Instance() != null) allDataProductIds.add(r.getDataproduct2Instance().getInstanceId());
        }));
        publishers.values().forEach(list -> list.forEach(r -> {
            if (r.getOrganizationInstance() != null) allOrganizationIds.add(r.getOrganizationInstance().getInstanceId());
        }));
        distributions.values().forEach(list -> list.forEach(r -> {
            if (r.getDistributionInstance() != null) allDistributionIds.add(r.getDistributionInstance().getInstanceId());
        }));
        spatials.values().forEach(list -> list.forEach(r -> {
            if (r.getSpatialInstance() != null) allSpatialIds.add(r.getSpatialInstance().getInstanceId());
        }));
        temporals.values().forEach(list -> list.forEach(r -> {
            if (r.getTemporalInstance() != null) allTemporalIds.add(r.getTemporalInstance().getInstanceId());
        }));
        
        // Step 4: Batch fetch all target entities
        Map<String, Attribution> attributionMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allAttributionIds), Attribution.class);
        Map<String, Category> categoryMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allCategoryIds), Category.class);
        Map<String, Contactpoint> contactPointMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allContactPointIds), Contactpoint.class);
        Map<String, Identifier> identifierMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allIdentifierIds), Identifier.class);
        Map<String, Dataproduct> relatedDpMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allDataProductIds), Dataproduct.class);
        Map<String, Organization> organizationMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allOrganizationIds), Organization.class);
        Map<String, Distribution> distributionMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allDistributionIds), Distribution.class);
        Map<String, Spatial> spatialMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allSpatialIds), Spatial.class);
        Map<String, Temporal> temporalMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allTemporalIds), Temporal.class);
        
        // Step 5: Batch fetch versioning status for all entities
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Step 5b: Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = dataproducts.values().stream()
                .map(Dataproduct::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        // Step 6: Assemble DataProduct DTOs using pre-fetched data (no additional queries!)
        List<DataProduct> results = foundIds.parallelStream()
                .map(id -> assembleDataProduct(id, dataproducts, 
                        attributions, categories, contactPoints, descriptions, titles,
                        identifiers, sources, hasParts, isPartOfs, provenances, publishers,
                        distributions, spatials, temporals, elements,
                        attributionMap, categoryMap, contactPointMap, identifierMap,
                        relatedDpMap, organizationMap, distributionMap, spatialMap, temporalMap,
                        versioningMap, groupsMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        long elapsed = System.currentTimeMillis() - startTime;
        LOG.log(Level.FINE, "Bulk retrieved {0} DataProducts in {1}ms", new Object[]{results.size(), elapsed});
        
        return results;
    }

    /**
     * Assembles a DataProduct DTO from pre-fetched data without any database queries.
     */
    private DataProduct assembleDataProduct(
            String instanceId,
            Map<String, Dataproduct> dataproducts,
            Map<String, List<DataproductAttribution>> attributions,
            Map<String, List<DataproductCategory>> categories,
            Map<String, List<DataproductContactpoint>> contactPoints,
            Map<String, List<DataproductDescription>> descriptions,
            Map<String, List<DataproductTitle>> titles,
            Map<String, List<DataproductIdentifier>> identifiers,
            Map<String, List<DataproductSource>> sources,
            Map<String, List<DataproductHaspart>> hasParts,
            Map<String, List<DataproductIspartof>> isPartOfs,
            Map<String, List<DataproductProvenance>> provenances,
            Map<String, List<DataproductPublisher>> publishers,
            Map<String, List<DistributionDataproduct>> distributions,
            Map<String, List<DataproductSpatial>> spatials,
            Map<String, List<DataproductTemporal>> temporals,
            Map<String, List<DataproductElement>> elements,
            Map<String, Attribution> attributionMap,
            Map<String, Category> categoryMap,
            Map<String, Contactpoint> contactPointMap,
            Map<String, Identifier> identifierMap,
            Map<String, Dataproduct> relatedDpMap,
            Map<String, Organization> organizationMap,
            Map<String, Distribution> distributionMap,
            Map<String, Spatial> spatialMap,
            Map<String, Temporal> temporalMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        Dataproduct edmobj = dataproducts.get(instanceId);
        if (edmobj == null) return null;
        
        DataProduct o = new DataProduct();
        
        // Set simple fields
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setAccrualPeriodicity(edmobj.getAccrualperiodicity());
        o.setCreated(edmobj.getCreated());
        o.setIssued(edmobj.getIssued());
        o.setModified(edmobj.getModified());
        o.setVersionInfo(edmobj.getVersioninfo());
        o.setDocumentation(edmobj.getDocumentation());
        o.setQualityAssurance(edmobj.getQualityassurance());
        o.setAccessRight(edmobj.getAccessright());
        
        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isBlank()) {
            for (String item : edmobj.getKeywords().split("\\|")) {
                o.addKeywords(item);
            }
        }
        
        // Set relations from pre-fetched data
        for (DataproductAttribution rel : attributions.getOrDefault(instanceId, Collections.emptyList())) {
            Attribution target = attributionMap.get(rel.getAttributionInstance().getInstanceId());
            if (target != null) {
                o.addQualifiedAttribution(createLinkedEntity(target, EntityNames.ATTRIBUTION.name()));
            }
        }
        
        for (DataproductCategory rel : categories.getOrDefault(instanceId, Collections.emptyList())) {
            Category target = categoryMap.get(rel.getCategoryInstance().getInstanceId());
            if (target != null) {
                o.addCategory(createLinkedEntity(target, EntityNames.CATEGORY.name()));
            }
        }
        
        for (DataproductContactpoint rel : contactPoints.getOrDefault(instanceId, Collections.emptyList())) {
            Contactpoint target = contactPointMap.get(rel.getContactpointInstance().getInstanceId());
            if (target != null) {
                o.addContactPoint(createLinkedEntity(target, EntityNames.CONTACTPOINT.name()));
            }
        }
        
        for (DataproductDescription rel : descriptions.getOrDefault(instanceId, Collections.emptyList())) {
            o.addDescription(rel.getDescription());
        }
        
        for (DataproductTitle rel : titles.getOrDefault(instanceId, Collections.emptyList())) {
            o.addTitle(rel.getTitle());
        }
        
        for (DataproductIdentifier rel : identifiers.getOrDefault(instanceId, Collections.emptyList())) {
            Identifier target = identifierMap.get(rel.getIdentifierInstance().getInstanceId());
            if (target != null) {
                o.addIdentifier(createLinkedEntity(target, EntityNames.IDENTIFIER.name()));
            }
        }
        
        for (DataproductSource rel : sources.getOrDefault(instanceId, Collections.emptyList())) {
            Dataproduct target = relatedDpMap.get(rel.getDataproduct2Instance().getInstanceId());
            if (target != null) {
                o.addSource(createLinkedEntity(target, EntityNames.DATAPRODUCT.name()));
            }
        }
        
        for (DataproductHaspart rel : hasParts.getOrDefault(instanceId, Collections.emptyList())) {
            Dataproduct target = relatedDpMap.get(rel.getDataproduct2Instance().getInstanceId());
            if (target != null) {
                o.addHasPart(createLinkedEntity(target, EntityNames.DATAPRODUCT.name()));
            }
        }
        
        for (DataproductIspartof rel : isPartOfs.getOrDefault(instanceId, Collections.emptyList())) {
            Dataproduct target = relatedDpMap.get(rel.getDataproduct2Instance().getInstanceId());
            if (target != null) {
                o.addIsPartOf(createLinkedEntity(target, EntityNames.DATAPRODUCT.name()));
            }
        }
        
        for (DataproductProvenance rel : provenances.getOrDefault(instanceId, Collections.emptyList())) {
            o.addProvenance(rel.getProvenance());
        }
        
        for (DataproductPublisher rel : publishers.getOrDefault(instanceId, Collections.emptyList())) {
            Organization target = organizationMap.get(rel.getOrganizationInstance().getInstanceId());
            if (target != null) {
                o.addPublisher(createLinkedEntity(target, EntityNames.ORGANIZATION.name()));
            }
        }
        
        Set<String> distributionIds = new HashSet<>();
        for (DistributionDataproduct rel : distributions.getOrDefault(instanceId, Collections.emptyList())) {
            Distribution target = distributionMap.get(rel.getDistributionInstance().getInstanceId());
            if (target != null && distributionIds.add(target.getInstanceId())) {
                o.addDistribution(createLinkedEntity(target, EntityNames.DISTRIBUTION.name()));
            }
        }
        
        for (DataproductSpatial rel : spatials.getOrDefault(instanceId, Collections.emptyList())) {
            Spatial target = spatialMap.get(rel.getSpatialInstance().getInstanceId());
            if (target != null) {
                o.addSpatialExtentItem(createLinkedEntity(target, EntityNames.LOCATION.name()));
            }
        }
        
        for (DataproductTemporal rel : temporals.getOrDefault(instanceId, Collections.emptyList())) {
            Temporal target = temporalMap.get(rel.getTemporalInstance().getInstanceId());
            if (target != null) {
                o.addTemporalExtent(createLinkedEntity(target, EntityNames.PERIODOFTIME.name()));
            }
        }
        
        for (DataproductElement rel : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = rel.getElementInstance();
            if (el != null) {
                if (ElementType.REFERENCEDBY.name().equals(el.getType())) o.addReferencedBy(el.getValue());
                if (ElementType.LANDINGPAGE.name().equals(el.getType())) o.addLandingPage(el.getValue());
                if (ElementType.VARIABLEMEASURED.name().equals(el.getType())) o.addVariableMeasured(el.getValue());
            }
        }
        
        // Apply versioning from pre-fetched data
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) {
                o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            }
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try {
                    o.setStatus(StatusType.valueOf(vs.getStatus()));
                } catch (Exception e) {
                    // Ignore invalid status
                }
            }
            o.setFileProvenance(vs.getProvenance());
        }
        
        // Apply groups from pre-fetched data
        if (o.getMetaId() != null && groupsMap != null) {
            List<String> groups = groupsMap.get(o.getMetaId());
            o.setGroups(groups != null ? groups : Collections.emptyList());
        }
        
        return o;
    }

    /**
     * Creates a LinkedEntity from a JPA entity.
     */
    private LinkedEntity createLinkedEntity(Object entity, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(utilities.ReflectionCache.getInstanceId(entity));
        le.setMetaId(utilities.ReflectionCache.getMetaId(entity));
        le.setUid(utilities.ReflectionCache.getUid(entity));
        le.setEntityType(entityType);
        return le;
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
