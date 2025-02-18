package relationsapi;

import abstractapis.AbstractRelationsAPI;
import commonapis.LinkedEntityAPI;
import metadataapis.CategoryAPI;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRelationsAPI extends AbstractRelationsAPI {

    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus){
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(EquipmentCategory.class)){
                EquipmentCategory item = (EquipmentCategory) object;
                if(item.getEquipmentInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    EquipmentCategory pi = new EquipmentCategory();
                    pi.setEquipmentInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(FacilityCategory.class)){
                FacilityCategory item = (FacilityCategory) object;
                if(item.getFacilityInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    FacilityCategory pi = new FacilityCategory();
                    pi.setFacilityInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(DataproductCategory.class)){
                DataproductCategory item = (DataproductCategory) object;
                if(item.getDataproductInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    DataproductCategory pi = new DataproductCategory();
                    pi.setDataproductInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(WebserviceCategory.class)){
                WebserviceCategory item = (WebserviceCategory) object;
                if(item.getWebserviceInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    WebserviceCategory pi = new WebserviceCategory();
                    pi.setWebserviceInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeCategory.class)){
                SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
                if(item.getSoftwaresourcecodeInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    SoftwaresourcecodeCategory pi = new SoftwaresourcecodeCategory();
                    pi.setSoftwaresourcecodeInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus) {
        if (obj.getCategory() != null) {
            for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationCategory.class)){
                SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
                if(item.getSoftwareapplicationInstance().getInstanceId().equals(obj.getInstanceId())){
                    dbaccess.deleteObject(item);
                }
            }
            for(org.epos.eposdatamodel.LinkedEntity category : obj.getCategory()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(category, overrideStatus);
                List<Category> categoryList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Category.class);
                if(!categoryList.isEmpty()) {
                    SoftwareapplicationCategory pi = new SoftwareapplicationCategory();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setCategoryInstance(categoryList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }
}
