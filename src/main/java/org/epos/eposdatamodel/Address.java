package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * Physical address of the item.
 */
public class Address extends EPOSDataModelEntity {
    /**
     * The country.
     **/
    @Schema(name = "country", description = "The country of the address", example = "IT or Italy", required = false)
    private String country = null;

    /**
     * The locality in which the street address is, and which is in the region. For example, Mountain View.
     **/
    @Schema(name = "locality", description = "The locality in which the street address is, and which is in the region. For example, Mountain View", example = "Mountain View", required = false)
    private String locality = null;

    /**
     * The postal code. For example, 94043.
     **/
    @Schema(name = "postalCode", description = "The postal code. For example, 94043.", example = "94043", required = false)
    private String postalCode = null;

    /**
     * The street address. For example, 1600 Amphitheatre Pkwy.
     **/
    @Schema(name = "street", description = "The street address. For example, 1600 Amphitheatre Pkwy.", example = "1600 Amphitheatre Pkwy", required = false)
    private String street = null;

    /**
     * The two-letter ISO 3166-1 alpha-2 country code (e.g., IT, UK).
     */
    @Schema(name = "countryCode", description = "The two-letter ISO 3166-1 alpha-2 country code (e.g., IT, UK).", example = "IT", required = false)
    private String countryCode = null;


    public Address country(String country) {
        this.country = country;
        return this;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Address locality(String locality) {
        this.locality = locality;
        return this;
    }


    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public Address postalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }


    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Address street(String street) {
        this.street = street;
        return this;
    }


    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public Address countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }


    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public String toString() {
        return "Address{" +
                "country='" + country + '\'' +
                ", locality='" + locality + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", street='" + street + '\'' +
                ", countryCode='" + countryCode + '\'' +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(getCountry(), address.getCountry()) && Objects.equals(getLocality(), address.getLocality()) && Objects.equals(getPostalCode(), address.getPostalCode()) && Objects.equals(getStreet(), address.getStreet()) && Objects.equals(getCountryCode(), address.getCountryCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCountry(), getLocality(), getPostalCode(), getStreet(), getCountryCode());
    }
}
