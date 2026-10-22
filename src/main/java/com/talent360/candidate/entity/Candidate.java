package com.talent360.candidate.entity;

import com.talent360.common.Auditable;
import com.talent360.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String location;

    @Column(length = 255)
    private String linkedin;

    @Column(length = 255)
    private String github;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(length = 255)
    private String education;

    @Column(name = "preferences_json", columnDefinition = "TEXT")
    private String preferencesJson;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateDocument> documents = new ArrayList<>();

    public List<String> getSkillList() {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setSkillList(List<String> skillList) {
        if (skillList == null || skillList.isEmpty()) {
            this.skills = null;
        } else {
            this.skills = String.join(", ", skillList);
        }
    }
}
