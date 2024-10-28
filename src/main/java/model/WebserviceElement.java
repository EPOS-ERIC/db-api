package model;

import jakarta.persistence.*;

@Entity
@Table(name = "webservice_element")
public class WebserviceElement {
    @EmbeddedId
    private WebserviceElementId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public WebserviceElementId getId() {
        return id;
    }

    public void setId(WebserviceElementId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}