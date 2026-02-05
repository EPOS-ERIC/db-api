package org.epos.eposdatamodel;

import java.util.Objects;

/**
 * This class is extended by all the EPOS Data Model beans.
 * It consists exclusively of the uid field which is used as business identitifier common for all EPOS Data Model.
 */
public abstract class EPOSDataModelEntity extends VersioningAndApproval {

    /**
     * This property contains the main identifier for the entity, e.g. the URI or
     * other unique identifier in the context of the Catalogue.
     */
    private String uid;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "EPOSDataModelEntity{" +
                "uid='" + uid + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EPOSDataModelEntity that = (EPOSDataModelEntity) o;
        return Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getUid());
    }
}
