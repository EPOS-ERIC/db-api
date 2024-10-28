package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceSpatialId implements java.io.Serializable {
    private static final long serialVersionUID = -9028994346874057089L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "spatial_instance_id", nullable = false, length = 100)
    private String spatialInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
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
        WebserviceSpatialId entity = (WebserviceSpatialId) o;
        return Objects.equals(this.spatialInstanceId, entity.spatialInstanceId) &&
                Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spatialInstanceId, webserviceInstanceId);
    }

}