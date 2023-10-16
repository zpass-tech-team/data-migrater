package io.mosip.packet.data.biosdk.bqat;

import com.google.gson.Gson;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.dto.biosdk.BioSDKRequestWrapper;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.core.spi.BioSdkApiFactory;
import io.mosip.packet.data.biosdk.bqat.constant.BQATFileType;
import io.mosip.packet.data.biosdk.bqat.constant.BQATModalityType;
import io.mosip.packet.data.biosdk.bqat.dto.BQATRequest;
import io.mosip.packet.data.biosdk.bqat.dto.BQATResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.mosip.packet.core.constant.GlobalConfig.IS_ONLY_FOR_QUALITY_CHECK;
import static io.mosip.packet.core.constant.GlobalConfig.WRITE_BIOSDK_RESPONSE;

@Component
public class BQATSdkImpl implements BioSdkApiFactory {

    @Autowired
    private DataRestClientService restApiClient;

    private static final Logger LOGGER = DataProcessLogger.getLogger(BQATSdkImpl.class);


    @Override
    public Double calculateBioQuality(BioSDKRequestWrapper bioSDKRequestWrapper) throws Exception {
        BQATRequest request = new BQATRequest();
        Long startTime = System.nanoTime();
        request.setModality(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).getModality());
        request.setType(BQATFileType.valueOf(bioSDKRequestWrapper.getFormat()).getType());
        request.setData(Base64.getEncoder().encodeToString(((BIR)bioSDKRequestWrapper.getSegments().get(0)).getBdb()));
        request.setId(UUID.randomUUID().toString());
        request.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        BQATResponse response= (BQATResponse) restApiClient.postApi(ApiName.BQAT_BIOSDK_QUALITY_CHECK, null, "", request, BQATResponse.class);
        LinkedHashMap<String, Object> bioSDKResponse = (LinkedHashMap<String, Object>) response.getResults();
        Long time = System.nanoTime() - startTime;
        System.out.println("Time Taken to call BIOSDK is" + TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS));
        if(bioSDKResponse != null) {
            try {
                if(bioSDKRequestWrapper.getIsOnlyForQualityCheck()) {
                    if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.FACE))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.IRIS))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else
                        return Double.valueOf(bioSDKResponse.get("NFIQ2").toString());
                } else {
                    if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.FACE))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.IRIS))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else
                        return Double.valueOf(bioSDKResponse.get("NFIQ2").toString());
                }
            } catch (Exception e) {
                if(!IS_ONLY_FOR_QUALITY_CHECK)
                    throw e;
                else
                    return Double.valueOf(0);
            } finally {
                if(WRITE_BIOSDK_RESPONSE) {
                    HashMap<String, String> csvMap = (HashMap<String, String>) bioSDKRequestWrapper.getInputObject();
                    csvMap.put(bioSDKRequestWrapper.getBiometricField(),  (new Gson()).toJson(bioSDKResponse));
                }
            }
        } else {
            LOGGER.error("Error While Calling BIOSDK for Quality Check for Modality " + (new Gson()).toJson(response));
            if(!IS_ONLY_FOR_QUALITY_CHECK)
                throw new Exception("Error While Calling BIOSDK for Quality Check for Modality ");
            else
                return Double.valueOf(0);
        }
    }
}
