package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "mapping", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Mapping {
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
    @Column(name = "label", length = 1024)
    private String label;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "variable", length = 1024)
    private String variable;

    @Column(name = "required")
    private Boolean required;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "range", length = 1024)
    private String range;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "defaultvalue", length = 1024)
    private String defaultvalue;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "minvalue", length = 1024)
    private String minvalue;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "maxvalue", length = 1024)
    private String maxvalue;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "property", length = 1024)
    private String property;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "valuepattern", length = 1024)
    private String valuepattern;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "read_only_value", length = 1024)
    private String readOnlyValue;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "multiple_values", length = 1024)
    private String multipleValues;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "ismappingof", length = 100)
    private String ismappingof;

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

    public String getDefaultvalue() {
        return defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue) {
        this.defaultvalue = defaultvalue;
    }

    public String getMinvalue() {
        return minvalue;
    }

    public void setMinvalue(String minvalue) {
        this.minvalue = minvalue;
    }

    public String getMaxvalue() {
        return maxvalue;
    }

    public void setMaxvalue(String maxvalue) {
        this.maxvalue = maxvalue;
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

    public String getReadOnlyValue() {
        return readOnlyValue;
    }

    public void setReadOnlyValue(String readOnlyValue) {
        this.readOnlyValue = readOnlyValue;
    }

    public String getMultipleValues() {
        return multipleValues;
    }

    public void setMultipleValues(String multipleValues) {
        this.multipleValues = multipleValues;
    }

    public String getIsmappingof() {
        return ismappingof;
    }

    public void setIsmappingof(String ismappingof) {
        this.ismappingof = ismappingof;
    }

}