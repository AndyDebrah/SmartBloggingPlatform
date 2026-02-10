package com.smartblog.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;


/**
 * Custom validation annotation to ensure email uniqueness.
 */

@Target({ElementType.FIELD , ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented

public @interface UniqueEmail {
    String message() default "Email is already in use.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
