package io.mosip.data.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.data.constant.ApiName;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.packet.metadata.BiometricsMetaInfoDto;
import io.mosip.data.dto.packet.type.IndividualBiometricType;
import io.mosip.data.dto.packet.type.SimpleType;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.service.DataRestClientService;
import io.mosip.kernel.biometrics.constant.OtherKey;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PacketCreator {

    @Autowired
    private DataRestClientService restApiClient;

    @Autowired
    private Environment env;

    @Autowired
    private BIRBuilder birBuilder;

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

            } else if (demoDetails.containsKey(id) && demoDetails.get(id) != null) {
                switch (type) {
                    case "simpleType":
                        List<SimpleType> valList = new ArrayList<>();
                        SimpleType simpleType = new SimpleType(primaryLamguage, demoDetails.get(id.toLowerCase()) == null ? "":demoDetails.get(id.toLowerCase()).toString());
                        valList.add(simpleType);
                        demoMap.put(id, mapper.writeValueAsString(valList));
                        break;

                    case "number":

                    case "string" :
                        demoMap.put(id, demoDetails.get(id) == null ? "" : demoDetails.get(id));
                        break;
                }
            } else if (required && !ignorableFields.contains(id)) {
                throw new Exception("Mandatory Field '" + id + "' value missing");
            }
        }
        return demoMap;
    }

    public LinkedHashMap<String, List<BIR>> setBiometrics(LinkedHashMap<String, Object> bioDetails, LinkedHashMap<String, String> metaInfoMap) throws ApisResourceAccessException, JsonProcessingException {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();

//        LOGGER.debug("Adding Biometrics to packet manager started..");
        LinkedHashMap<String, List<BIR>> capturedBiometrics = new LinkedHashMap<>();
        Map<String, Map<String, Object>> capturedMetaInfo = new LinkedHashMap<>();
        Map<String, Map<String, Object>> exceptionMetaInfo = new LinkedHashMap<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();
            Boolean required = (Boolean) map.get("required");
            String subtype = map.get("subType").toString();

            if (type.equals("biometricsType")) {
                List<String> bioAttributes = (List<String>) map.get("bioAttributes");
                Integer attributeCount = bioAttributes.size();
                bioAttributes.add("unknown");

                for (Map.Entry<String, Object> entry : bioDetails.entrySet()) {
                    String[] keyEntries = entry.getKey().split("_");
                    String fieldId = keyEntries[0];
                    String bioAttribute = keyEntries[1];

                    if(fieldId.equals(id) && bioAttributes.contains(bioAttribute)) {
                        bioAttributes.remove(bioAttribute);
                        String bioQualityScore = keyEntries.length > 2 ? keyEntries[2] : null;
                        BIR bir = birBuilder.buildBIR(bioAttribute, (byte[])entry.getValue(), bioQualityScore);
                        if (!capturedBiometrics.containsKey(fieldId)) {
                            capturedBiometrics.put(fieldId, new ArrayList<>());
                        }
                        capturedBiometrics.get(fieldId).add(bir);
                        if (!capturedMetaInfo.containsKey(fieldId)) {
                            capturedMetaInfo.put(fieldId, new HashMap<>());
                        }
                        capturedMetaInfo.get(fieldId).put(bioAttribute, new BiometricsMetaInfoDto(1, false, bir.getBdbInfo().getIndex()));
                    }
                }
                bioAttributes.remove("unknown");

                if (attributeCount.equals(bioAttributes.size()))
                    bioAttributes.clear();

                for (String bioAttribute : bioAttributes) {
                    BIR bir = birBuilder.buildBIR(bioAttribute, null, "0");
                    if (!capturedBiometrics.containsKey(id)) {
                        capturedBiometrics.put(id, new ArrayList<>());
                    }
                    capturedBiometrics.get(id).add(bir);
                    if (!capturedMetaInfo.containsKey(id)) {
                        capturedMetaInfo.put(id, new HashMap<>());
                    }
                    capturedMetaInfo.get(id).put(bioAttribute, new BiometricsMetaInfoDto(1, false, bir.getBdbInfo().getIndex()));
                }
            }

        }

  /*      for(String key : registrationDTO.getBiometricExceptions().keySet()) {
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
        }); */

        metaInfoMap.put("biometrics", mapper.writeValueAsString(capturedMetaInfo));
        metaInfoMap.put("exceptionBiometrics", mapper.writeValueAsString(exceptionMetaInfo));
        return capturedBiometrics;
    }
}
