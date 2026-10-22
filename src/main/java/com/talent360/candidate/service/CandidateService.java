package com.talent360.candidate.service;

import com.talent360.candidate.dto.CandidateDocumentResponse;
import com.talent360.candidate.dto.CandidateResponse;
import com.talent360.candidate.dto.CandidateUpdateRequest;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.entity.CandidateDocument;
import com.talent360.candidate.mapper.CandidateMapper;
import com.talent360.candidate.repository.CandidateDocumentRepository;
import com.talent360.candidate.repository.CandidateRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/octet-stream"
    );

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository candidateDocumentRepository;
    private final CandidateMapper candidateMapper;

    @Value("${app.upload.dir:./uploads/cvs}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize upload folder", e);
        }
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Candidate not found with id: " + id));
        return candidateMapper.toResponse(candidate);
    }

    @Transactional
    public CandidateResponse updateCandidate(Long id, CandidateUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Candidate not found with id: " + id));

        candidateMapper.updateEntityFromRequest(request, candidate);
        if (request.getSkills() != null) {
            candidate.setSkillList(request.getSkills());
        }

        Candidate updated = candidateRepository.save(candidate);
        log.info("Updated candidate profile for ID: {}", id);
        return candidateMapper.toResponse(updated);
    }

    @Transactional
    public CandidateDocumentResponse uploadCv(Long candidateId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid or malicious filename detected");
        }
        String cleanFilename = StringUtils.cleanPath(rawFilename);

        int dotIndex = cleanFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("File must have a valid extension (.pdf, .docx, .doc)");
        }
        String extension = cleanFilename.substring(dotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Forbidden file type: " + extension + ". Only PDF and DOCX/DOC documents are permitted.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid MIME type: " + contentType);
        }

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("Candidate not found with id: " + candidateId));

        String storedFileName = "cv_" + candidateId + "_" + UUID.randomUUID() + extension;
        Path targetLocation = this.rootLocation.resolve(storedFileName).normalize();

        if (!targetLocation.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside target upload directory");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file: {}", ex.getMessage());
            throw new RuntimeException("Could not store file securely. Please try again!", ex);
        }

        CandidateDocument document = CandidateDocument.builder()
                .candidate(candidate)
                .fileName(cleanFilename)
                .fileUrl(targetLocation.toString())
                .fileType(contentType != null ? contentType : "application/pdf")
                .build();

        CandidateDocument savedDoc = candidateDocumentRepository.save(document);
        log.info("Uploaded and saved CV document ID {} for candidate {}", savedDoc.getId(), candidateId);

        return candidateMapper.toDocumentResponse(savedDoc);
    }

    @Transactional(readOnly = true)
    public Resource loadCvResource(Long candidateId) {
        CandidateDocument document = candidateDocumentRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("No CV document found for candidate id: " + candidateId));

        try {
            Path filePath = Paths.get(document.getFileUrl()).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new SecurityException("Unauthorized access to file outside allowed directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new EntityNotFoundException("CV file not found or readable on disk");
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Could not read CV file", ex);
        }
    }

    @Transactional(readOnly = true)
    public CandidateDocument getLatestCvDocument(Long candidateId) {
        return candidateDocumentRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("No CV document found for candidate id: " + candidateId));
    }

    @Transactional
    public void deleteCv(Long candidateId) {
        CandidateDocument document = candidateDocumentRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("No CV document found for candidate id: " + candidateId));

        try {
            Path filePath = Paths.get(document.getFileUrl()).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete physical CV file: {}", e.getMessage());
        }

        candidateDocumentRepository.delete(document);
        log.info("Deleted CV document for candidate {}", candidateId);
    }
}