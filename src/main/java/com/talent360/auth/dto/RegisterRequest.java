package com.talent360.auth.dto;

import com.talent360.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_\\-#^~`+=\\[\\]{}|;:,.<>])[A-Za-z\\d@$!%*?&_\\-#^~`+=\\[\\]{}|;:,.<>]{8,100}$",
            message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;

    private String confirmPassword;

    private Role role;

    private String firstName;
    private String lastName;
}
