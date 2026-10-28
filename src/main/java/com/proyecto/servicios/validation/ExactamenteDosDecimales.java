package com.proyecto.servicios.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExactamenteDosDecimalesValidator.class)
@Documented
public @interface ExactamenteDosDecimales {

    String message() default "El monto debe tener exactamente dos decimales (ejemplo: 100.00, no se acepta 100)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
