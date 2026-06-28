package com.mingzhe.resumetailor.skill;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body used when updating Skill records.
 */
@Data
public class UpdateSkillDTO {

    private String category;

    @Pattern(regexp = ".*\\S.*", message = "name must not be blank")
    private String name;

}
