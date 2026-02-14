package com.codeops.entity;

import com.codeops.entity.enums.SpecType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specifications", indexes = {
        @Index(name = "idx_spec_job_id", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "spec_type", nullable = false)
    private SpecType specType;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;
}
