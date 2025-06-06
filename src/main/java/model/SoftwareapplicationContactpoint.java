package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_contactpoint", schema = "metadata_catalogue")
public class SoftwareapplicationContactpoint {
    @EmbeddedId
    private SoftwareapplicationContactpointId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public SoftwareapplicationContactpointId getId() {
        return id;
    }

    public void setId(SoftwareapplicationContactpointId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}