package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwaresourcecodeElementId implements java.io.Serializable {
    private static final long serialVersionUID = 7451318007206152580L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
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
        SoftwaresourcecodeElementId entity = (SoftwaresourcecodeElementId) o;
        return Objects.equals(this.softwaresourcecodeInstanceId, entity.softwaresourcecodeInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwaresourcecodeInstanceId, elementInstanceId);
    }

}