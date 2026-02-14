package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_directives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDirective {

    @EmbeddedId
    private ProjectDirectiveId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("directiveId")
    @JoinColumn(name = "directive_id")
    private Directive directive;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean enabled = true;
}
