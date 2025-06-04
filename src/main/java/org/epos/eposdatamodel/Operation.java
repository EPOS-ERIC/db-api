package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A description of a web service operation.
 */
public class Operation extends EPOSDataModelEntity {

    /**
     * The HTTP method.
     **/
    @Schema(name = "method", description = "The HTTP method.", example = "GET", required = false)
    private String method;

    /**
     * This property is used to specify the output format of the Operation. The possible values are listed here: https://www.iana.org/assignments/media-types/media-types.xhtml
     **/
    @Schema(name = "returns", description = "This property is used to specify the output format of the Operation. The possible values are listed here: https://www.iana.org/assignments/media-types/media-types.xhtml", example = "[\"application/json\"]", required = false)
    private List<String> returns;

    /**
     * The syntax of the template literal is specified by the [RFC6570] URI Template syntax.
     **/
    @Schema(name = "template", description = "The syntax of the template literal is specified by the [RFC6570] URI Template syntax..", example = "http://urltoservice{?param1,param2,param3}", required = false)
    private String template;

    /**
     * A variable-to-property mapping of the IRI template.
     **/
    @Schema(name = "mapping", description = "A variable-to-property mapping of the IRI template.", example = "[{\n" +
            "    \"entityType\": \"MAPPING\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> mapping;

    /**
     * Reverse reference to the related webservice.
     */
    @Schema(name = "maturity", description = "Reverse reference to the related webservice.", example = "{\n" +
            "    \"entityType\": \"WEBSERVICE\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private List<LinkedEntity> webservice;

    private LinkedEntity iriTemplate;

    public LinkedEntity getIriTemplate() {
        return iriTemplate;
    }

    public void setIriTemplate(LinkedEntity iriTemplate) {
        this.iriTemplate = iriTemplate;
    }

    /**
     * A variable-to-property mapping of the payload expected
     **/
    @Schema(name = "mapping", description = "A variable-to-property mapping of the IRI template.", example = "[{\n" +
            "    \"entityType\": \"MAPPING\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> payload;

    public List<LinkedEntity> getPayload() {
        return payload;
    }

    public void setPayload(List<LinkedEntity> payload) {
        this.payload = payload;
    }

    public void addPayload(LinkedEntity payload) {
        if (this.payload == null) {
            ArrayList<LinkedEntity> tmpList = new ArrayList<>();
            tmpList.add(payload);
            this.setPayload(tmpList);
        } else {
            this.getPayload().add(payload);
        }
    }

    public void addMapping(LinkedEntity mapping) {
        if (this.mapping == null) {
            ArrayList<LinkedEntity> tmpList = new ArrayList<>();
            tmpList.add(mapping);
            this.setMapping(tmpList);
        } else {
            this.getMapping().add(mapping);
        }
    }

    public void addReturns(String returns) {
        if (this.getReturns() == null) {
            ArrayList<String> returnsList = new ArrayList<>();
            returnsList.add(returns);
            this.setReturns(returnsList);
        } else {
            this.getReturns().add(returns);
        }
    }


    public Operation method(String method) {
        this.method = method;
        return this;
    }

    /**
     * The HTTP method.
     *
     * @return method
     **/

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Operation returns(List<String> returns) {
        this.returns = returns;
        return this;
    }

    public Operation addReturnsItem(String returnsItem) {
        if (this.returns == null) {
            this.returns = new ArrayList<>();
        }
        this.returns.add(returnsItem);
        return this;
    }

    /**
     * This property is used to specify the output format of the Operation. The possible values are listed here: https://www.iana.org/assignments/media-types/media-types.xhtml
     *
     * @return returns
     **/

    public List<String> getReturns() {
        return returns;
    }

    public void setReturns(List<String> returns) {
        this.returns = returns;
    }

    public Operation template(String template) {
        this.template = template;
        return this;
    }

    /**
     * The syntax of the template literal is specified by the [RFC6570] URI Template syntax.
     *
     * @return template
     **/

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Operation mapping(List<LinkedEntity> mapping) {
        this.mapping = mapping;
        return this;
    }

    public Operation addMappingItem(LinkedEntity mapping) {
        if (this.mapping == null) {
            this.mapping = new ArrayList<>();
        }
        this.mapping.add(mapping);
        return this;
    }


    /**
     * A variable-to-property mapping of the IRI template.
     *
     * @return spatialExtent
     **/
    public List<LinkedEntity> getMapping() {
        return mapping;
    }

    public void setMapping(List<LinkedEntity> mapping) {
        this.mapping = mapping;
    }


    public List<LinkedEntity> getWebservice() {
        return webservice;
    }

    public Operation setWebservice(List<LinkedEntity> webservice) {
        this.webservice = webservice;
        return this;
    }

    public Operation addWebservice(LinkedEntity webservice) {
        if (this.webservice == null) {
            this.webservice = new ArrayList<>();
        }
        this.webservice.add(webservice);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Operation)) return false;
        if (!super.equals(o)) return false;
        Operation operation = (Operation) o;
        return Objects.equals(method, operation.method) && Objects.equals(returns, operation.returns) && Objects.equals(template, operation.template) && Objects.equals(mapping, operation.mapping) && Objects.equals(webservice, operation.webservice) && Objects.equals(payload, operation.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), method, returns, template, mapping, webservice, payload);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "method='" + method + '\'' +
                ", returns=" + returns +
                ", template='" + template + '\'' +
                ", mapping=" + mapping +
                ", webservice=" + webservice +
                ", payload=" + payload +
                '}';
    }
}
