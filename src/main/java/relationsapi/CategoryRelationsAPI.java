package relationsapi;

import abstractapis.AbstractRelationsAPI;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import relationsapi.RelationSyncUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CategoryRelationsAPI extends AbstractRelationsAPI {

    private static final Logger LOG = Logger.getLogger(CategoryRelationsAPI.class.getName());

    /**
     * Equipment - Category relation
     * @param previousObj The previous version of the entity (for versioning support)
     */
    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Equipment instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    EquipmentCategory.class, Category.class,
                    "equipmentInstance",
                    EquipmentCategory::getCategoryInstance,
                    EquipmentCategory::setEquipmentInstance,
                    EquipmentCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * Facility - Category relation
     */
    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Facility instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    FacilityCategory.class, Category.class,
                    "facilityInstance",
                    FacilityCategory::getCategoryInstance,
                    FacilityCategory::setFacilityInstance,
                    FacilityCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * DataProduct - Category relation
     */
    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Dataproduct instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    DataproductCategory.class, Category.class,
                    "dataproductInstance",
                    DataproductCategory::getCategoryInstance,
                    DataproductCategory::setDataproductInstance,
                    DataproductCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * WebService - Category relation
     */
    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Webservice instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    WebserviceCategory.class, Category.class,
                    "webserviceInstance",
                    WebserviceCategory::getCategoryInstance,
                    WebserviceCategory::setWebserviceInstance,
                    WebserviceCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * SoftwareSourceCode - Category relation
     */
    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Softwaresourcecode instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    SoftwaresourcecodeCategory.class, Category.class,
                    "softwaresourcecodeInstance",
                    SoftwaresourcecodeCategory::getCategoryInstance,
                    SoftwaresourcecodeCategory::setSoftwaresourcecodeInstance,
                    SoftwaresourcecodeCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * SoftwareApplication - Category relation
     */
    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[CategoryRelationsAPI] Creating Category relation for Softwareapplication instanceId: {0}", edmobj.getInstanceId());
        }
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    SoftwareapplicationCategory.class, Category.class,
                    "softwareapplicationInstance",
                    SoftwareapplicationCategory::getCategoryInstance,
                    SoftwareapplicationCategory::setSoftwareapplicationInstance,
                    SoftwareapplicationCategory::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }
    }

    /**
     * @deprecated Use {@link #createRelation(Equipment, org.epos.eposdatamodel.Equipment, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }

    /**
     * @deprecated Use {@link #createRelation(Facility, org.epos.eposdatamodel.Facility, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }

    /**
     * @deprecated Use {@link #createRelation(Dataproduct, org.epos.eposdatamodel.DataProduct, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }

    /**
     * @deprecated Use {@link #createRelation(Webservice, org.epos.eposdatamodel.WebService, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }

    /**
     * @deprecated Use {@link #createRelation(Softwaresourcecode, org.epos.eposdatamodel.SoftwareSourceCode, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }

    /**
     * @deprecated Use {@link #createRelation(Softwareapplication, org.epos.eposdatamodel.SoftwareApplication, StatusType, EPOSDataModelEntity)} instead
     */
    @Deprecated
    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus) {
        createRelation(edmobj, obj, overrideStatus, null);
    }
}