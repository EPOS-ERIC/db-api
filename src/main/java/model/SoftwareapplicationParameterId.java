package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.util.Objects;

@Embeddable
public class SoftwareapplicationParameterId implements java.io.Serializable {
    private static final long serialVersionUID = 923285777304050903L;
    @Size(max = 100)
    @NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "parameter_instance_id", nullable = false, length = 100)
    private String parameterInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
    }

    public String getParameterInstanceId() {
        return parameterInstanceId;
    }

    public void setParameterInstanceId(String parameterInstanceId) {
        this.parameterInstanceId = parameterInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        SoftwareapplicationParameterId entity = (SoftwareapplicationParameterId) o;
        return Objects.equals(this.parameterInstanceId, entity.parameterInstanceId) &&
                Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterInstanceId, softwareapplicationInstanceId);
    }

}