package model;

import jakarta.persistence.*;

@Entity
@Table(name = "webservice_category")
public class WebserviceCategory {
    @EmbeddedId
    private WebserviceCategoryId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public WebserviceCategoryId getId() {
        return id;
    }

    public void setId(WebserviceCategoryId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}