package io.mosip.packet.data.biosdk.tech5;

import com.google.gson.Gson;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.biosdk.*;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.core.spi.BioSdkApiFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class BioSdkImpl implements BioSdkApiFactory {

    @Autowired
    private DataRestClientService restApiClient;

    @Value("${mosip.biometric.sdk.provider.write.sdk.response:true}")
    private Boolean writeResponse;

    private static final Logger LOGGER = DataProcessLogger.getLogger(BioSdkImpl.class);

    @Override
    public Double calculateBioQuality(BioSDKRequestWrapper bioSDKRequestWrapper) throws Exception {
        SegmentDto segment = new SegmentDto();
        segment.setSegments(new ArrayList<>());
        segment.getSegments().add(bioSDKRequestWrapper.getSegments().get(0));
        segment.setOthers(new OtherDto());
        QualityCheckRequest request = new QualityCheckRequest();
        request.setSample(segment);
        request.setModalitiesToCheck(new ArrayList<>());
        request.getModalitiesToCheck().add(bioSDKRequestWrapper.getBiometricType());

        String requestText = (new Gson()).toJson(request);
        String encodedRequest = Base64.getEncoder().encodeToString(requestText.getBytes(StandardCharsets.UTF_8));
        BioSDKRequest bioSDKRequest = new BioSDKRequest();
        bioSDKRequest.setVersion("1.0");
        bioSDKRequest.setRequest(encodedRequest);

        LOGGER.info("Request Send Time for SDK " + bioSDKRequestWrapper.getBiometricField() + " : " + new Date());
        ResponseWrapper response= (ResponseWrapper) restApiClient.postApi(ApiName.BIOSDK_QUALITY_CHECK, null, "", bioSDKRequest, ResponseWrapper.class);
        LOGGER.info("Response Received Time for SDK " + bioSDKRequestWrapper.getBiometricField() + " : " + new Date());
        if(writeResponse) {
            HashMap<String, String> csvMap = (HashMap<String, String>) bioSDKRequestWrapper.getInputObject();
            csvMap.put(bioSDKRequestWrapper.getBiometricField(),  (new Gson()).toJson(response));
        }
        LinkedHashMap<String, Object> bioSDKResponse = (LinkedHashMap<String, Object>) response.getResponse();
        if(bioSDKResponse.get("statusCode").equals(200)) {
            LinkedHashMap<String, Object> resp = (LinkedHashMap<String, Object>) bioSDKResponse.get("response");
            LinkedHashMap<String, Object> scoreMap = (LinkedHashMap<String, Object>) resp.get("scores");
            LinkedHashMap<String, Object> modalityMap = (LinkedHashMap<String, Object>) scoreMap.get(bioSDKRequestWrapper.getBiometricType());
            return (Double) modalityMap.get("score");
        } else {
            throw new Exception("Error While Calling BIOSDK for Quality Check for Modality ");
        }
    }
}
