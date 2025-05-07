package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "authorization_group")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class AuthorizationGroup {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private model.MetadataGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id")
    private model.EdmEntityId meta;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public model.MetadataGroup getGroup() {
        return group;
    }

    public void setGroup(model.MetadataGroup group) {
        this.group = group;
    }

    public model.EdmEntityId getMeta() {
        return meta;
    }

    public void setMeta(model.EdmEntityId meta) {
        this.meta = meta;
    }

}