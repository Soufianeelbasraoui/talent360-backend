package com.talent360.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.talent360.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInMs;
    private Long userId;
    private String email;
    private Role role;
}
