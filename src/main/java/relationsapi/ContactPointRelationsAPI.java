package relationsapi;

import abstractapis.AbstractRelationsAPI;
import commonapis.LinkedEntityAPI;
import metadataapis.ContactPointAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContactPointRelationsAPI extends AbstractRelationsAPI {

    public static void createRelation(Equipment edmobj, org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    EquipmentContactpoint pi = new EquipmentContactpoint();
                    pi.setEquipmentInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Facility edmobj, org.epos.eposdatamodel.Facility obj, StatusType overrideStatus) {
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    FacilityContactpoint pi = new FacilityContactpoint();
                    pi.setFacilityInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Dataproduct edmobj, org.epos.eposdatamodel.DataProduct obj, StatusType overrideStatus) {
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    DataproductContactpoint pi = new DataproductContactpoint();
                    pi.setDataproductInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Webservice edmobj, org.epos.eposdatamodel.WebService obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    WebserviceContactpoint pi = new WebserviceContactpoint();
                    pi.setWebserviceInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Softwaresourcecode edmobj, org.epos.eposdatamodel.SoftwareSourceCode obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    SoftwaresourcecodeContactpoint pi = new SoftwaresourcecodeContactpoint();
                    pi.setSoftwaresourcecodeInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }

    public static void createRelation(Softwareapplication edmobj, org.epos.eposdatamodel.SoftwareApplication obj, StatusType overrideStatus){
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
            for(org.epos.eposdatamodel.LinkedEntity contactPoint : obj.getContactPoint()){
                LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(contactPoint, overrideStatus);
                List<Contactpoint> contactpointList = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Contactpoint.class);
                if(!contactpointList.isEmpty()) {
                    SoftwareapplicationContactpoint pi = new SoftwareapplicationContactpoint();
                    pi.setSoftwareapplicationInstance(edmobj);
                    pi.setContactpointInstance(contactpointList.get(0));
                    dbaccess.updateObject(pi);
                }
            }
        }
    }
}
