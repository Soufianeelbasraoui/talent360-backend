package com.talent360.application.dto;

import com.talent360.common.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdateRequest {

    @NotNull(message = "Status is required (NEW, SCREENING, OFFER, HIRED, REJECTED)")
    private ApplicationStatus status;
}
