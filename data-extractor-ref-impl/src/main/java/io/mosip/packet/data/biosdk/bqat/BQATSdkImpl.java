package io.mosip.packet.data.biosdk.bqat;

import com.google.gson.Gson;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.biosdk.BioSDKRequest;
import io.mosip.packet.core.dto.biosdk.BioSDKRequestWrapper;
import io.mosip.packet.core.dto.biosdk.QualityCheckRequest;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.core.spi.BioSdkApiFactory;
import io.mosip.packet.data.biosdk.bqat.constant.BQATFileType;
import io.mosip.packet.data.biosdk.bqat.constant.BQATModalityType;
import io.mosip.packet.data.biosdk.bqat.dto.BQATRequest;
import io.mosip.packet.data.biosdk.bqat.dto.BQATResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class BQATSdkImpl implements BioSdkApiFactory {

    @Autowired
    private DataRestClientService restApiClient;

    @Value("${mosip.biometric.sdk.provider.write.sdk.response:true}")
    private Boolean writeResponse;

    @Override
    public Double calculateBioQuality(BioSDKRequestWrapper bioSDKRequestWrapper) throws Exception {
        BQATRequest request = new BQATRequest();

        try {
            request.setModality(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).getModality());
            request.setType(BQATFileType.valueOf(bioSDKRequestWrapper.getFormat()).getType());
            request.setData(Base64.getEncoder().encodeToString(((BIR)bioSDKRequestWrapper.getSegments().get(0)).getBdb()));
            request.setId(UUID.randomUUID().toString());
            request.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
            BQATResponse response= (BQATResponse) restApiClient.postApi(ApiName.BQAT_BIOSDK_QUALITY_CHECK, null, "", request, BQATResponse.class);
            LinkedHashMap<String, Object> bioSDKResponse = (LinkedHashMap<String, Object>) response.getResults();

            if(bioSDKResponse != null) {
                if(writeResponse) {
                    HashMap<String, String> csvMap = (HashMap<String, String>) bioSDKRequestWrapper.getInputObject();
                    csvMap.put(bioSDKRequestWrapper.getBiometricField(),  (new Gson()).toJson(bioSDKResponse));
                }

                if(bioSDKRequestWrapper.getIsOnlyForQualityCheck()) {
                    if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.FACE))
                        return Double.valueOf(bioSDKResponse.get("quality").toString()) * 10;
                    else if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.IRIS))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else
                        return Double.valueOf(bioSDKResponse.get("NFIQ2").toString());
                } else {
                    if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.FACE))
                        return Double.valueOf(bioSDKResponse.get("quality").toString()) * 10;
                    else if(BQATModalityType.valueOf(bioSDKRequestWrapper.getBiometricType()).equals(BQATModalityType.IRIS))
                        return Double.valueOf(bioSDKResponse.get("quality").toString());
                    else
                        return Double.valueOf(bioSDKResponse.get("NFIQ2").toString());
                }
            } else {
                throw new Exception("Error While Calling BIOSDK for Quality Check for Modality ");
            }
        } catch (Exception e) {
            throw new Exception("BQAT Tool Error Request " + (new Gson()).toJson(request));
        }
    }
}
