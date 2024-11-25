package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class LinkedEntityAPI {

    public static LinkedEntity createFromLinkedEntity(LinkedEntity obj, StatusType overrideStatus){

        AbstractAPI api = null;
        Class<?> edmClass = null;
        EPOSDataModelEntity entity = null;
        String entityType = obj.getEntityType().toUpperCase();
        switch(EntityNames.valueOf(entityType)){
            case PERSON:
                edmClass = Person.class;
                api = new PersonAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Person();
                break;
            case MAPPING:
                edmClass = Mapping.class;
                api = new MappingAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Mapping();
                break;
            case CATEGORY:
                edmClass = Category.class;
                api = new CategoryAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Category();
                break;
            case FACILITY:
                edmClass = Facility.class;
                api = new FacilityAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Facility();
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                api = new EquipmentAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Equipment();
                break;
            case OPERATION:
                edmClass = Operation.class;
                api = new OperationAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Operation();
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                api = new WebServiceAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.WebService();
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                api = new DataProductAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.DataProduct();
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                api = new ContactPointAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.ContactPoint();
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                api = new DistributionAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Distribution();
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                api = new OrganizationAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Organization();
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                api = new CategorySchemeAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.CategoryScheme();
                break;
            case SOFTWARESOURCECODE:
                edmClass = Softwaresourcecode.class;
                api = new SoftwareSourceCodeAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.SoftwareSourceCode();
                break;
            case SOFTWAREAPPLICATION:
                edmClass = Softwareapplication.class;
                api = new SoftwareApplicationAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.SoftwareApplication();
                break;
            case ADDRESS:
                edmClass = Address.class;
                api = new AddressAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Address();
                break;
            case ELEMENT:
                edmClass = Element.class;
                api = new ElementAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Element();
                break;
            case LOCATION:
                edmClass = Spatial.class;
                api = new SpatialAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Location();
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                api = new TemporalAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.PeriodOfTime();
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                api = new IdentifierAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Identifier();
                break;
            case QUANTITATIVEVALUE:
                edmClass = Quantitativevalue.class;
                api = new QuantitativeValueAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.QuantitativeValue();
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                api = new DocumentationAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.Documentation();
                break;
            case PARAMETER:
                edmClass = Parameter.class;
                api = new ParameterAPI(entityType, edmClass);
                entity = new org.epos.eposdatamodel.SoftwareApplicationParameter();
                break;
            case RELATION:
                System.out.println("Relation empty case");
                break;
        }

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                Optional.ofNullable(obj.getInstanceId()).orElse(null),
                Optional.ofNullable(obj.getMetaId()).orElse(null),
                Optional.ofNullable(obj.getUid()).orElse(null),
                null,
                Versioningstatus.class
        );

        System.out.println("------------------------");
        System.out.println(returnList);
        System.out.println("------------------------");

        if(api!=null && entity!=null) {
            if (returnList.isEmpty()) {
                entity.setInstanceId(Optional.ofNullable(obj.getInstanceId()).orElse(UUID.randomUUID().toString()));
                entity.setMetaId(Optional.ofNullable(obj.getMetaId()).orElse(UUID.randomUUID().toString()));
                entity.setUid(Optional.ofNullable(obj.getUid()).orElse(UUID.randomUUID().toString()));
                if(overrideStatus!=null) entity.setStatus(overrideStatus);
                return api.create(entity, overrideStatus, null, null);
            } else {
                Versioningstatus versioningstatus = returnList.get(0);
                obj.setInstanceId(versioningstatus.getInstanceId());
                obj.setMetaId(versioningstatus.getMetaId());
                return obj;
            }
        }
        return null;
    }


    public static Object retrieveFromLinkedEntity(LinkedEntity obj) {

        AbstractAPI api = null;
        Class<?> edmClass = null;
        String entityType = obj.getEntityType().toUpperCase();

        switch(EntityNames.valueOf(entityType)){
            case PERSON:
                edmClass = Person.class;
                api = new PersonAPI(entityType, edmClass);
                break;
            case MAPPING:
                edmClass = Mapping.class;
                api = new MappingAPI(entityType, edmClass);
                break;
            case CATEGORY:
                edmClass = Category.class;
                api = new CategoryAPI(entityType, edmClass);
                break;
            case FACILITY:
                edmClass = Facility.class;
                api = new FacilityAPI(entityType, edmClass);
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                api = new EquipmentAPI(entityType, edmClass);
                break;
            case OPERATION:
                edmClass = Operation.class;
                api = new OperationAPI(entityType, edmClass);
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                api = new WebServiceAPI(entityType, edmClass);
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                api = new DataProductAPI(entityType, edmClass);
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                api = new ContactPointAPI(entityType, edmClass);
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                api = new DistributionAPI(entityType, edmClass);
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                api = new OrganizationAPI(entityType, edmClass);
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                api = new CategorySchemeAPI(entityType, edmClass);
                break;
            case SOFTWARESOURCECODE:
                edmClass = Softwaresourcecode.class;
                api = new SoftwareSourceCodeAPI(entityType, edmClass);
                break;
            case SOFTWAREAPPLICATION:
                edmClass = Softwareapplication.class;
                api = new SoftwareApplicationAPI(entityType, edmClass);
                break;
            case ADDRESS:
                edmClass = Address.class;
                api = new AddressAPI(entityType, edmClass);
                break;
            case ELEMENT:
                edmClass = Element.class;
                api = new ElementAPI(entityType, edmClass);
                break;
            case LOCATION:
                edmClass = Spatial.class;
                api = new SpatialAPI(entityType, edmClass);
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                api = new TemporalAPI(entityType, edmClass);
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                api = new IdentifierAPI(entityType, edmClass);
                break;
            case QUANTITATIVEVALUE:
                edmClass = Quantitativevalue.class;
                api = new QuantitativeValueAPI(entityType, edmClass);
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                api = new DocumentationAPI(entityType, edmClass);
                break;
            case PARAMETER:
                edmClass = SoftwareapplicationParameter.class;
                api = new ParameterAPI(entityType, edmClass);
                break;
            case RELATION:
                System.out.println("Relation empty case");
                break;
        }

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                Optional.ofNullable(obj.getInstanceId()).orElse(null),
                Optional.ofNullable(obj.getMetaId()).orElse(null),
                Optional.ofNullable(obj.getUid()).orElse(null),
                null,
                Versioningstatus.class
        );

        if(api!=null) {
            if (!returnList.isEmpty()) {
                Versioningstatus edmobj = returnList.get(0);
                return api.retrieve(edmobj.getInstanceId());
            }
        } else {
            return createFromLinkedEntity(obj, null);
        }
        return null;
    }

    private static EposDataModelDAO getDbaccess() {
        return new EposDataModelDAO();
    }


}
