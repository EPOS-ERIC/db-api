package relationsapi;

import abstractapis.AbstractRelationsAPI;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import relationsapi.RelationSyncUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactPointRelationsAPI extends AbstractRelationsAPI {

    private static final Logger LOG = Logger.getLogger(ContactPointRelationsAPI.class.getName());

    /**
     * Equipment - ContactPoint relation
     * @param previousObj The previous version of the entity (for versioning support)
     */
    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Equipment instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                EquipmentContactpoint.class, Contactpoint.class,
                "equipmentInstance",
                EquipmentContactpoint::getContactpointInstance,
                EquipmentContactpoint::setEquipmentInstance,
                EquipmentContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
    }

    /**
     * Facility - ContactPoint relation
     */
    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Facility instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                FacilityContactpoint.class, Contactpoint.class,
                "facilityInstance",
                FacilityContactpoint::getContactpointInstance,
                FacilityContactpoint::setFacilityInstance,
                FacilityContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
    }

    /**
     * DataProduct - ContactPoint relation
     */
    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Dataproduct instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                DataproductContactpoint.class, Contactpoint.class,
                "dataproductInstance",
                DataproductContactpoint::getContactpointInstance,
                DataproductContactpoint::setDataproductInstance,
                DataproductContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
    }

    /**
     * WebService - ContactPoint relation
     */
    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Webservice instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                WebserviceContactpoint.class, Contactpoint.class,
                "webserviceInstance",
                WebserviceContactpoint::getContactpointInstance,
                WebserviceContactpoint::setWebserviceInstance,
                WebserviceContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
    }

    /**
     * SoftwareSourceCode - ContactPoint relation
     */
    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Softwaresourcecode instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                SoftwaresourcecodeContactpoint.class, Contactpoint.class,
                "softwaresourcecodeInstance",
                SoftwaresourcecodeContactpoint::getContactpointInstance,
                SoftwaresourcecodeContactpoint::setSoftwaresourcecodeInstance,
                SoftwaresourcecodeContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
    }

    /**
     * SoftwareApplication - ContactPoint relation
     */
    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus, EPOSDataModelEntity previousObj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[ContactPointRelationsAPI] Creating ContactPoint relation for Softwareapplication instanceId: {0}", edmobj.getInstanceId());
        }
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                SoftwareapplicationContactpoint.class, Contactpoint.class,
                "softwareapplicationInstance",
                SoftwareapplicationContactpoint::getContactpointInstance,
                SoftwareapplicationContactpoint::setSoftwareapplicationInstance,
                SoftwareapplicationContactpoint::setContactpointInstance,
                obj, previousObj, overrideStatus, false
        );
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
