package abstractapis;

import commonapis.*;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;

public abstract class AbstractAPI<T> {

    protected Class<?> edmClass;
    protected String entityName;

    protected EposDataModelDAO dbaccess = new EposDataModelDAO();

    public AbstractAPI(String entityName, Class<?> edmClass){
        this.edmClass = edmClass;
        this.entityName = entityName;
    }

    public EposDataModelDAO getDbaccess(){
        return dbaccess;
    }

    public void setEdmClass(Class<?> edmClass){
        this.edmClass = edmClass;
    }

    public Class<?> getEdmClass(){
        return edmClass;
    }

    public void setEntityName(String entityName){
        this.entityName = entityName;
    }

    public  String getEntityName() {
        return entityName;
    }

    public abstract LinkedEntity create(T obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate);

    public abstract T retrieve(String instanceId);

    public abstract Boolean delete(String instanceId);

    public abstract List<T> retrieveBunch(List<String> entities);

    public abstract List<T> retrieveAll();

    public abstract LinkedEntity retrieveLinkedEntity(String instanceId);

    public static AbstractAPI retrieveAPI(String entityType){
        AbstractAPI api = null;
        Class<?> edmClass = null;

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
        return api;
    }

    public static Class retrieveClass(String entityType){

        Class<?> edmClass = null;

        switch(EntityNames.valueOf(entityType)){
            case PERSON:
                edmClass = Person.class;
                break;
            case MAPPING:
                edmClass = Mapping.class;
                break;
            case CATEGORY:
                edmClass = Category.class;
                break;
            case FACILITY:
                edmClass = Facility.class;
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                break;
            case OPERATION:
                edmClass = Operation.class;
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                break;
            case SOFTWARESOURCECODE:
                edmClass = Softwaresourcecode.class;
                break;
            case SOFTWAREAPPLICATION:
                edmClass = Softwareapplication.class;
                break;
            case ADDRESS:
                edmClass = Address.class;
                break;
            case ELEMENT:
                edmClass = Element.class;
                break;
            case LOCATION:
                edmClass = Spatial.class;
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                break;
            case QUANTITATIVEVALUE:
                edmClass = Quantitativevalue.class;
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                break;
            case PARAMETER:
                edmClass = SoftwareapplicationParameter.class;
                break;
            case RELATION:
                System.out.println("Relation empty case");
                break;
        }
        return edmClass;
    }


}
