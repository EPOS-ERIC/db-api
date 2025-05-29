package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_identifier", schema = "metadata_catalogue")
public class SoftwareapplicationIdentifier {
    @EmbeddedId
    private SoftwareapplicationIdentifierId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private Identifier identifierInstance;

    public SoftwareapplicationIdentifierId getId() {
        return id;
    }

    public void setId(SoftwareapplicationIdentifierId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}