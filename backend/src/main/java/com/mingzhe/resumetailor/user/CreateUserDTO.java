package com.mingzhe.resumetailor.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body used when creating User records.
 */
@Data
public class CreateUserDTO {

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    private String fullName;

}
