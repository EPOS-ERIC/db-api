package model;

import jakarta.persistence.*;

@Entity
@Table(name = "ontologies")
public class Ontology {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "name", length = 1024)
    private String name;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "type", length = 1024)
    private String type;

    @Lob
    @Column(name = "content")
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}