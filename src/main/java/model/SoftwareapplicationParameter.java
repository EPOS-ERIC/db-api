package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_parameters", schema = "metadata_catalogue")
public class SoftwareapplicationParameter {
    @EmbeddedId
    private SoftwareapplicationParameterId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("parameterInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parameter_instance_id", nullable = false)
    private Parameter parameterInstance;

    public SoftwareapplicationParameterId getId() {
        return id;
    }

    public void setId(SoftwareapplicationParameterId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Parameter getParameterInstance() {
        return parameterInstance;
    }

    public void setParameterInstance(Parameter parameterInstance) {
        this.parameterInstance = parameterInstance;
    }

}