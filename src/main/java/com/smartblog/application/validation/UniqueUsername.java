package com.smartblog.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;



    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = UniqueUsernameValidator.class)
    @Documented
    public @interface UniqueUsername {
        String message() default "Username already exists";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }


