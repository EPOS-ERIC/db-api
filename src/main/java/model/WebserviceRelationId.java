package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WebserviceRelationId implements Serializable {
    private static final long serialVersionUID = 5557402518980442780L;
    @Size(max = 100)
    @NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
    }

    public String getEntityInstanceId() {
        return entityInstanceId;
    }

    public void setEntityInstanceId(String entityInstanceId) {
        this.entityInstanceId = entityInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        WebserviceRelationId entity = (WebserviceRelationId) o;
        return Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId) &&
                Objects.equals(this.entityInstanceId, entity.entityInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webserviceInstanceId, entityInstanceId);
    }

}