package com.talent360.department.service;

import com.talent360.department.dto.DepartmentRequest;
import com.talent360.department.dto.DepartmentResponse;
import com.talent360.department.entity.Department;
import com.talent360.department.mapper.DepartmentMapper;
import com.talent360.department.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
        return departmentMapper.toResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Department with name '" + request.getName() + "' already exists");
        }
        Department department = departmentMapper.toEntity(request);
        Department saved = departmentRepository.save(department);
        log.info("Created department with ID: {}", saved.getId());
        return departmentMapper.toResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        departmentRepository.findByNameIgnoreCase(request.getName())
                .filter(d -> !d.getId().equals(id))
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Department with name '" + request.getName() + "' already exists");
                });

        departmentMapper.updateEntityFromRequest(request, department);
        Department updated = departmentRepository.save(department);
        log.info("Updated department with ID: {}", updated.getId());
        return departmentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
        if (!department.getJobOffers().isEmpty()) {
            throw new IllegalStateException("Cannot delete department with active job offers. Reassign offers first.");
        }
        departmentRepository.delete(department);
        log.info("Deleted department with ID: {}", id);
    }
}
