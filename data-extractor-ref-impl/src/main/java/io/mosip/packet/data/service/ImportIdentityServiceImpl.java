package io.mosip.packet.data.service;

import com.google.gson.Gson;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.LoggerFileConstant;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.data.dto.IdRequestDto;

import io.mosip.packet.data.dto.LocationDto;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ImportIdentityServiceImpl implements ImportIdentityService {

    private static final String PROVINCE = "province";
    private static final String DISTRICT = "district";
    private static final String CONSTITUENCY = "constituency";
    private static final String SUBFOLDER = "subfolder";
    private static final String LANG_CODE = "eng";

    private final Logger logger = DataProcessLogger.getLogger(ImportIdentityServiceImpl.class);

    @Autowired
    private DataRestClientService dataRestClientService;

    @Override
    public ResponseWrapper importIdentity(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        logger.info("Writer Request: {}", idRequestDto);
        // Fetches location detail
        getLocationDetails(idRequestDto);
        // Generates UIN
        generateUIN(idRequestDto);
        // Add Identity call
        return addIdentity(idRequestDto);
    }

    private void getLocationDetails(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        try {
            // Location details fetch
            String locationCode = (String) ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).get(SUBFOLDER);
            List<String> pathSegments = Arrays.asList(locationCode, LANG_CODE);
            ResponseWrapper response = (ResponseWrapper) dataRestClientService.getApi(ApiName.MASTER_LOCATION_GET, pathSegments, "", "", ResponseWrapper.class);
            if (response != null && response.getResponse() != null) {
                List<LocationDto> locationDtos = (List<LocationDto>) response.getResponse();
                locationDtos.stream().forEach(locationDto -> {
                    if (locationDto.getHierarchyName().equalsIgnoreCase(PROVINCE)) {
                        ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).put(PROVINCE, locationDto.getName());
                    } if(locationDto.getHierarchyName().equalsIgnoreCase(DISTRICT)) {
                        ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).put(DISTRICT, locationDto.getName());
                    } if(locationDto.getHierarchyName().equalsIgnoreCase(CONSTITUENCY)) {
                        ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).put(CONSTITUENCY, locationDto.getName());
                    }
                });
                logger.info("Location fetch success.");
            }
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private ResponseWrapper addIdentity(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        //logger.info("Add Identity Request: {}", (new Gson()).toJson(idRequestDto));
        try {

            ResponseWrapper response = (ResponseWrapper) dataRestClientService.postApi(ApiName.ADD_IDENTITY,
                    null, "", "", idRequestDto, ResponseWrapper.class);
            logger.info("Add Identity call completed.");
            return response;
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private void generateUIN(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        try {
            // UIN generation
            ResponseWrapper response = (ResponseWrapper) dataRestClientService.getApi(ApiName.GET_UIN, null, "", "", ResponseWrapper.class);
            if (response != null && response.getResponse() != null) {
                Map<String, String> responseMap = (Map<String, String>) response.getResponse();
                ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).put("UIN", responseMap.get("uin"));
                logger.info("UIN Generation success.");
            }
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

}
