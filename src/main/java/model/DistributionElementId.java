package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DistributionElementId implements java.io.Serializable {
    private static final long serialVersionUID = 7059131312769524985L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "distribution_instance_id", nullable = false, length = 100)
    private String distributionInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getDistributionInstanceId() {
        return distributionInstanceId;
    }

    public void setDistributionInstanceId(String distributionInstanceId) {
        this.distributionInstanceId = distributionInstanceId;
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
        DistributionElementId entity = (DistributionElementId) o;
        return Objects.equals(this.distributionInstanceId, entity.distributionInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distributionInstanceId, elementInstanceId);
    }

}