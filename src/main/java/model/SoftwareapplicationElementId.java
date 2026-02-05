package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SoftwareapplicationElementId implements Serializable {
    private static final long serialVersionUID = 6290219082120387191L;
    @Size(max = 100)
    @NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
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
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        SoftwareapplicationElementId entity = (SoftwareapplicationElementId) o;
        return Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, elementInstanceId);
    }

}