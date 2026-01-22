package relationsapi;

import abstractapis.AbstractRelationsAPI;
import model.*;
import relationsapi.RelationSyncUtil;

public class ContactPointRelationsAPI extends AbstractRelationsAPI {

    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    EquipmentContactpoint.class, Contactpoint.class,
                    "equipmentInstance",
                    EquipmentContactpoint::getContactpointInstance,
                    EquipmentContactpoint::setEquipmentInstance,
                    EquipmentContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus) {
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    FacilityContactpoint.class, Contactpoint.class,
                    "facilityInstance",
                    FacilityContactpoint::getContactpointInstance,
                    FacilityContactpoint::setFacilityInstance,
                    FacilityContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus) {
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    DataproductContactpoint.class, Contactpoint.class,
                    "dataproductInstance",
                    DataproductContactpoint::getContactpointInstance,
                    DataproductContactpoint::setDataproductInstance,
                    DataproductContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    WebserviceContactpoint.class, Contactpoint.class,
                    "webserviceInstance",
                    WebserviceContactpoint::getContactpointInstance,
                    WebserviceContactpoint::setWebserviceInstance,
                    WebserviceContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    SoftwaresourcecodeContactpoint.class, Contactpoint.class,
                    "softwaresourcecodeInstance",
                    SoftwaresourcecodeContactpoint::getContactpointInstance,
                    SoftwaresourcecodeContactpoint::setSoftwaresourcecodeInstance,
                    SoftwaresourcecodeContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getContactPoint(), null, null,
                    SoftwareapplicationContactpoint.class, Contactpoint.class,
                    "softwareapplicationInstance",
                    SoftwareapplicationContactpoint::getContactpointInstance,
                    SoftwareapplicationContactpoint::setSoftwareapplicationInstance,
                    SoftwareapplicationContactpoint::setContactpointInstance,
                    obj, null, overrideStatus, false
            );
        }
    }
}