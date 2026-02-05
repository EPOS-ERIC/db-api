package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * This class allow to represent the implementation status of a Service made available by a Service Provider.
 */
public class QuantitativeValue extends EPOSDataModelEntity{
    /**
     * The unit of measurement given using the UN/CEFACT Common Code (3 characters) or a URL
     */
    @Schema(name = "unit", description = "The unit of measurement given using the UN/CEFACT Common Code (3 characters) or a URL", example = "UNT", required = false)
    private String unit;

    /**
     * The unit of measurement given using the UN/CEFACT Common Code (3 characters) or a URL
     */
    @Schema(name = "value", description = "The unit of measurement given using the UN/CEFACT Common Code (3 characters) or a URL", example = "value", required = false)
    private String value;


    public String getUnit() {
        return unit;
    }


    public void setUnit(String unit) {
        this.unit = unit;
    }


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "QuantitativeValue{" +
                "unit='" + unit + '\'' +
                ", value='" + value + '\'' +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuantitativeValue that = (QuantitativeValue) o;
        return Objects.equals(getUnit(), that.getUnit()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnit(), getValue());
    }
}
