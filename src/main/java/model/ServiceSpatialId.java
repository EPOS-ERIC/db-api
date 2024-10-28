package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ServiceSpatialId implements java.io.Serializable {
    private static final long serialVersionUID = -3627615381211372878L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "service_instance_id", nullable = false, length = 100)
    private String serviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "spatial_instance_id", nullable = false, length = 100)
    private String spatialInstanceId;

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getSpatialInstanceId() {
        return spatialInstanceId;
    }

    public void setSpatialInstanceId(String spatialInstanceId) {
        this.spatialInstanceId = spatialInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        ServiceSpatialId entity = (ServiceSpatialId) o;
        return Objects.equals(this.spatialInstanceId, entity.spatialInstanceId) &&
                Objects.equals(this.serviceInstanceId, entity.serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spatialInstanceId, serviceInstanceId);
    }

}