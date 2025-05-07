package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "softwaresourcecode_element")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class SoftwaresourcecodeElement {
    @EmbeddedId
    private SoftwaresourcecodeElementId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecodeInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public SoftwaresourcecodeElementId getId() {
        return id;
    }

    public void setId(SoftwaresourcecodeElementId id) {
        this.id = id;
    }

    public Softwaresourcecode getSoftwaresourcecodeInstance() {
        return softwaresourcecodeInstance;
    }

    public void setSoftwaresourcecodeInstance(Softwaresourcecode softwaresourcecodeInstance) {
        this.softwaresourcecodeInstance = softwaresourcecodeInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}