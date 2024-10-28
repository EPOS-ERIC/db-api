package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication")
public class Softwareapplication {
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
    @Column(name = "name", length = 1024)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "downloadurl", length = 1024)
    private String downloadurl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "licenseurl", length = 1024)
    private String licenseurl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "softwareversion", length = 1024)
    private String softwareversion;

    @Lob
    @Column(name = "keywords")
    private String keywords;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "requirements", length = 1024)
    private String requirements;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "installurl", length = 1024)
    private String installurl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "mainentityofpage", length = 1024)
    private String mainentityofpage;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadurl() {
        return downloadurl;
    }

    public void setDownloadurl(String downloadurl) {
        this.downloadurl = downloadurl;
    }

    public String getLicenseurl() {
        return licenseurl;
    }

    public void setLicenseurl(String licenseurl) {
        this.licenseurl = licenseurl;
    }

    public String getSoftwareversion() {
        return softwareversion;
    }

    public void setSoftwareversion(String softwareversion) {
        this.softwareversion = softwareversion;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getInstallurl() {
        return installurl;
    }

    public void setInstallurl(String installurl) {
        this.installurl = installurl;
    }

    public String getMainentityofpage() {
        return mainentityofpage;
    }

    public void setMainentityofpage(String mainentityofpage) {
        this.mainentityofpage = mainentityofpage;
    }

}