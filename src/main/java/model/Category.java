package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "category", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Category {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "meta_id", length = 100)
    private String metaId;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "uid", length = 1024)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private model.Versioningstatus version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_scheme")
    private model.CategoryScheme inScheme;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 255)
    @Column(name = "name")
    private String name;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public model.Versioningstatus getVersion() {
        return version;
    }

    public void setVersion(model.Versioningstatus version) {
        this.version = version;
    }

    public model.CategoryScheme getInScheme() {
        return inScheme;
    }

    public void setInScheme(model.CategoryScheme inScheme) {
        this.inScheme = inScheme;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}