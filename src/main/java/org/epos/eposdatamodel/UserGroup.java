package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;
import model.RoleType;

import java.util.Objects;

public class UserGroup {

    @Schema(name="role", description = "Role of the User in the group", example = "EDITOR", required = false)
    private RoleType role;

    @Schema(name="groupId", description = "Id of the group", example = "342442", required = false)
    private String groupId;

    public UserGroup(RoleType role, String groupId) {
        this.role = role;
        this.groupId = groupId;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGroup userGroup = (UserGroup) o;
        return role == userGroup.role && Objects.equals(groupId, userGroup.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, groupId);
    }

    @Override
    public String toString() {
        return "UserGroup{" +
                "role=" + role +
                ", groupId=" + groupId +
                '}';
    }
}
