package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceDistributionId implements java.io.Serializable {
    private static final long serialVersionUID = 6904808250901264132L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "distribution_instance_id", nullable = false, length = 100)
    private String distributionInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
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
        WebserviceDistributionId entity = (WebserviceDistributionId) o;
        return Objects.equals(this.distributionInstanceId, entity.distributionInstanceId) &&
                Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distributionInstanceId, webserviceInstanceId);
    }

}