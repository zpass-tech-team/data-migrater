package io.mosip.data.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.commons.packet.dto.Document;
import io.mosip.commons.packet.dto.packet.DocumentType;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.data.constant.ApiName;
import io.mosip.data.constant.RegistrationConstants;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.biosdk.BioSDKRequest;
import io.mosip.data.dto.biosdk.OtherDto;
import io.mosip.data.dto.biosdk.QualityCheckRequest;
import io.mosip.data.dto.biosdk.SegmentDto;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.packet.metadata.BiometricsMetaInfoDto;
import io.mosip.data.dto.packet.metadata.DocumentMetaInfoDTO;
import io.mosip.data.dto.packet.type.IndividualBiometricType;
import io.mosip.data.dto.packet.type.SimpleType;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.repository.BlocklistedWordsRepository;
import io.mosip.data.service.DataRestClientService;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private String version;

    @Value("${mosip.primary.language}")
    private String primaryLamguage;

    @Autowired
    private BlocklistedWordsRepository blocklistedWordsRepository;

    private LinkedHashMap<String, Object> latestIdSchemaMap;

    private ObjectMapper mapper = new ObjectMapper();

    public LinkedHashMap<String, Object> getLatestIdSchema() throws ApisResourceAccessException {
        if (latestIdSchemaMap == null) {
            ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
            latestIdSchemaMap = (LinkedHashMap<String, Object> ) response.getResponse();
        }
        return latestIdSchemaMap;
    }

    public LinkedHashMap<String, String> setDemographic(LinkedHashMap<String, Object> demoDetails, Boolean isBiometricPresent, List ignorableFields) throws Exception {
        LinkedHashMap<String, String> demoMap = new LinkedHashMap<>();
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
                if (demoDetails.containsKey(id) && demoDetails.get(id) != null)
                    demoMap.put(id, String.valueOf(demoDetails.get(id)));
            } else if (demoDetails.containsKey(id) && demoDetails.get(id) != null) {
                switch (type) {
                    case "simpleType":
                        List<SimpleType> valList = new ArrayList<>();
                        SimpleType simpleType = new SimpleType(primaryLamguage, demoDetails.get(id) == null ? "":demoDetails.get(id).toString());
                        valList.add(simpleType);
                        demoMap.put(id, mapper.writeValueAsString(valList));
                        break;

                    case "number":

                    case "string" :
                        demoMap.put(id, demoDetails.get(id) == null ? "" : String.valueOf(demoDetails.get(id)));
                        break;
                }
            } else if (required && !ignorableFields.contains(id)) {
                throw new Exception("Mandatory Field '" + id + "' value missing");
            }
        }
        return demoMap;
    }

    public LinkedHashMap<String, Document> setDocuments(LinkedHashMap<String, Object> docDetails, List ignorableFields, LinkedHashMap<String, String> metaInfoMap, LinkedHashMap<String, Object> demoDetails)
            throws Exception {

        LinkedHashMap<String, Document> docMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();
            Boolean required = (Boolean) map.get("required");
            String subType = (String) map.get("subType");

            if (type.equals("documentType")) {
                if (docDetails.containsKey(id) && docDetails.get(id) != null) {
                    Document document = mapper.readValue((String) docDetails.get(id), new TypeReference<Document>() {});
                    Document documentDto = new Document();
//                    documentDto.setOwner("Applicant");
                    documentDto.setDocument(document.getDocument());
//                    documentDto.setCategory(subType);
                    documentDto.setFormat(document.getFormat());
                    documentDto.setType(document.getType());
                    documentDto.setRefNumber(document.getRefNumber());
                    documentDto.setValue(id);
                    docMap.put(id,documentDto);

                    DocumentType documentType = new DocumentType(id, document.getType(), document.getFormat(), document.getRefNumber());
                    demoDetails.put(id, mapper.writeValueAsString(documentType));
                }  else if (required && !ignorableFields.contains(id)) {
                    throw new Exception("Mandatory Field '" + id + "' value missing");
                }

            }
        }

        List<DocumentMetaInfoDTO> documentMetaInfoDTOs = new LinkedList<>();
        for (String fieldName : docMap.keySet()) {
            Document document = docMap.get(fieldName);
            DocumentMetaInfoDTO documentMetaInfoDTO = new DocumentMetaInfoDTO();
//            documentMetaInfoDTO.setDocumentCategory(document.getCategory());
            documentMetaInfoDTO.setDocumentName(document.getValue());
//            documentMetaInfoDTO.setDocumentOwner(document.getOwner());
            documentMetaInfoDTO.setDocumentType(document.getType());
            documentMetaInfoDTO.setRefNumber(document.getRefNumber());

            documentMetaInfoDTOs.add(documentMetaInfoDTO);
        }

        metaInfoMap.put("documents", mapper.writeValueAsString(documentMetaInfoDTOs));
        return docMap;
    }

    public LinkedHashMap<String, BiometricRecord> setBiometrics(LinkedHashMap<String, Object> bioDetails, LinkedHashMap<String, String> metaInfoMap) throws Exception {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();

//        LOGGER.debug("Adding Biometrics to packet manager started..");
        LinkedHashMap<String, List<BIR>> capturedBiometrics = new LinkedHashMap<>();
        Map<String, Map<String, Object>> capturedMetaInfo = new LinkedHashMap<>();
        Map<String, Map<String, Object>> exceptionMetaInfo = new LinkedHashMap<>();
        LinkedHashMap<String, BiometricRecord> biometricsMap = new LinkedHashMap<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();
            Boolean required = (Boolean) map.get("required");
            String subtype = map.get("subType").toString();

            if (type.equals("biometricsType")) {
                List<String> bioAttributes = new ArrayList<String>();
                bioAttributes.addAll((List<String>) map.get("bioAttributes"));
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

                        if (bioQualityScore==null) {
                            BiometricType biometricType = Biometric.getSingleTypeByAttribute(bioAttribute);
                            SegmentDto segment = new SegmentDto();
                            segment.setSegments(new ArrayList<>());
                            segment.getSegments().add(bir);
                            segment.setOthers(new OtherDto());
                            QualityCheckRequest request = new QualityCheckRequest();
                            request.setSample(segment);
                            request.setModalitiesToCheck(new ArrayList<>());
                            request.getModalitiesToCheck().add(biometricType.toString());
                            String requestText = (new Gson()).toJson(request);
                            String encodedRequest = Base64.getEncoder().encodeToString(requestText.getBytes(StandardCharsets.UTF_8));
                            BioSDKRequest bioSDKRequest = new BioSDKRequest();
                            bioSDKRequest.setVersion("1.0");
                            bioSDKRequest.setRequest(encodedRequest);
                            ResponseWrapper response= (ResponseWrapper) restApiClient.postApi(ApiName.BIOSDK_QUALITY_CHECK, null, "", bioSDKRequest, ResponseWrapper.class);
                            LinkedHashMap<String, Object> bioSDKResponse = (LinkedHashMap<String, Object>) response.getResponse();
                            if(bioSDKResponse.get("statusCode").equals(200)) {
                                LinkedHashMap<String, Object> resp = (LinkedHashMap<String, Object>) bioSDKResponse.get("response");
                                LinkedHashMap<String, Object> scoreMap = (LinkedHashMap<String, Object>) resp.get("scores");
                                LinkedHashMap<String, Object> modalityMap = (LinkedHashMap<String, Object>) scoreMap.get(biometricType.toString());
                                Double score = (Double) modalityMap.get("score");
                                bir.getBdbInfo().getQuality().setScore(score.longValue());
                            } else {
                                throw new Exception("Error While Calling BIOSDK for Quality Check for Modality " + biometricType.toString() + ", " + bioAttribute);
                            }
                        } else {
                            bir.getBdbInfo().getQuality().setScore(Long.parseLong(bioQualityScore));
                        }

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
        } */

        capturedBiometrics.keySet().forEach(fieldId -> {
            BiometricRecord biometricRecord = new BiometricRecord();
            biometricRecord.setOthers(new HashMap<>());
//            biometricRecord.getOthers().put(OtherKey.CONFIGURED, String.join(",",
//                    registrationDTO.CONFIGURED_BIOATTRIBUTES.getOrDefault(fieldId, Collections.EMPTY_LIST)));
            biometricRecord.setSegments(capturedBiometrics.get(fieldId));
//            LOGGER.debug("Adding biometric to packet manager for field : {}", fieldId);
            biometricsMap.put(fieldId, biometricRecord);
        });

        metaInfoMap.put("biometrics", mapper.writeValueAsString(capturedMetaInfo));
        metaInfoMap.put("exceptionBiometrics", mapper.writeValueAsString(exceptionMetaInfo));
        return biometricsMap;
    }

    public List<Map<String, String>> setAudits(String regId) {
        LocalDateTime responsetime = LocalDateTime.now(ZoneId.of("UTC"));
        String timeStamp = responsetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        List<Map<String, String>> auditList = new LinkedList<>();

        Map<String, String> auditMap = new LinkedHashMap<>();
        auditMap.put("uuid", UUID.randomUUID().toString());
        auditMap.put("createdAt", timeStamp);
        auditMap.put("eventId", "DATA_MIGRATOR_FROM_OTHER_DOMAIN");
        auditMap.put("eventName", "ADD");
        auditMap.put("eventType", "REGISTRATION");
        auditMap.put("hostName", "DATA_MIGRATOR");
        auditMap.put("hostIp", null);
        auditMap.put("applicationId", "REG");
        auditMap.put("applicationName", "REGISTRATION");
        auditMap.put("sessionUserId", null);
        auditMap.put("sessionUserName", null);
        auditMap.put("id", regId);
        auditMap.put("idType", "REGISTRATION_ID");
        auditMap.put("createdBy", "MOSIP");
        auditMap.put("moduleName", "Registration initialization");
        auditMap.put("moduleId", null);
        auditMap.put("description", "Registration Via Data Migrator");
        auditMap.put("actionTimeStamp", timeStamp);
        auditList.add(auditMap);
        return auditList;
    }

    public void setMetaData(Map<String, String> metaInfoMap, PacketDto packetDto, DBImportRequest dbImportRequest) throws JsonProcessingException {
        Map<String, String> metaData = new LinkedHashMap<>();
        metaData.put(PacketManagerConstants.REGISTRATIONID, packetDto.getId());
        metaData.put(RegistrationConstants.PACKET_APPLICATION_ID, packetDto.getId());
        metaData.put(PacketManagerConstants.META_CREATION_DATE, LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        metaData.put(PacketManagerConstants.META_CLIENT_VERSION, ConfigUtil.getConfigUtil().getRegClientVersion());
        metaData.put(PacketManagerConstants.META_REGISTRATION_TYPE, dbImportRequest.getProcess());
        metaData.put(PacketManagerConstants.META_PRE_REGISTRATION_ID, null);
        metaData.put(PacketManagerConstants.META_MACHINE_ID, ConfigUtil.getConfigUtil().getMachineId());
        metaData.put(PacketManagerConstants.META_CENTER_ID, ConfigUtil.getConfigUtil().getCenterId());
        metaData.put(PacketManagerConstants.META_KEYINDEX, ConfigUtil.getConfigUtil().getKeyIndex());
        metaData.put(PacketManagerConstants.META_DONGLE_ID, ConfigUtil.getConfigUtil().getMachineSerialNum());
        metaData.put("langCodes", String.join(RegistrationConstants.COMMA, ConfigUtil.getConfigUtil().getSelectedLanguages()));
        metaData.put(PacketManagerConstants.META_APPLICANT_CONSENT, null);

        metaInfoMap.put("metaData", mapper.writeValueAsString(getLabelValueDTOListString(metaData)));
        metaInfoMap.put("blockListedWords", mapper.writeValueAsString(  blocklistedWordsRepository.findAllActiveBlockListedWords()));
        metaInfoMap.put("capturedRegisteredDevices", mapper.writeValueAsString(new ArrayList<>()));
        metaInfoMap.put("capturedNonRegisteredDevices", mapper.writeValueAsString(new ArrayList<>()));
        metaInfoMap.put("printingName", mapper.writeValueAsString(new ArrayList<>()));

    }

    private List<Map<String, String>> getLabelValueDTOListString(Map<String, String> operationsDataMap) {

        List<Map<String, String>> labelValueMap = new LinkedList<>();

        for (Map.Entry<String, String> fieldName : operationsDataMap.entrySet()) {

            Map<String, String> map = new LinkedHashMap<>();

            map.put("label", fieldName.getKey());
            map.put("value", fieldName.getValue());

            labelValueMap.add(map);
        }

        return labelValueMap;
    }
}
