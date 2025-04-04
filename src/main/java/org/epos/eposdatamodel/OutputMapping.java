package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

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
    private String outputLabel;

    /**
     * This property contains the vocabulary term which indicates the semantic description of parameter.
     **/
    @Schema(name = "property", description = "This property contains the vocabulary term which indicates the semantic description of parameter.", example = "schema:endDate", required = false)
    private String outputProperty;

    /**
     * This property contains the type of parameter
     **/
    @Schema(name = "range", description = "This property contains the type of parameter", example = "string", required = false)
    private String outputRange;

    /**
     * This property contains true if the property is required, false otherwise.
     **/
    @Schema(name = "required", description = "This property contains true if the property is required, false otherwise.", example = "true", required = false)
    private String outputRequired;

    /**
     * This property contains the regular expression for testing values according to the parameters specification.
     **/
    @Schema(name = "valuePattern", description = "This property contains the regular expression for testing values according to the parameters specification.", example = "yyyy-MM-dd", required = false)
    private String outputValuePattern;

    /**
     * This property contains the name of the parameter as required by web service specifications.
     **/
    @Schema(name = "variable", description = "This property contains the name of the parameter as required by web service specifications.", example = "eventid", required = false)
    private String outputVariable;

    /**
     * This property contains a short string used to describe the meaning of the parameter.
     *
     * @return label
     **/

    public String getOutputLabel() {
        return outputLabel;
    }

    public void setOutputLabel(String outputLabel) {
        this.outputLabel = outputLabel;
    }

    public OutputMapping property(String property) {
        this.outputProperty = property;
        return this;
    }

    /**
     * This property contains the vocabulary term which indicates the semantic description of parameter.
     *
     * @return property
     **/

    public String getOutputProperty() {
        return outputProperty;
    }

    public void setOutputProperty(String outputProperty) {
        this.outputProperty = outputProperty;
    }

    public OutputMapping range(String range) {
        this.outputRange = range;
        return this;
    }

    /**
     * This property contains the type of parameter
     *
     * @return range
     **/

    public String getOutputRange() {
        return outputRange;
    }

    public void setOutputRange(String outputRange) {
        this.outputRange = outputRange;
    }

    public OutputMapping required(String required) {
        this.outputRequired = required;
        return this;
    }

    /**
     * This property contains true if the property is required, false otherwise.
     *
     * @return required
     **/

    public String getOutputRequired() {
        return outputRequired;
    }

    public void setOutputRequired(String outputRequired) {
        this.outputRequired = outputRequired;
    }

    public OutputMapping valuePattern(String valuePattern) {
        this.outputValuePattern = valuePattern;
        return this;
    }

    /**
     * This property contains the regular expression for testing values according to the parameters specification.
     *
     * @return valuePattern
     **/

    public String getOutputValuePattern() {
        return outputValuePattern;
    }

    public void setOutputValuePattern(String outputValuePattern) {
        this.outputValuePattern = outputValuePattern;
    }

    public OutputMapping variable(String variable) {
        this.outputVariable = variable;
        return this;
    }

    /**
     * This property contains the name of the parameter as required by web service specifications.
     *
     * @return variable
     **/

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OutputMapping)) return false;
        if (!super.equals(o)) return false;
        OutputMapping that = (OutputMapping) o;
        return Objects.equals(outputLabel, that.outputLabel) && Objects.equals(outputProperty, that.outputProperty) && Objects.equals(outputRange, that.outputRange) && Objects.equals(outputRequired, that.outputRequired) && Objects.equals(outputValuePattern, that.outputValuePattern) && Objects.equals(outputVariable, that.outputVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), outputLabel, outputProperty, outputRange, outputRequired, outputValuePattern, outputVariable);
    }

    @Override
    public String toString() {
        return "OutputMapping{" +
                "label='" + outputLabel + '\'' +
                ", property='" + outputProperty + '\'' +
                ", range='" + outputRange + '\'' +
                ", required='" + outputRequired + '\'' +
                ", valuePattern='" + outputValuePattern + '\'' +
                ", variable='" + outputVariable + '\'' +
                '}';
    }
}
