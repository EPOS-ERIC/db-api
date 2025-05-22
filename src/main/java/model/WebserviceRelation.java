package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "webservice_relation", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class WebserviceRelation {
    @EmbeddedId
    private WebserviceRelationId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @Size(max = 100)
    @NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public WebserviceRelationId getId() {
        return id;
    }

    public void setId(WebserviceRelationId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public String getResourceEntity() {
        return resourceEntity;
    }

    public void setResourceEntity(String resourceEntity) {
        this.resourceEntity = resourceEntity;
    }

}