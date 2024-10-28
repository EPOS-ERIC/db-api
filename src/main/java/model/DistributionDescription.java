package model;

import jakarta.persistence.*;

@Entity
@Table(name = "distribution_description")
public class DistributionDescription {
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "version_id")
    private model.Versioningstatus version;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "lang", length = 50)
    private String lang;

    @jakarta.validation.constraints.NotNull
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "distribution_instance_id", nullable = false)
    private Distribution distributionInstance;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Distribution getDistributionInstance() {
        return distributionInstance;
    }

    public void setDistributionInstance(Distribution distributionInstance) {
        this.distributionInstance = distributionInstance;
    }

}