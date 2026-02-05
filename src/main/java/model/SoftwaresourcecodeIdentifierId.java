package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwaresourcecodeIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = 8452888726987251876L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
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
        SoftwaresourcecodeIdentifierId entity = (SoftwaresourcecodeIdentifierId) o;
        return Objects.equals(this.softwaresourcecodeInstanceId, entity.softwaresourcecodeInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwaresourcecodeInstanceId, identifierInstanceId);
    }

}