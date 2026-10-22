package com.talent360.ai.jobdescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.ai.entity.AIGeneration;
import com.talent360.ai.jobdescription.dto.JdGenerateRequest;
import com.talent360.ai.jobdescription.dto.JdGeneratedDto;
import com.talent360.ai.repository.AIGenerationRepository;
import com.talent360.common.enums.AIGenerationType;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDescriptionGeneratorService {

    private final ChatClient chatClient;
    private final AIGenerationRepository aiGenerationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public JdGeneratedDto generateJobDescription(JdGenerateRequest request, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail).orElse(null);

        String skillsList = request.getSkills() != null && !request.getSkills().isEmpty()
                ? String.join(", ", request.getSkills())
                : "Standard domain skills";

        String prompt = """
                You are a senior recruitment specialist. Generate an engaging, professional, and comprehensive job description.
                INPUT CRITERIA:
                - Title: %s
                - Department: %s
                - Experience Level: %s
                - Required Skills: %s
                - Location: %s
                - Contract Type: %s

                Provide a structured JSON output with:
                1. title: Polished job title
                2. description: Compelling overview of the role and company impact
                3. responsibilities: 5 to 7 key responsibilities and expected deliverables
                4. requiredSkills: Core technical and behavioral skills
                5. profile: Ideal candidate background and mindset
                6. conditions: Working environment, perks, and contract details
                """.formatted(
                request.getTitle(),
                request.getDepartment(),
                request.getExperienceLevel(),
                skillsList,
                request.getLocation(),
                request.getContractType()
        );

        JdGeneratedDto result;
        try {
            result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(JdGeneratedDto.class);
        } catch (Exception ex) {
            log.error("Spring AI JD generation failed: {}", ex.getMessage());
            result = fallbackGeneration(request);
        }

        if (result == null) {
            result = fallbackGeneration(request);
        }
        try {
            AIGeneration audit = AIGeneration.builder()
                    .type(AIGenerationType.JD_GENERATION)
                    .relatedEntityId(null)
                    .relatedEntityType("JobOffer")
                    .promptUsed(prompt)
                    .generatedContent(objectMapper.writeValueAsString(result))
                    .createdBy(recruiter)
                    .build();
            aiGenerationRepository.save(audit);
            log.info("Saved JD generation audit record for recruiter {}", recruiterEmail);
        } catch (Exception e) {
            log.warn("Could not save JD generation audit record: {}", e.getMessage());
        }

        return result;
    }

    private JdGeneratedDto fallbackGeneration(JdGenerateRequest request) {
        log.warn("Generating fallback job description template");
        return JdGeneratedDto.builder()
                .title(request.getTitle())
                .description("We are looking for a skilled " + request.getTitle() + " to join our " +
                             request.getDepartment() + " team. You will play a key role in building high-impact solutions.")
                .responsibilities(List.of(
                        "Design, build, and maintain efficient, reusable, and reliable solutions",
                        "Collaborate with cross-functional teams to define architecture and requirements",
                        "Ensure the best possible performance, quality, and responsiveness of applications",
                        "Participate in code reviews and mentor junior developers"
                ))
                .requiredSkills(request.getSkills() != null && !request.getSkills().isEmpty()
                        ? request.getSkills()
                        : List.of("Problem solving", "Team collaboration", "Agile methodologies"))
                .profile("Candidate with proven experience as a " + request.getExperienceLevel() +
                         " professional in " + request.getDepartment() + ".")
                .conditions("Contract: " + request.getContractType() + " | Location: " + request.getLocation())
                .build();
    }
}
