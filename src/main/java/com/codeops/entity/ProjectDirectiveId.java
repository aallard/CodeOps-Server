package com.codeops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDirectiveId implements Serializable {

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "directive_id")
    private UUID directiveId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectDirectiveId that = (ProjectDirectiveId) o;
        return Objects.equals(projectId, that.projectId) &&
                Objects.equals(directiveId, that.directiveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, directiveId);
    }
}
