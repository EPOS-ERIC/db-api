package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "output_mapping", schema = "metadata_catalogue")
public class OutputMapping {
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id")
    private Versioningstatus version;

    @Size(max = 1024)
    @Column(name = "label", length = 1024)
    private String label;

    @Size(max = 1024)
    @Column(name = "variable", length = 1024)
    private String variable;

    @Column(name = "required")
    private Boolean required;

    @Size(max = 1024)
    @Column(name = "range", length = 1024)
    private String range;

    @Size(max = 1024)
    @Column(name = "property", length = 1024)
    private String property;

    @Size(max = 1024)
    @Column(name = "valuepattern", length = 1024)
    private String valuepattern;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValuepattern() {
        return valuepattern;
    }

    public void setValuepattern(String valuepattern) {
        this.valuepattern = valuepattern;
    }

}