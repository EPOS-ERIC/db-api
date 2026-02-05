package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceTemporalId implements java.io.Serializable {
    private static final long serialVersionUID = -8913902307263613718L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "temporal_instance_id", nullable = false, length = 100)
    private String temporalInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
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
        WebserviceTemporalId entity = (WebserviceTemporalId) o;
        return Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId) &&
                Objects.equals(this.temporalInstanceId, entity.temporalInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webserviceInstanceId, temporalInstanceId);
    }

}