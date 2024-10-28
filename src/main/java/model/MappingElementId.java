package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class MappingElementId implements java.io.Serializable {
    private static final long serialVersionUID = -5673897650662453749L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "mapping_instance_id", nullable = false, length = 100)
    private String mappingInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getMappingInstanceId() {
        return mappingInstanceId;
    }

    public void setMappingInstanceId(String mappingInstanceId) {
        this.mappingInstanceId = mappingInstanceId;
    }

    public String getElementInstanceId() {
        return elementInstanceId;
    }

    public void setElementInstanceId(String elementInstanceId) {
        this.elementInstanceId = elementInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        MappingElementId entity = (MappingElementId) o;
        return Objects.equals(this.mappingInstanceId, entity.mappingInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingInstanceId, elementInstanceId);
    }

}