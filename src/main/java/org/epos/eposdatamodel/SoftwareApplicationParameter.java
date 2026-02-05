package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents software package, application and program.
 */
public class SoftwareApplicationParameter extends EPOSDataModelEntity {

    /**
     * This property contains the description of the Software Application
     **/
    @Schema(name="encodingformat", description = "This property contains the encodingformat of the Software Application Parameter", example = "application/json", required = false)
    private String encodingformat;

    /**
     * If the Software Application can be downloaded this property contains the URL to download it.
     **/
    @Schema(name="conformsto", description = "The parameter url to schema", example = "https://urltoschema", required = false)
    private String conformsto;

    /**
     * This property contains the URL at which the application may be installed.
     **/
    @Schema(name="action", description = "This property contains the action at which the application may be installed.", example = "object or result", required = false)
    private String action;

    public String getEncodingformat() {
        return encodingformat;
    }

    public void setEncodingformat(String encodingformat) {
        this.encodingformat = encodingformat;
    }

    public String getConformsto() {
        return conformsto;
    }

    public void setConformsto(String conformsto) {
        this.conformsto = conformsto;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SoftwareApplicationParameter that = (SoftwareApplicationParameter) o;
        return Objects.equals(encodingformat, that.encodingformat) && Objects.equals(conformsto, that.conformsto) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), encodingformat, conformsto, action);
    }

    @Override
    public String toString() {
        return "SoftwareApplicationParameter{" +
                "encodingformat='" + encodingformat + '\'' +
                ", conformsto='" + conformsto + '\'' +
                ", action='" + action + '\'' +
                '}'+ super.toString();
    }
}
