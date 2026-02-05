package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwareapplicationIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = -2182117433978288328L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
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
        SoftwareapplicationIdentifierId entity = (SoftwareapplicationIdentifierId) o;
        return Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, identifierInstanceId);
    }

}