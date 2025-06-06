package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "attribution_role", schema = "metadata_catalogue")
public class AttributionRole {
    @Id
    @Size(max = 100)
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;

    @Size(max = 100)
    @Column(name = "meta_id", length = 100)
    private String metaId;

    @Size(max = 1024)
    @Column(name = "uid", length = 1024)
    private String uid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id")
    private Versioningstatus version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribution_instance_id")
    private Attribution attributionInstance;

    @Size(max = 1024)
    @Column(name = "roletype", length = 1024)
    private String roletype;

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

    public Versioningstatus getVersion() {
        return version;
    }

    public void setVersion(Versioningstatus version) {
        this.version = version;
    }

    public Attribution getAttributionInstance() {
        return attributionInstance;
    }

    public void setAttributionInstance(Attribution attributionInstance) {
        this.attributionInstance = attributionInstance;
    }

    public String getRoletype() {
        return roletype;
    }

    public void setRoletype(String roletype) {
        this.roletype = roletype;
    }

}