package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ServiceIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = -4437055528023658406L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "service_instance_id", nullable = false, length = 100)
    private String serviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getIdentifierInstanceId() {
        return identifierInstanceId;
    }

    public void setIdentifierInstanceId(String identifierInstanceId) {
        this.identifierInstanceId = identifierInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        ServiceIdentifierId entity = (ServiceIdentifierId) o;
        return Objects.equals(this.identifierInstanceId, entity.identifierInstanceId) &&
                Objects.equals(this.serviceInstanceId, entity.serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierInstanceId, serviceInstanceId);
    }

}