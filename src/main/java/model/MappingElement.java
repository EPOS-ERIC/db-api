package model;

import jakarta.persistence.*;

@Entity
@Table(name = "mapping_element", schema = "metadata_catalogue")
public class MappingElement {
    @EmbeddedId
    private MappingElementId id;

    @MapsId("mappingInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_instance_id", nullable = false)
    private Mapping mappingInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public MappingElementId getId() {
        return id;
    }

    public void setId(MappingElementId id) {
        this.id = id;
    }

    public Mapping getMappingInstance() {
        return mappingInstance;
    }

    public void setMappingInstance(Mapping mappingInstance) {
        this.mappingInstance = mappingInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}