package io.mosip.data.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.data.constant.ApiName;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.packet.type.IndividualBiometricType;
import io.mosip.data.dto.packet.type.SimpleType;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.service.DataRestClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PacketCreator {

    @Autowired
    private DataRestClientService restApiClient;

    @Autowired
    private Environment env;

    @Value("${mosip.id.schema.version:0.1}")
    private Float version;

    @Value("${mosip.primary.language}")
    private String primaryLamguage;

    private LinkedHashMap<String, Object> latestIdSchemaMap;

    private ObjectMapper mapper = new ObjectMapper();

    public LinkedHashMap<String, Object> getLatestIdSchema() throws ApisResourceAccessException {
        if (latestIdSchemaMap == null) {
            ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
            latestIdSchemaMap = (LinkedHashMap<String, Object> ) response.getResponse();
        }
        return latestIdSchemaMap;
    }

    public LinkedHashMap<String, Object> setDemographic(LinkedHashMap<String, Object> demoDetails, Boolean isBiometricPresent, List ignorableFields) throws Exception {
        LinkedHashMap<String, Object> demoMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();
            Boolean required = (Boolean) map.get("required");

            if (id.equals("IDSchemaVersion")) {
                demoMap.put(id, version);
            } else if (type.equals("biometricsType")) {
                if (id.equals("individualBiometrics")) {
                    if (isBiometricPresent) {
                        IndividualBiometricType indiBiotype = new IndividualBiometricType();
                        indiBiotype.setFormat("cbeff");
                        indiBiotype.setVersion(1.0);
                        indiBiotype.setValue("individualBiometrics_bio_CBEFF");
                        demoMap.put("individualBiometrics", mapper.writeValueAsString(indiBiotype));
                    }
                }
            } else if (type.equals("documentType")) {

            } else if (demoDetails.containsKey(id.toLowerCase()) && demoDetails.get(id.toLowerCase()) != null) {
                switch (type) {
                    case "simpleType":
                        List<SimpleType> valList = new ArrayList<>();
                        SimpleType simpleType = new SimpleType(primaryLamguage, demoDetails.get(id.toLowerCase()) == null ? "":demoDetails.get(id.toLowerCase()).toString());
                        valList.add(simpleType);
                        demoMap.put(id, mapper.writeValueAsString(valList));
                        break;

                    case "number":

                    case "string" :
                        demoMap.put(id, demoDetails.get(id.toLowerCase()) == null ? "" : demoDetails.get(id.toLowerCase()));
                        break;
                }
            } else if (required && !ignorableFields.contains(id)) {
                throw new Exception("Mandatory Field '" + id + "' value missing");
            }
        }
        return demoMap;
    }

    public LinkedHashMap<String, String> setBiometrics() throws ApisResourceAccessException {
        LinkedHashMap<String, Object> demoMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();

/*        LOGGER.debug("Adding Biometrics to packet manager started..");
        Map<String, List<BIR>> capturedBiometrics = new HashMap<>();
        Map<String, Map<String, Object>> capturedMetaInfo = new LinkedHashMap<>();
        Map<String, Map<String, Object>> exceptionMetaInfo = new LinkedHashMap<>();

        for(String key : registrationDTO.getBiometrics().keySet()) {
            String fieldId = key.split("_")[0];
            String bioAttribute = key.split("_")[1];
            BIR bir = birBuilder.buildBIR(registrationDTO.getBiometrics().get(key));
            if (!capturedBiometrics.containsKey(fieldId)) {
                capturedBiometrics.put(fieldId, new ArrayList<>());
            }
            capturedBiometrics.get(fieldId).add(bir);
            if (!capturedMetaInfo.containsKey(fieldId)) {
                capturedMetaInfo.put(fieldId, new HashMap<>());
            }
            capturedMetaInfo.get(fieldId).put(bioAttribute, new BiometricsMetaInfoDto(
                    registrationDTO.getBiometrics().get(key).getNumOfRetries(),
                    registrationDTO.getBiometrics().get(key).isForceCaptured(),
                    bir.getBdbInfo().getIndex()));
        }

        for(String key : registrationDTO.getBiometricExceptions().keySet()) {
            String fieldId = key.split("_")[0];
            String bioAttribute = key.split("_")[1];
            BIR bir = birBuilder.buildBIR(new BiometricsDto(bioAttribute, null, 0));
            capturedBiometrics.getOrDefault(fieldId, new ArrayList<>()).add(bir);
            exceptionMetaInfo.getOrDefault(fieldId, new HashMap<>()).put(bioAttribute,
                    registrationDTO.getBiometricExceptions().get(key));
        }

        capturedBiometrics.keySet().forEach(fieldId -> {
            BiometricRecord biometricRecord = new BiometricRecord();
            biometricRecord.setOthers(new HashMap<>());
            biometricRecord.getOthers().put(OtherKey.CONFIGURED, String.join(",",
                    registrationDTO.CONFIGURED_BIOATTRIBUTES.getOrDefault(fieldId, Collections.EMPTY_LIST)));
            biometricRecord.setSegments(capturedBiometrics.get(fieldId));
            LOGGER.debug("Adding biometric to packet manager for field : {}", fieldId);
            packetWriter.setBiometric(registrationDTO.getRegistrationId(), fieldId, biometricRecord,
                    source.toUpperCase(), registrationDTO.getProcessId().toUpperCase());
        });

        metaInfoMap.put("biometrics", getJsonString(capturedMetaInfo));
        metaInfoMap.put("exceptionBiometrics", getJsonString(exceptionMetaInfo));*/
        return null;
    }
}
