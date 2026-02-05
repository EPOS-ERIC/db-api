package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = 1326042749596488830L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
    }

    public String getContactpointInstanceId() {
        return contactpointInstanceId;
    }

    public void setContactpointInstanceId(String contactpointInstanceId) {
        this.contactpointInstanceId = contactpointInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        WebserviceContactpointId entity = (WebserviceContactpointId) o;
        return Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webserviceInstanceId, contactpointInstanceId);
    }

}