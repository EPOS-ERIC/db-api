package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplicationInputParameter;
import org.epos.eposdatamodel.SoftwareApplicationOutputParameter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class LinkedEntityAPI {

    private static final Map<String, AbstractAPI> apiMap = new HashMap<>();
    private static final Map<String, Class<?>> edmClassMap = new HashMap<>();

    static {
        apiMap.put(EntityNames.PERSON.name(), new PersonAPI(EntityNames.PERSON.name(), Person.class));
        apiMap.put(EntityNames.MAPPING.name(), new MappingAPI(EntityNames.MAPPING.name(), Mapping.class));
        apiMap.put(EntityNames.CATEGORY.name(), new CategoryAPI(EntityNames.CATEGORY.name(), Category.class));
        apiMap.put(EntityNames.FACILITY.name(), new FacilityAPI(EntityNames.FACILITY.name(), Facility.class));
        apiMap.put(EntityNames.EQUIPMENT.name(), new EquipmentAPI(EntityNames.EQUIPMENT.name(), Equipment.class));
        apiMap.put(EntityNames.OPERATION.name(), new OperationAPI(EntityNames.OPERATION.name(), Operation.class));
        apiMap.put(EntityNames.WEBSERVICE.name(), new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class));
        apiMap.put(EntityNames.DATAPRODUCT.name(), new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class));
        apiMap.put(EntityNames.CONTACTPOINT.name(), new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class));
        apiMap.put(EntityNames.DISTRIBUTION.name(), new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class));
        apiMap.put(EntityNames.ORGANIZATION.name(), new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class));
        apiMap.put(EntityNames.CATEGORYSCHEME.name(), new CategorySchemeAPI(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class));
        apiMap.put(EntityNames.SOFTWARESOURCECODE.name(), new SoftwareSourceCodeAPI(EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATION.name(), new SoftwareApplicationAPI(EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class));
        apiMap.put(EntityNames.ADDRESS.name(), new AddressAPI(EntityNames.ADDRESS.name(), Address.class));
        apiMap.put(EntityNames.ELEMENT.name(), new ElementAPI(EntityNames.ELEMENT.name(), Element.class));
        apiMap.put(EntityNames.LOCATION.name(), new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class));
        apiMap.put(EntityNames.PERIODOFTIME.name(), new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class));
        apiMap.put(EntityNames.IDENTIFIER.name(), new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class));
        apiMap.put(EntityNames.QUANTITATIVEVALUE.name(), new QuantitativeValueAPI(EntityNames.QUANTITATIVEVALUE.name(), Quantitativevalue.class));
        apiMap.put(EntityNames.DOCUMENTATION.name(), new DocumentationAPI(EntityNames.DOCUMENTATION.name(), Element.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), SoftwareApplicationInputParameter.class));
        apiMap.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), new ParameterAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), SoftwareApplicationOutputParameter.class));

        edmClassMap.put(EntityNames.PERSON.name(), org.epos.eposdatamodel.Person.class);
        edmClassMap.put(EntityNames.MAPPING.name(), org.epos.eposdatamodel.Mapping.class);
        edmClassMap.put(EntityNames.CATEGORY.name(), org.epos.eposdatamodel.Category.class);
        edmClassMap.put(EntityNames.FACILITY.name(), org.epos.eposdatamodel.Facility.class);
        edmClassMap.put(EntityNames.EQUIPMENT.name(), org.epos.eposdatamodel.Equipment.class);
        edmClassMap.put(EntityNames.OPERATION.name(), org.epos.eposdatamodel.Operation.class);
        edmClassMap.put(EntityNames.WEBSERVICE.name(), org.epos.eposdatamodel.WebService.class);
        edmClassMap.put(EntityNames.DATAPRODUCT.name(), org.epos.eposdatamodel.DataProduct.class);
        edmClassMap.put(EntityNames.CONTACTPOINT.name(), org.epos.eposdatamodel.ContactPoint.class);
        edmClassMap.put(EntityNames.DISTRIBUTION.name(), org.epos.eposdatamodel.Distribution.class);
        edmClassMap.put(EntityNames.ORGANIZATION.name(), org.epos.eposdatamodel.Organization.class);
        edmClassMap.put(EntityNames.CATEGORYSCHEME.name(), org.epos.eposdatamodel.CategoryScheme.class);
        edmClassMap.put(EntityNames.SOFTWARESOURCECODE.name(), org.epos.eposdatamodel.SoftwareSourceCode.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATION.name(), org.epos.eposdatamodel.SoftwareApplication.class);
        edmClassMap.put(EntityNames.ADDRESS.name(), org.epos.eposdatamodel.Address.class);
        edmClassMap.put(EntityNames.ELEMENT.name(), org.epos.eposdatamodel.Element.class);
        edmClassMap.put(EntityNames.LOCATION.name(), org.epos.eposdatamodel.Location.class);
        edmClassMap.put(EntityNames.PERIODOFTIME.name(), org.epos.eposdatamodel.PeriodOfTime.class);
        edmClassMap.put(EntityNames.IDENTIFIER.name(), org.epos.eposdatamodel.Identifier.class);
        edmClassMap.put(EntityNames.QUANTITATIVEVALUE.name(), org.epos.eposdatamodel.QuantitativeValue.class);
        edmClassMap.put(EntityNames.DOCUMENTATION.name(), org.epos.eposdatamodel.Documentation.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(), org.epos.eposdatamodel.SoftwareApplicationInputParameter.class);
        edmClassMap.put(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), org.epos.eposdatamodel.SoftwareApplicationOutputParameter.class);
    }

    public static LinkedEntity createFromLinkedEntity(LinkedEntity obj, StatusType overrideStatus){
        AbstractAPI api = apiMap.get(obj.getEntityType().toUpperCase());
        Class<?> edmClass = edmClassMap.get(obj.getEntityType().toUpperCase());

        if (api != null && edmClass != null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    null,
                    Versioningstatus.class
            );

            if (returnList.isEmpty()) {
                EPOSDataModelEntity entity = null;
                try {
                    entity = (EPOSDataModelEntity) edmClass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                entity.setInstanceId(Optional.ofNullable(obj.getInstanceId()).orElse(UUID.randomUUID().toString()));
                entity.setMetaId(Optional.ofNullable(obj.getMetaId()).orElse(UUID.randomUUID().toString()));
                entity.setUid(Optional.ofNullable(obj.getUid()).orElse(UUID.randomUUID().toString()));
                if (overrideStatus != null) entity.setStatus(overrideStatus);
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
        AbstractAPI api = apiMap.get(obj.getEntityType().toUpperCase());
        Class<?> edmClass = edmClassMap.get(obj.getEntityType().toUpperCase());

        if (api != null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    null,
                    Versioningstatus.class
            );

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
