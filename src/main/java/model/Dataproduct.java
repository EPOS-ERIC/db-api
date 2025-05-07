package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dataproduct")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Dataproduct {
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

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "identifier", length = 1024)
    private String identifier;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "issued")
    private LocalDateTime issued;

    @Column(name = "modified")
    private LocalDateTime modified;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "versioninfo", length = 1024)
    private String versioninfo;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "type", length = 1024)
    private String type;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "accrualperiodicity", length = 1024)
    private String accrualperiodicity;

    @Lob
    @Column(name = "keywords")
    private String keywords;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "accessright", length = 1024)
    private String accessright;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "documentation", length = 1024)
    private String documentation;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "qualityassurance", length = 1024)
    private String qualityassurance;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "has_quality_annotation", length = 1024)
    private String hasQualityAnnotation;

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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
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

    public String getVersioninfo() {
        return versioninfo;
    }

    public void setVersioninfo(String versioninfo) {
        this.versioninfo = versioninfo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccrualperiodicity() {
        return accrualperiodicity;
    }

    public void setAccrualperiodicity(String accrualperiodicity) {
        this.accrualperiodicity = accrualperiodicity;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getAccessright() {
        return accessright;
    }

    public void setAccessright(String accessright) {
        this.accessright = accessright;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getQualityassurance() {
        return qualityassurance;
    }

    public void setQualityassurance(String qualityassurance) {
        this.qualityassurance = qualityassurance;
    }

    public String getHasQualityAnnotation() {
        return hasQualityAnnotation;
    }

    public void setHasQualityAnnotation(String hasQualityAnnotation) {
        this.hasQualityAnnotation = hasQualityAnnotation;
    }

}