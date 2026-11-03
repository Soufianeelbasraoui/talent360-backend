package com.talent360.candidate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateUpdateRequest {

    private String firstName;
    private String lastName;
    private String phone;
    private String location;
    private String linkedin;
    private String github;
    private String bio;
    private List<String> skills;
    private Integer experienceYears;
    private String education;
    private String preferencesJson;
}
