package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment")
public class Equipment {
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

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "identifier", length = 1024)
    private String identifier;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "name", length = 1024)
    private String name;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "type", length = 1024)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "creator")
    private model.Organization creator;

    @Lob
    @Column(name = "keywords")
    private String keywords;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "pageurl", length = 1024)
    private String pageurl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "filter", length = 1024)
    private String filter;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "dynamicrange", length = 100)
    private String dynamicrange;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "orientation", length = 1024)
    private String orientation;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "resolution", length = 1024)
    private String resolution;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "sampleperiod", length = 100)
    private String sampleperiod;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "serialnumber", length = 1024)
    private String serialnumber;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public model.Organization getCreator() {
        return creator;
    }

    public void setCreator(model.Organization creator) {
        this.creator = creator;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPageurl() {
        return pageurl;
    }

    public void setPageurl(String pageurl) {
        this.pageurl = pageurl;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getDynamicrange() {
        return dynamicrange;
    }

    public void setDynamicrange(String dynamicrange) {
        this.dynamicrange = dynamicrange;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getSampleperiod() {
        return sampleperiod;
    }

    public void setSampleperiod(String sampleperiod) {
        this.sampleperiod = sampleperiod;
    }

    public String getSerialnumber() {
        return serialnumber;
    }

    public void setSerialnumber(String serialnumber) {
        this.serialnumber = serialnumber;
    }

}