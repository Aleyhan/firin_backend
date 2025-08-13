package com.firinyonetim.backend.validation;

import com.firinyonetim.backend.dto.company_directory.CompanyInfoDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class CompanyInfoValidator implements ConstraintValidator<ValidCompanyInfo, CompanyInfoDto> {

    @Override
    public boolean isValid(CompanyInfoDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // Null nesneleri başka bir validasyon kontrol etmeli (@NotNull gibi)
        }

        String idNumber = dto.getIdentificationNumber();

        if (StringUtils.hasText(idNumber) && idNumber.replaceAll("\\s+", "").length() == 11) {
            // Eğer TCKN (11 haneli) girilmişse, soyadı zorunludur.
            if (!StringUtils.hasText(dto.getPersonSurName())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("TCKN girildiğinde soyad alanı zorunludur.")
                        .addPropertyNode("personSurName")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}