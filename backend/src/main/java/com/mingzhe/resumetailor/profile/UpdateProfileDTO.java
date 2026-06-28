package com.mingzhe.resumetailor.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// DTO for profile update

/**
 * Request body used when updating Profile records.
 */
@Data
public class UpdateProfileDTO {

    @Pattern(regexp = ".*\\S.*", message = "fullName must not be blank")
    private String fullName;

    private String phone;

    @Email(message = "contactEmail must be valid")
    private String contactEmail;

    private String linkedinUrl;

    private String githubUrl;

    private String location;

    private String summary;

}
