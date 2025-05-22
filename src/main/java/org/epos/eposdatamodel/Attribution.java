package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

/**
 * Physical address of the item.
 */
public class Attribution extends EPOSDataModelEntity {
    /**
     * The agent of the attribution
     **/
    @Schema(name = "agent", description = "The agent of the attribution", example = "ID", required = false)
    private LinkedEntity agent = null;

    /**
     * The role of the attribution. For example, ROLE_X
     **/
    @Schema(name = "role", description = "The role of the attribution. For example, ROLE_X", example = "Role", required = false)
    private List<String> role = null;

    public LinkedEntity getAgent() {
        return agent;
    }

    public void setAgent(LinkedEntity agent) {
        this.agent = agent;
    }

    public List<String> getRole() {
        return role;
    }

    public void setRole(List<String> role) {
        this.role = role;
    }

    public void addRole(String role) {
        if (this.role == null) {
            this.role = new java.util.ArrayList<>();
        }
        this.role.add(role);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Attribution that = (Attribution) o;
        return Objects.equals(agent, that.agent) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), agent, role);
    }

    @Override
    public String toString() {
        return "Attribution{" +
                "agent=" + agent +
                ", role='" + role + '\'' +
                '}';
    }
}
