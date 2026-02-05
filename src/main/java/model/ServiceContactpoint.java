package model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_contactpoint", schema = "metadata_catalogue")
public class ServiceContactpoint {
    @EmbeddedId
    private ServiceContactpointId id;

    @MapsId("serviceInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_instance_id", nullable = false)
    private Service serviceInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public ServiceContactpointId getId() {
        return id;
    }

    public void setId(ServiceContactpointId id) {
        this.id = id;
    }

    public Service getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Service serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}