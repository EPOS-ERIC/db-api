package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class allows to map a variable used in the template to a property and may optionally specify whether that
 * variable is required or not. The syntax of the template literal is specified by its datatype and defaults to the
 * [RFC6570] URI Template syntax, which can be explicitly indicated by hydra:Rfc6570Template.
 */
public class OutputMapping extends EPOSDataModelEntity {

    /**
     * This property contains a short string used to describe the meaning of the parameter.
     **/
    @Schema(name = "label", description = "This property contains a short string used to describe the meaning of the parameter.", example = "Parameter label", required = false)
    private String label;

    /**
     * This property contains the vocabulary term which indicates the semantic description of parameter.
     **/
    @Schema(name = "property", description = "This property contains the vocabulary term which indicates the semantic description of parameter.", example = "schema:endDate", required = false)
    private String property;

    /**
     * This property contains the type of parameter
     **/
    @Schema(name = "range", description = "This property contains the type of parameter", example = "string", required = false)
    private String range;

    /**
     * This property contains true if the property is required, false otherwise.
     **/
    @Schema(name = "required", description = "This property contains true if the property is required, false otherwise.", example = "true", required = false)
    private String required;

    /**
     * This property contains the regular expression for testing values according to the parameters specification.
     **/
    @Schema(name = "valuePattern", description = "This property contains the regular expression for testing values according to the parameters specification.", example = "yyyy-MM-dd", required = false)
    private String valuePattern;

    /**
     * This property contains the name of the parameter as required by web service specifications.
     **/
    @Schema(name = "variable", description = "This property contains the name of the parameter as required by web service specifications.", example = "eventid", required = false)
    private String variable;

    /**
     * This property contains a short string used to describe the meaning of the parameter.
     *
     * @return label
     **/

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OutputMapping property(String property) {
        this.property = property;
        return this;
    }

    /**
     * This property contains the vocabulary term which indicates the semantic description of parameter.
     *
     * @return property
     **/

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public OutputMapping range(String range) {
        this.range = range;
        return this;
    }

    /**
     * This property contains the type of parameter
     *
     * @return range
     **/

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public OutputMapping required(String required) {
        this.required = required;
        return this;
    }

    /**
     * This property contains true if the property is required, false otherwise.
     *
     * @return required
     **/

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public OutputMapping valuePattern(String valuePattern) {
        this.valuePattern = valuePattern;
        return this;
    }

    /**
     * This property contains the regular expression for testing values according to the parameters specification.
     *
     * @return valuePattern
     **/

    public String getValuePattern() {
        return valuePattern;
    }

    public void setValuePattern(String valuePattern) {
        this.valuePattern = valuePattern;
    }

    public OutputMapping variable(String variable) {
        this.variable = variable;
        return this;
    }

    /**
     * This property contains the name of the parameter as required by web service specifications.
     *
     * @return variable
     **/

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OutputMapping)) return false;
        if (!super.equals(o)) return false;
        OutputMapping that = (OutputMapping) o;
        return Objects.equals(label, that.label) && Objects.equals(property, that.property) && Objects.equals(range, that.range) && Objects.equals(required, that.required) && Objects.equals(valuePattern, that.valuePattern) && Objects.equals(variable, that.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, property, range, required, valuePattern, variable);
    }

    @Override
    public String toString() {
        return "OutputMapping{" +
                "label='" + label + '\'' +
                ", property='" + property + '\'' +
                ", range='" + range + '\'' +
                ", required='" + required + '\'' +
                ", valuePattern='" + valuePattern + '\'' +
                ", variable='" + variable + '\'' +
                '}';
    }
}
