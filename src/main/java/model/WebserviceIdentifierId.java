package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = 2226703229028280083L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
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
        WebserviceIdentifierId entity = (WebserviceIdentifierId) o;
        return Objects.equals(this.identifierInstanceId, entity.identifierInstanceId) &&
                Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierInstanceId, webserviceInstanceId);
    }

}