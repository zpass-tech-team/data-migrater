package io.mosip.packet.extractor.validator.impl;

import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.TableRequestDto;
import io.mosip.packet.extractor.validator.Validator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderByValidator implements Validator {

    @Override
    public Boolean validate(DBImportRequest dbImportRequest) throws Exception {
        List<TableRequestDto> tableDetails = dbImportRequest.getTableDetails();

        for(TableRequestDto tableRequestDto : tableDetails) {
            if(tableRequestDto.getExecutionOrderSequence() == 1) {
                if(tableRequestDto.getQueryType().equals(QuerySelection.TABLE) && (tableRequestDto.getOrderBy() == null || tableRequestDto.getOrderBy().length <= 0) )
                    throw new Exception(" Order By condition is mandatory for first execution Table");
                else if(tableRequestDto.getQueryType().equals(QuerySelection.SQL_QUERY)) {
                    if(!tableRequestDto.getSqlQuery().toLowerCase().contains("order by"))
                        throw new Exception(" Order By condition is mandatory for first execution Table");
                }
            }
        }
        return true;
    }
}
