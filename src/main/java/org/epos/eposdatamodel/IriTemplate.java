package org.epos.eposdatamodel;

import java.util.ArrayList;
import java.util.List;

public class IriTemplate extends EPOSDataModelEntity{
    private String template;
    private List<LinkedEntity> mappings = new ArrayList<>();

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<LinkedEntity> getMappings() {
        return mappings;
    }

    public void setMappings(List<LinkedEntity> mappings) {
        this.mappings = mappings;
    }

    public void addMapping(LinkedEntity mapping) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        this.mappings.add(mapping);
    }

    @Override
    public String toString() {
        return "IriTemplate{" +
                "template='" + template + '\'' +
                ", mappings=" + mappings +
                '}'+ super.toString();
    }
}

