package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ServiceTemporalId implements java.io.Serializable {
    private static final long serialVersionUID = 7963513551527842792L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "service_instance_id", nullable = false, length = 100)
    private String serviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "temporal_instance_id", nullable = false, length = 100)
    private String temporalInstanceId;

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getTemporalInstanceId() {
        return temporalInstanceId;
    }

    public void setTemporalInstanceId(String temporalInstanceId) {
        this.temporalInstanceId = temporalInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        ServiceTemporalId entity = (ServiceTemporalId) o;
        return Objects.equals(this.serviceInstanceId, entity.serviceInstanceId) &&
                Objects.equals(this.temporalInstanceId, entity.temporalInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceInstanceId, temporalInstanceId);
    }

}