package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribution", schema = "metadata_catalogue")
public class Distribution {
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

    @Column(name = "issued")
    private LocalDateTime issued;

    @Column(name = "modified")
    private LocalDateTime modified;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "type", length = 1024)
    private String type;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "format", length = 1024)
    private String format;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "license", length = 1024)
    private String license;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "datapolicy", length = 1024)
    private String datapolicy;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "mediatype", length = 1024)
    private String mediaType;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "maturity", length = 1024)
    private String maturity;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "bytesize", length = 1024)
    private String byteSize;


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

    public LocalDateTime getIssued() {
        return issued;
    }

    public void setIssued(LocalDateTime issued) {
        this.issued = issued;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getDatapolicy() {
        return datapolicy;
    }

    public void setDatapolicy(String datapolicy) {
        this.datapolicy = datapolicy;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMaturity() {
        return maturity;
    }

    public void setMaturity(String maturity) {
        this.maturity = maturity;
    }

    public String getByteSize() {
        return byteSize;
    }

    public void setByteSize(String byteSize) {
        this.byteSize = byteSize;
    }
}