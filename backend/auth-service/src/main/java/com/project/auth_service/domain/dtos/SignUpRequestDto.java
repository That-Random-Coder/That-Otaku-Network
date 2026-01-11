package com.project.auth_service.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequestDto {

    @NotBlank(message = "Username must not be blank")
    @Length(min = 6, max = 50, message = "Username length must be between 6 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, digits, and underscores"
    )
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email must not be blank")
    @Length(max = 100, message = "Email length must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Length(min = 6, max = 60, message = "Password length must be between 6 and 60 characters")
    private String password;
}

