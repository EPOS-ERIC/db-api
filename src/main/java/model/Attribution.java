package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "attribution", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Attribution {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private Versioningstatus version;

    @Size(max = 1024)
    @Column(name = "agent_id", length = 1024)
    private String agentId;

    @Size(max = 1024)
    @Column(name = "agent_type", length = 1024)
    private String agentType;

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

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

}