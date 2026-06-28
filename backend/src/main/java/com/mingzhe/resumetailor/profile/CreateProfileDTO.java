package com.mingzhe.resumetailor.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// DTO for profile creation
// userId and fullName required

/**
 * Request body used when creating Profile records.
 */
@Data
public class CreateProfileDTO {

    @NotNull(message = "userId is required for profile")
    private Long userId;

    @NotBlank(message = "fullName is required for profile")
    private String fullName;

    private String phone;

    @Email(message = "contactEmail must be valid")
    private String contactEmail;

    private String linkedinUrl;

    private String githubUrl;

    private String location;

    private String summary;

}
