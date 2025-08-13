package com.firinyonetim.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CompanyInfoValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCompanyInfo {
    String message() default "Geçersiz firma bilgileri";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}