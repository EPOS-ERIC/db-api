package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WebserviceCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -1662627375348249814L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "webservice_instance_id", nullable = false, length = 100)
    private String webserviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getWebserviceInstanceId() {
        return webserviceInstanceId;
    }

    public void setWebserviceInstanceId(String webserviceInstanceId) {
        this.webserviceInstanceId = webserviceInstanceId;
    }

    public String getCategoryInstanceId() {
        return categoryInstanceId;
    }

    public void setCategoryInstanceId(String categoryInstanceId) {
        this.categoryInstanceId = categoryInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        WebserviceCategoryId entity = (WebserviceCategoryId) o;
        return Objects.equals(this.categoryInstanceId, entity.categoryInstanceId) &&
                Objects.equals(this.webserviceInstanceId, entity.webserviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryInstanceId, webserviceInstanceId);
    }

}