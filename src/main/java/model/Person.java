package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "person", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Person {
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
    @Column(name = "familyname", length = 1024)
    private String familyname;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "givenname", length = 1024)
    private String givenname;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "qualifications", length = 1024)
    private String qualifications;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "cvurl", length = 1024)
    private String cvurl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

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

    public String getFamilyname() {
        return familyname;
    }

    public void setFamilyname(String familyname) {
        this.familyname = familyname;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getCvurl() {
        return cvurl;
    }

    public void setCvurl(String cvurl) {
        this.cvurl = cvurl;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

}