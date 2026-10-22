package com.talent360.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.common.dto.PageResponse;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.job.dto.JobOfferRequest;
import com.talent360.job.dto.JobOfferResponse;
import com.talent360.job.service.JobOfferService;
import com.talent360.security.JwtAuthenticationFilter;
import com.talent360.security.OwnershipSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobOfferController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobOfferService jobOfferService;

    @MockBean
    private OwnershipSecurityService ownershipSecurity;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Public GET /api/jobs returns paginated job offers")
    void searchJobsPublic() throws Exception {
        JobOfferResponse job = JobOfferResponse.builder()
                .id(1L)
                .title("Senior Java Engineer")
                .status(JobStatus.OPEN)
                .build();

        PageResponse<JobOfferResponse> pageResponse = PageResponse.<JobOfferResponse>builder()
                .content(List.of(job))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(jobOfferService.searchJobs(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Senior Java Engineer"));
    }

    @Test
    @DisplayName("PUT /api/jobs/{id} succeeds for owner recruiter")
    @WithMockUser(username = "recruiter@talent.com", roles = {"RECRUITER"})
    void updateJobSuccess() throws Exception {
        JobOfferRequest request = JobOfferRequest.builder()
                .title("Updated Senior Java Engineer")
                .description("Updated job description in detail.")
                .departmentId(1L)
                .location("Paris")
                .contractType(ContractType.CDI)
                .workMode(WorkMode.HYBRID)
                .experienceLevel(ExperienceLevel.SENIOR)
                .requiredSkills(List.of("Java", "Spring Boot"))
                .build();

        JobOfferResponse response = JobOfferResponse.builder()
                .id(1L)
                .title("Updated Senior Java Engineer")
                .recruiterEmail("recruiter@talent.com")
                .build();

        when(jobOfferService.updateJob(eq(1L), any(JobOfferRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Senior Java Engineer"));
    }
}
