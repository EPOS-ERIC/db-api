package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwaresourcecode_contactpoint", schema = "metadata_catalogue")
public class SoftwaresourcecodeContactpoint {
    @EmbeddedId
    private SoftwaresourcecodeContactpointId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecodeInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public SoftwaresourcecodeContactpointId getId() {
        return id;
    }

    public void setId(SoftwaresourcecodeContactpointId id) {
        this.id = id;
    }

    public Softwaresourcecode getSoftwaresourcecodeInstance() {
        return softwaresourcecodeInstance;
    }

    public void setSoftwaresourcecodeInstance(Softwaresourcecode softwaresourcecodeInstance) {
        this.softwaresourcecodeInstance = softwaresourcecodeInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}