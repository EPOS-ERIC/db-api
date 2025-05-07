package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "publication")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Publication {
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
    @Column(name = "name", length = 1024)
    private String name;

    @Column(name = "published")
    private LocalDateTime published;

    @Lob
    @Column(name = "abstracttext")
    private String abstracttext;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "licenseurl", length = 1024)
    private String licenseurl;

    @Lob
    @Column(name = "keywords")
    private String keywords;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "issn", length = 1024)
    private String issn;

    @Column(name = "pages")
    private Integer pages;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "volumenumber", length = 1024)
    private String volumenumber;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "author", length = 1024)
    private String author;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "publisher", length = 100)
    private String publisher;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getPublished() {
        return published;
    }

    public void setPublished(LocalDateTime published) {
        this.published = published;
    }

    public String getAbstracttext() {
        return abstracttext;
    }

    public void setAbstracttext(String abstracttext) {
        this.abstracttext = abstracttext;
    }

    public String getLicenseurl() {
        return licenseurl;
    }

    public void setLicenseurl(String licenseurl) {
        this.licenseurl = licenseurl;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public String getVolumenumber() {
        return volumenumber;
    }

    public void setVolumenumber(String volumenumber) {
        this.volumenumber = volumenumber;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

}