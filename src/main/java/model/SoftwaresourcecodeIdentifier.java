package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwaresourcecode_identifier")
public class SoftwaresourcecodeIdentifier {
    @EmbeddedId
    private SoftwaresourcecodeIdentifierId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecodeInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private Identifier identifierInstance;

    public SoftwaresourcecodeIdentifierId getId() {
        return id;
    }

    public void setId(SoftwaresourcecodeIdentifierId id) {
        this.id = id;
    }

    public Softwaresourcecode getSoftwaresourcecodeInstance() {
        return softwaresourcecodeInstance;
    }

    public void setSoftwaresourcecodeInstance(Softwaresourcecode softwaresourcecodeInstance) {
        this.softwaresourcecodeInstance = softwaresourcecodeInstance;
    }

    public Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}