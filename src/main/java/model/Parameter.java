package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "parameter")
public class Parameter {
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
    @Column(name = "encodingformat", length = 1024)
    private String encodingformat;

    @Size(max = 1024)
    @Column(name = "conformsto", length = 1024)
    private String conformsto;

    @Size(max = 1024)
    @Column(name = "action", length = 1024)
    private String action;

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

    public String getEncodingformat() {
        return encodingformat;
    }

    public void setEncodingformat(String encodingformat) {
        this.encodingformat = encodingformat;
    }

    public String getConformsto() {
        return conformsto;
    }

    public void setConformsto(String conformsto) {
        this.conformsto = conformsto;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}