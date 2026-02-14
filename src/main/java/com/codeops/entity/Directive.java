package com.codeops.entity;

import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.DirectiveScope;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "directives", indexes = {
        @Index(name = "idx_directive_team_id", columnList = "team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Directive extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private DirectiveCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private DirectiveScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = true)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Builder.Default
    @Column(name = "version", columnDefinition = "integer default 1")
    private Integer version = 1;
}
