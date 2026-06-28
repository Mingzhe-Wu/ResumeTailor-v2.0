package com.mingzhe.resumetailor.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body used when updating User records.
 */
@Data
public class UpdateUserDTO {

    @Email(message = "email must be valid")
    @Pattern(regexp = ".*\\S.*", message = "email must not be blank")
    private String email;

    @Pattern(regexp = ".*\\S.*", message = "password must not be blank")
    private String password;

}
