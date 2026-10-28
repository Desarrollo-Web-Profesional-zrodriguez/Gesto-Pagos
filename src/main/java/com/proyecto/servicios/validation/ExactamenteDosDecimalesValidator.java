package com.proyecto.servicios.validation;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExactamenteDosDecimalesValidator implements ConstraintValidator<ExactamenteDosDecimales, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull to validate presence
        }
        return value.scale() == 2;
    }
}
