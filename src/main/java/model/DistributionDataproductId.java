package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DistributionDataproductId implements java.io.Serializable {
    private static final long serialVersionUID = 5967839325970861174L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "distribution_instance_id", nullable = false, length = 100)
    private String distributionInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getDistributionInstanceId() {
        return distributionInstanceId;
    }

    public void setDistributionInstanceId(String distributionInstanceId) {
        this.distributionInstanceId = distributionInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DistributionDataproductId entity = (DistributionDataproductId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.distributionInstanceId, entity.distributionInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, distributionInstanceId);
    }

}