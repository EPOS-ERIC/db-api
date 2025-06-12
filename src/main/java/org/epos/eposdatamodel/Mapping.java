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
public class Mapping extends EPOSDataModelEntity {

    /**
     * This property contains the default value of the parameter.
     **/
    @Schema(name = "defaultValue", description = "This property contains the default value of the parameter.", example = "15", required = false)
    private String defaultValue;

    /**
     * This property contains a short string used to describe the meaning of the parameter.
     **/
    @Schema(name = "label", description = "This property contains a short string used to describe the meaning of the parameter.", example = "Parameter label", required = false)
    private String label;

    /**
     * This property contains the maximum value of the parameter.
     **/
    @Schema(name = "maxValue", description = "This property contains the maximum value of the parameter.", example = "20", required = false)
    private String maxValue;

    /**
     * This property contains the minimum value of the parameter.
     **/
    @Schema(name = "minValue", description = "This property contains the minimum value of the parameter.", example = "12", required = false)
    private String minValue;

    /**
     * This property contains one of the possible values which should be used in the web service query. It could be repeated as many times as needed.
     **/
    @Schema(name = "paramValue", description = "This property contains one of the possible values which should be used in the web service query. It could be repeated as many times as needed.", example = "[\"val1\"]", required = false)
    private List<String> paramValue;

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
     * This property is a boolean property that describe if the mapping has multiple values.
     */
    @Schema(name = "multipleValues", description = "This property is a boolean property that describe if the mapping has multiple values.", example = "true", required = false)
    private String multipleValues;

    /**
     * This property is a boolean property which if is marked as true means that the parameters need to be set but
     * it isn't showed on the gui and the user cannot interact with it
     */
    @Schema(name = "readOnlyValue", description = "This property is a boolean property which if is marked as true means that the parameters need to be set but it isn't showed on the gui and the user cannot interact with it", example = "true", required = false)
    private String readOnlyValue;

    /**
     * This property contains the name of the parameter as required by web service specifications.
     **/
    @Schema(name = "healthCheckVariable", description = "This property contains the healthCheckVariable of the parameter as required by web service specifications for monitoring.", example = "eventid", required = false)
    private String healthCheckVariable;


    public void addParamValue(String paramValue) {
        if (this.getParamValue() == null) {
            ArrayList<String> paramValueList = new ArrayList<>();
            paramValueList.add(paramValue);
            this.setParamValue(paramValueList);
        } else {
            this.getParamValue().add(paramValue);
        }
    }


    public Mapping defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * This property contains the default value of the parameter.
     *
     * @return defaultValue
     **/

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Mapping label(String label) {
        this.label = label;
        return this;
    }

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

    public Mapping maxValue(String maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    /**
     * This property contains the maximum value of the parameter.
     *
     * @return maxValue
     **/

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public Mapping minValue(String minValue) {
        this.minValue = minValue;
        return this;
    }

    /**
     * This property contains the minimum value of the parameter.
     *
     * @return minValue
     **/

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public Mapping paramValue(List<String> paramValue) {
        this.paramValue = paramValue;
        return this;
    }

    public Mapping addParamValueItem(String paramValueItem) {
        if (this.paramValue == null) {
            this.paramValue = new ArrayList<>();
        }
        this.paramValue.add(paramValueItem);
        return this;
    }

    /**
     * This property contains one of the possible values which should be used in the web service query. It could be repeated as many times as needed.
     *
     * @return paramValue
     **/

    public List<String> getParamValue() {
        return paramValue;
    }

    public void setParamValue(List<String> paramValue) {
        this.paramValue = paramValue;
    }

    public Mapping property(String property) {
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

    public Mapping range(String range) {
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

    public Mapping required(String required) {
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

    public Mapping valuePattern(String valuePattern) {
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

    public Mapping variable(String variable) {
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

    public String getMultipleValues() {
        return multipleValues;
    }

    public void setMultipleValues(String multipleValues) {
        this.multipleValues = multipleValues;
    }

    public String getReadOnlyValue() {
        return readOnlyValue;
    }

    public void setReadOnlyValue(String readOnlyValue) {
        this.readOnlyValue = readOnlyValue;
    }

    public String getHealthCheckVariable() {
        return healthCheckVariable;
    }

    public void setHealthCheckVariable(String healthCheckVariable) {
        this.healthCheckVariable = healthCheckVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Mapping mapping = (Mapping) o;
        return Objects.equals(defaultValue, mapping.defaultValue) && Objects.equals(label, mapping.label) && Objects.equals(maxValue, mapping.maxValue) && Objects.equals(minValue, mapping.minValue) && Objects.equals(paramValue, mapping.paramValue) && Objects.equals(property, mapping.property) && Objects.equals(range, mapping.range) && Objects.equals(required, mapping.required) && Objects.equals(valuePattern, mapping.valuePattern) && Objects.equals(variable, mapping.variable) && Objects.equals(multipleValues, mapping.multipleValues) && Objects.equals(readOnlyValue, mapping.readOnlyValue) && Objects.equals(healthCheckVariable, mapping.healthCheckVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defaultValue, label, maxValue, minValue, paramValue, property, range, required, valuePattern, variable, multipleValues, readOnlyValue, healthCheckVariable);
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "defaultValue='" + defaultValue + '\'' +
                ", label='" + label + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", minValue='" + minValue + '\'' +
                ", paramValue=" + paramValue +
                ", property='" + property + '\'' +
                ", range='" + range + '\'' +
                ", required='" + required + '\'' +
                ", valuePattern='" + valuePattern + '\'' +
                ", variable='" + variable + '\'' +
                ", multipleValues='" + multipleValues + '\'' +
                ", readOnlyValue='" + readOnlyValue + '\'' +
                ", healthCheckVariable='" + healthCheckVariable + '\'' +
                '}'+ super.toString();
    }
}
