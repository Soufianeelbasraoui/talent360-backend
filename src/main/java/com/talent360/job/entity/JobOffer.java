package com.talent360.job.entity;

import com.talent360.common.Auditable;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.department.entity.Department;
import com.talent360.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOffer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 30)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 30)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 30)
    private ExperienceLevel experienceLevel;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobOfferVersion> versions = new ArrayList<>();

    public List<String> getRequiredSkillList() {
        if (requiredSkills == null || requiredSkills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(requiredSkills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setRequiredSkillList(List<String> skillList) {
        if (skillList == null || skillList.isEmpty()) {
            this.requiredSkills = null;
        } else {
            this.requiredSkills = String.join(", ", skillList);
        }
    }
}
