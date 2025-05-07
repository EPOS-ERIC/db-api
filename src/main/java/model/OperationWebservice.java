package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "operation_webservice")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class OperationWebservice {
    @EmbeddedId
    private OperationWebserviceId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private model.Webservice webserviceInstance;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    public OperationWebserviceId getId() {
        return id;
    }

    public void setId(OperationWebserviceId id) {
        this.id = id;
    }

    public model.Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(model.Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

}