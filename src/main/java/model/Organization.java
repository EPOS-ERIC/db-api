package model;

import jakarta.persistence.*;

@Entity
@Table(name = "organization", schema = "metadata_catalogue")
public class Organization {
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id")
    private model.Versioningstatus version;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "acronym", length = 1024)
    private String acronym;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "legalname", length = 1024)
    private String legalname;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "leicode", length = 1024)
    private String leicode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id")
    private Address address;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "logo", length = 1024)
    private String logo;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "url", length = 1024)
    private String url;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "type", length = 1024)
    private String type;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "maturity", length = 1024)
    private String maturity;

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

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getLegalname() {
        return legalname;
    }

    public void setLegalname(String legalname) {
        this.legalname = legalname;
    }

    public String getLeicode() {
        return leicode;
    }

    public void setLeicode(String leicode) {
        this.leicode = leicode;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaturity() {
        return maturity;
    }

    public void setMaturity(String maturity) {
        this.maturity = maturity;
    }

}