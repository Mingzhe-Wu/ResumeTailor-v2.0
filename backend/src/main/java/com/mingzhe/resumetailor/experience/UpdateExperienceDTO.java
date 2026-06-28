package com.mingzhe.resumetailor.experience;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body used when updating Experience records.
 */
@Data
public class UpdateExperienceDTO {

    @Pattern(regexp = ".*\\S.*", message = "companyName must not be blank")
    private String companyName;

    @Pattern(regexp = ".*\\S.*", message = "position must not be blank")
    private String position;

    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

}
