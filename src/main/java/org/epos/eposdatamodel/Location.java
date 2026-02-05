package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * A spatial region specified by using geographic coordinates.
 */
public class Location extends EPOSDataModelEntity{

    /**
     * This property associates any resource with the corresponding geometry.
     **/
    @Schema(name = "location", description = "This property associates any resource with the corresponding geometry.", example = "POLYGON(....)", required = false)
    private String location;


    public Location location(String location) {
        this.location = location;
        return this;
    }

    /**
     * Get location
     *
     * @return location
     **/

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Location{" +
                "location='" + location + '\'' +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location1 = (Location) o;
        return Objects.equals(getLocation(), location1.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation());
    }
}
