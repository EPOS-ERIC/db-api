package model;

import jakarta.persistence.*;

@Entity
@Table(name = "operation_element", schema = "metadata_catalogue")
public class OperationElement {
    @EmbeddedId
    private OperationElementId id;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public OperationElementId getId() {
        return id;
    }

    public void setId(OperationElementId id) {
        this.id = id;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}