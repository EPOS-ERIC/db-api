package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "webservice", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Webservice {
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
    private Versioningstatus version;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "schemaidentifier", length = 1024)
    private String schemaidentifier;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "name", length = 1024)
    private String name;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "entrypoint", length = 1024)
    private String entrypoint;

    @Column(name = "datapublished")
    private LocalDateTime datapublished;

    @Column(name = "datamodified")
    private LocalDateTime datamodified;

    @Lob
    @Column(name = "keywords")
    private String keywords;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "license", length = 1024)
    private String license;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "provider", length = 100)
    private String provider;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "aaaitypes", length = 1024)
    private String aaaitypes;

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

    public String getSchemaidentifier() {
        return schemaidentifier;
    }

    public void setSchemaidentifier(String schemaidentifier) {
        this.schemaidentifier = schemaidentifier;
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

    public String getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public LocalDateTime getDatapublished() {
        return datapublished;
    }

    public void setDatapublished(LocalDateTime datapublished) {
        this.datapublished = datapublished;
    }

    public LocalDateTime getDatamodified() {
        return datamodified;
    }

    public void setDatamodified(LocalDateTime datamodified) {
        this.datamodified = datamodified;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAaaitypes() {
        return aaaitypes;
    }

    public void setAaaitypes(String aaaitypes) {
        this.aaaitypes = aaaitypes;
    }

}