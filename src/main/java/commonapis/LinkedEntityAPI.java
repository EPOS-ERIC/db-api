package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LinkedEntityAPI {

    public static LinkedEntity createFromLinkedEntity(LinkedEntity obj, StatusType overrideStatus){

        AbstractAPI api = null;
        Class<?> edmClass = null;
        EPOSDataModelEntity entity = null;
        switch(EntityNames.valueOf(obj.getEntityType())){
            case PERSON:
                edmClass = Person.class;
                api = new PersonAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Person();
                break;
            case MAPPING:
                edmClass = Mapping.class;
                api = new MappingAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Mapping();
                break;
            case CATEGORY:
                edmClass = Category.class;
                api = new CategoryAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Category();
                break;
            case FACILITY:
                edmClass = Facility.class;
                api = new FacilityAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Facility();
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                api = new EquipmentAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Equipment();
                break;
            case OPERATION:
                edmClass = Operation.class;
                api = new OperationAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Operation();
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                api = new WebServiceAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.WebService();
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                api = new DataProductAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.DataProduct();
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                api = new ContactPointAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.ContactPoint();
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                api = new DistributionAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Distribution();
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                api = new OrganizationAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Organization();
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                api = new CategorySchemeAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.CategoryScheme();
                break;
            case SOFTWARESOURCECODE:
                edmClass = Softwaresourcecode.class;
                api = new SoftwareSourceCodeAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.SoftwareSourceCode();
                break;
            case SOFTWAREAPPLICATION:
                edmClass = Softwareapplication.class;
                api = new SoftwareApplicationAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.SoftwareApplication();
                break;
            case ADDRESS:
                edmClass = Address.class;
                api = new AddressAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Address();
                break;
            case ELEMENT:
                edmClass = Element.class;
                api = new ElementAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Element();
                break;
            case LOCATION:
                edmClass = Spatial.class;
                api = new SpatialAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Location();
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                api = new TemporalAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.PeriodOfTime();
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                api = new IdentifierAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Identifier();
                break;
            case QUANTITATIVEVALUE:
                edmClass = Quantitativevalue.class;
                api = new QuantitativeValueAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.QuantitativeValue();
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                api = new DocumentationAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Documentation();
                break;
            case PARAMETER:
                edmClass = SoftwareapplicationParameter.class;
                api = new ParameterAPI(obj.getEntityType(), edmClass);
                entity = new org.epos.eposdatamodel.Parameter();
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
                return api.create(entity, overrideStatus);
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
        switch(EntityNames.valueOf(obj.getEntityType())){
            case PERSON:
                edmClass = Person.class;
                api = new PersonAPI(obj.getEntityType(), edmClass);
                break;
            case MAPPING:
                edmClass = Mapping.class;
                api = new MappingAPI(obj.getEntityType(), edmClass);
                break;
            case CATEGORY:
                edmClass = Category.class;
                api = new CategoryAPI(obj.getEntityType(), edmClass);
                break;
            case FACILITY:
                edmClass = Facility.class;
                api = new FacilityAPI(obj.getEntityType(), edmClass);
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                api = new EquipmentAPI(obj.getEntityType(), edmClass);
                break;
            case OPERATION:
                edmClass = Operation.class;
                api = new OperationAPI(obj.getEntityType(), edmClass);
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                api = new WebServiceAPI(obj.getEntityType(), edmClass);
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                api = new DataProductAPI(obj.getEntityType(), edmClass);
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                api = new ContactPointAPI(obj.getEntityType(), edmClass);
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                api = new DistributionAPI(obj.getEntityType(), edmClass);
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                api = new OrganizationAPI(obj.getEntityType(), edmClass);
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                api = new CategorySchemeAPI(obj.getEntityType(), edmClass);
                break;
            case SOFTWARESOURCECODE:
                edmClass = Softwaresourcecode.class;
                api = new SoftwareSourceCodeAPI(obj.getEntityType(), edmClass);
                break;
            case SOFTWAREAPPLICATION:
                edmClass = Softwareapplication.class;
                api = new SoftwareApplicationAPI(obj.getEntityType(), edmClass);
                break;
            case ADDRESS:
                edmClass = Address.class;
                api = new AddressAPI(obj.getEntityType(), edmClass);
                break;
            case ELEMENT:
                edmClass = Element.class;
                api = new ElementAPI(obj.getEntityType(), edmClass);
                break;
            case LOCATION:
                edmClass = Spatial.class;
                api = new SpatialAPI(obj.getEntityType(), edmClass);
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                api = new TemporalAPI(obj.getEntityType(), edmClass);
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                api = new IdentifierAPI(obj.getEntityType(), edmClass);
                break;
            case QUANTITATIVEVALUE:
                edmClass = Quantitativevalue.class;
                api = new QuantitativeValueAPI(obj.getEntityType(), edmClass);
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                api = new DocumentationAPI(obj.getEntityType(), edmClass);
                break;
            case PARAMETER:
                edmClass = SoftwareapplicationParameter.class;
                api = new ParameterAPI(obj.getEntityType(), edmClass);
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
