package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A description of a web service operation.
 */
public class Payload extends EPOSDataModelEntity {

    /**
     * A variable-to-property mapping of the IRI template.
     **/
    @Schema(name = "mapping", description = "A variable-to-property mapping of the IRI template.", example = "[{\n" +
            "    \"entityType\": \"OUTPUTMAPPING\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> outputMapping;

    /**
     * Reverse reference to the related webservice.
     */
    @Schema(name = "maturity", description = "Reverse reference to the related operation.", example = "{\n" +
            "    \"entityType\": \"OPERATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity supportedOperation;

    public List<LinkedEntity> getOutputMapping() {
        return outputMapping;
    }

    public void setOutputMapping(List<LinkedEntity> outputMapping) {
        this.outputMapping = outputMapping;
    }

    public void addOutputMapping(LinkedEntity outputMapping) {
        if (this.outputMapping == null) {
            ArrayList<LinkedEntity> tmpList = new ArrayList<>();
            tmpList.add(outputMapping);
            this.setOutputMapping(tmpList);
        } else {
            this.getOutputMapping().add(outputMapping);
        }
    }

    public LinkedEntity getSupportedOperation() {
        return supportedOperation;
    }

    public void setSupportedOperation(LinkedEntity supportedOperation) {
        this.supportedOperation = supportedOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Payload)) return false;
        if (!super.equals(o)) return false;
        Payload payload = (Payload) o;
        return Objects.equals(outputMapping, payload.outputMapping) && Objects.equals(supportedOperation, payload.supportedOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), outputMapping, supportedOperation);
    }

    @Override
    public String toString() {
        return "Payload{" +
                "outputMapping=" + outputMapping +
                ", supportedOperation=" + supportedOperation +
                '}';
    }
}
