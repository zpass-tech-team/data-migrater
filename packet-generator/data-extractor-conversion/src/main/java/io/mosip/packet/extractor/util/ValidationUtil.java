package io.mosip.packet.extractor.util;

import io.mosip.packet.core.constant.ValidatorEnum;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.extractor.validator.Validator;
import io.mosip.packet.extractor.validator.impl.FilterValidation;
import io.mosip.packet.extractor.validator.impl.IdSchemaFieldValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
@Import({IdSchemaFieldValidator.class, FilterValidation.class})
public class ValidationUtil {

    @Autowired
    private IdSchemaFieldValidator idSchemaFieldValidator;

    @Autowired
    private FilterValidation filterValidation;

    private LinkedHashMap<ValidatorEnum, Validator> validatorList = null;

    public LinkedHashMap<ValidatorEnum, Validator> getValidatorMap() {
        if(validatorList == null) {
            validatorList= new LinkedHashMap<>();
            validatorList.put(ValidatorEnum.ID_SCHEMA_VALIDATOR, idSchemaFieldValidator);
            validatorList.put(ValidatorEnum.FILTER_VALIDATOR, filterValidation);
        }
        return validatorList;
    }


    public void validateRequest(DBImportRequest dbImportRequest, List<ValidatorEnum> validationList) throws Exception {
        for (ValidatorEnum validatorEnum : validationList)
            getValidatorMap().get(validatorEnum).validate(dbImportRequest);
    }
}
