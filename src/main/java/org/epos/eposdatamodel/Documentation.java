package org.epos.eposdatamodel;


import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * Documentation.
 */
public class Documentation extends EPOSDataModelEntity{
    @Schema(name = "title", description = "Documentation title", example = "Documentation title", required = false)
    private String title = null;

    @Schema(name = "description", description = "Documentation description", example = "Documentation description text", required = false)
    private String description = null;

    @Schema(name = "uri", description = "Documentation url", example = "URL", required = false)
    private String uri = null;

    public Documentation title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Get title
     *
     * @return title
     **/

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Documentation description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Get description
     *
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Documentation uri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Get uri
     *
     * @return uri
     **/

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Documentation documentation = (Documentation) o;
        return Objects.equals(this.title, documentation.title) &&
                Objects.equals(this.description, documentation.description) &&
                Objects.equals(this.uri, documentation.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, uri);
    }

    @Override
    public String toString() {
        return "Documentation{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", uri='" + uri + '\'' +
                '}'+ super.toString();
    }
}
