package relationsapi;

import abstractapis.AbstractRelationsAPI;
import model.*;
import relationsapi.RelationSyncUtil;

public class CategoryRelationsAPI extends AbstractRelationsAPI {

    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus){
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    EquipmentCategory.class, Category.class,
                    "equipmentInstance",
                    EquipmentCategory::getCategoryInstance,
                    EquipmentCategory::setEquipmentInstance,
                    EquipmentCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    FacilityCategory.class, Category.class,
                    "facilityInstance",
                    FacilityCategory::getCategoryInstance,
                    FacilityCategory::setFacilityInstance,
                    FacilityCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    DataproductCategory.class, Category.class,
                    "dataproductInstance",
                    DataproductCategory::getCategoryInstance,
                    DataproductCategory::setDataproductInstance,
                    DataproductCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    WebserviceCategory.class, Category.class,
                    "webserviceInstance",
                    WebserviceCategory::getCategoryInstance,
                    WebserviceCategory::setWebserviceInstance,
                    WebserviceCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    SoftwaresourcecodeCategory.class, Category.class,
                    "softwaresourcecodeInstance",
                    SoftwaresourcecodeCategory::getCategoryInstance,
                    SoftwaresourcecodeCategory::setSoftwaresourcecodeInstance,
                    SoftwaresourcecodeCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }

    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getCategory(), null, null,
                    SoftwareapplicationCategory.class, Category.class,
                    "softwareapplicationInstance",
                    SoftwareapplicationCategory::getCategoryInstance,
                    SoftwareapplicationCategory::setSoftwareapplicationInstance,
                    SoftwareapplicationCategory::setCategoryInstance,
                    obj, null, overrideStatus, false
            );
        }
    }
}