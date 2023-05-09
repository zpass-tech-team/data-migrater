package io.mosip.packet.core.util;

import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.service.DataRestClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class CommonUtil {

    @Autowired
    private RidGenerator ridGenerator;

    @Autowired
    private DataRestClientService restApiClient;

    private LinkedHashMap<String, Object> latestIdSchemaMap;

    public String generateRegistrationId(String centerId, String machineId) {
        return (String) ridGenerator.generateId(centerId, machineId);
    }

    public String getDocumentAttributeStaticValue(String val) {
        return val.substring(val.indexOf(":")+1).trim();
    }

    public LinkedHashMap<String, Object> getLatestIdSchema() throws ApisResourceAccessException {
        if (latestIdSchemaMap == null) {
            ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
            latestIdSchemaMap = (LinkedHashMap<String, Object> ) response.getResponse();
        }
        return latestIdSchemaMap;
    }
}
