package io.mosip.packet.extractor.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.config.activity.Activity;
import io.mosip.packet.core.constant.*;
import io.mosip.packet.core.constant.activity.ActivityName;
import io.mosip.packet.core.constant.tracker.TrackerStatus;
import io.mosip.packet.core.dto.DataPostProcessorResponseDto;
import io.mosip.packet.core.dto.DataProcessorResponseDto;
import io.mosip.packet.core.dto.RequestWrapper;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.biosdk.BioSDKRequestWrapper;
import io.mosip.packet.core.dto.dbimport.*;
import io.mosip.packet.core.dto.packet.PacketRequest;
import io.mosip.packet.core.dto.packet.RegistrationIdRequest;
import io.mosip.packet.core.dto.tracker.TrackerRequestDto;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.exception.ExceptionUtils;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.core.service.thread.*;
import io.mosip.packet.core.spi.BioConvertorApiFactory;
import io.mosip.packet.core.spi.QualityWriterFactory;
import io.mosip.packet.core.spi.dataexporter.DataExporterApiFactory;
import io.mosip.packet.core.spi.datapostprocessor.DataPostProcessorApiFactory;
import io.mosip.packet.core.spi.dataprocessor.DataProcessorApiFactory;
import io.mosip.packet.core.spi.datareader.DataReaderApiFactory;
import io.mosip.packet.core.util.*;
import io.mosip.packet.extractor.service.DataExtractionService;
import io.mosip.packet.extractor.util.ValidationUtil;
import io.mosip.packet.manager.util.PacketCreator;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.MockDeviceUtil;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.mosip.packet.core.constant.GlobalConfig.SESSION_KEY;
import static io.mosip.packet.core.constant.GlobalConfig.*;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Service
public class DataExtractionServiceImpl implements DataExtractionService {

    private static final Logger LOGGER = DataProcessLogger.getLogger(DataExtractionServiceImpl.class);

    @Value("${mosip.packet.upload.max-threadpool-count:1}")
    private Integer uploadMaxThreadPoolCount;

    @Value("${mosip.packet.upload.max-records-process-per-threadpool:10000}")
    private Integer uploadMaxRecordsCountPerThreadPool;

    @Value("${mosip.packet.upload.max-thread-execution-count:5}")
    private Integer uploadMaxThreadExecCount;

    @Value("${mosip.packet.uploader.enable.only.packet.upload:false}")
    private boolean enableOnlyPacketUploader;

    @Autowired
    ValidationUtil validationUtil;

    @Autowired
    private MockDeviceUtil mockDeviceUtil;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private PacketCreator packetCreator;

    @Autowired
    private TrackerUtil trackerUtil;

    @Autowired
    private QualityWriterFactory qualityWriterFactory;

    @Autowired
    private PacketTrackerRepository packetTrackerRepository;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private DataRestClientService dataRestClientService;

    private boolean uploadProcessStarted = false;
    private boolean isUploadInProgress = false;

    private Map<String, HashMap<String, String>> fieldsCategoryMap = new HashMap<>();

    @Autowired
    private BioConvertorApiFactory bioConvertorApiFactory;

    @Autowired
    private BioSDKUtil bioSDKUtil;

    @Autowired
    private DataReaderApiFactory dataReaderApiFactory;

    @Autowired
    private DataProcessorApiFactory dataProcessorApiFactory;

    @Autowired
    private DataPostProcessorApiFactory dataPostProcessorApiFactory;

    @Autowired
    private DataExporterApiFactory dataExporterApiFactory;

    @Autowired
    private Activity activity;

    @Override
    public HashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        HashMap<String, Object> biodata = new HashMap<>();
        dataReaderApiFactory.connectDataReader(dbImportRequest);
        populateTableFields(dbImportRequest);
        commonUtil.updateFieldCategory(dbImportRequest);

        List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
        Collections.sort(tableRequestDtoList);
        TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
 /*       ResultSet resultSet = null;
        resultSet = dataBaseUtil.readDataFromDatabase(dbImportRequest, null, fieldsCategoryMap);

        if (resultSet != null) {
            dataBaseUtil.populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet, null, dataMap, fieldsCategoryMap, localStoreRequired, false);

            for (Map<FieldCategory, HashMap<String, Object>> dataHashMap : dataMap) {
                for (int i = 1; i < tableRequestDtoList.size(); i++) {
                    TableRequestDto tableRequestDto1 = tableRequestDtoList.get(i);
                    resultSet = dataBaseUtil.readDataFromDatabase(tableRequestDto1, dataHashMap, fieldsCategoryMap);

                    if (resultSet != null) {
                        dataBaseUtil.populateDataFromResultSet(tableRequestDto1, dbImportRequest.getColumnDetails(), resultSet, dataHashMap, dataMap, fieldsCategoryMap, localStoreRequired, false);
                    }

                    for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                        if(fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                            byte[] convertedImageData = (byte[]) dataHashMap.get(FieldCategory.BIO).get(fieldFormatRequest.getFieldToMap());
                            biodata.put(dataHashMap.get(FieldCategory.DEMO).get(fieldFormatRequest.getPrimaryField())+ "-" + fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getFieldName()),  convertedImageData);
                        }
                    }
                }
            }
        }*/

        return biodata;
    }

    @Override
    public HashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        HashMap<String, Object> bioData = extractBioDataFromDBAsBytes(dbImportRequest, localStoreRequired);
        HashMap<String, Object> convertedData = new HashMap<>();

        for(Map.Entry<String, Object> entry : bioData.entrySet()) {
            String data = Base64.getEncoder().encodeToString((byte[])entry.getValue());
             convertedData.put(entry.getKey(), data);
        }
        return convertedData;
    }

    @Override
    public PacketCreatorResponse createPacketFromDataBase(DBImportRequest dbImportRequest) throws Exception {
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "DataExtractionServiceImpl :: createPacketFromDataBase():: entry");
        TIMECONSUPTIONQUEUE = new FixedListQueue<Long>(100);
        PacketCreatorResponse packetCreatorResponse = new PacketCreatorResponse();
        packetCreatorResponse.setRID(new ArrayList<>());
        PacketDto packetDto = null;
        TOTAL_RECORDS_FOR_PROCESS=0L;

        try {
            Date startTime = new Date();
            IS_PACKET_CREATOR_OPERATION = true;
            List<ValidatorEnum> enumList = new ArrayList<>();
            enumList.add(ValidatorEnum.ID_SCHEMA_VALIDATOR);
            enumList.add(ValidatorEnum.FILTER_VALIDATOR);
            enumList.add(ValidatorEnum.BIOMETRIC_FORMAT_VALIDATOR);

            mockDeviceUtil.resetDevices();
            mockDeviceUtil.initDeviceHelpers();
            commonUtil.initialize(dbImportRequest);
            validationUtil.validateRequest(dbImportRequest, enumList);
            populateTableFields(dbImportRequest);
            dataReaderApiFactory.connectDataReader(dbImportRequest);

            ResultSetter setter = new ResultSetter() {
                @SneakyThrows
                @Override
                public void setResult(Object obj) {
                    ResultDto resultDto = (ResultDto) obj;
                    if(!packetCreatorResponse.getRID().contains(resultDto.getRegNo()))
                        packetCreatorResponse.getRID().add(resultDto.getRegNo());
                    TrackerRequestDto trackerRequestDto = new TrackerRequestDto();
                    trackerRequestDto.setRegNo(resultDto.getRegNo());
                    trackerRequestDto.setRefId(resultDto.getRefId());
                    trackerRequestDto.setProcess(dbImportRequest.getProcess());
                    trackerRequestDto.setActivity(GlobalConfig.getActivityName());
                    trackerRequestDto.setSessionKey(SESSION_KEY);
                    trackerRequestDto.setStatus(resultDto.getStatus().toString());
                    trackerRequestDto.setComments(resultDto.getComments());
                    trackerRequestDto.setAdditionalMaps(resultDto.getAdditionalMaps());
                    trackerUtil.addTrackerEntry(trackerRequestDto);
                    trackerUtil.addTrackerLocalEntry(resultDto.getRefId(), null, resultDto.getStatus(), dbImportRequest.getProcess(), resultDto.getComments(), SESSION_KEY, GlobalConfig.getActivityName());
                }
            };

            ResultSetter DataProcessor = new ResultSetter() {
                @SneakyThrows
                @Override
                public void setResult(Object obj) {
                    Map<FieldCategory, HashMap<String, Object>> dataHashMap = (Map<FieldCategory, HashMap<String, Object>>) obj;
                    TrackerRequestDto trackerRequestDto = new TrackerRequestDto();
                    trackerRequestDto.setRegNo(null);
                    trackerRequestDto.setRefId(dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()).toString());
                    trackerRequestDto.setProcess(dbImportRequest.getProcess());
                    trackerRequestDto.setActivity(GlobalConfig.getActivityName());
                    trackerRequestDto.setSessionKey(SESSION_KEY);
                    trackerRequestDto.setStatus(TrackerStatus.STARTED.toString());
                    trackerRequestDto.setComments("Object Ready For Processing");

                    if (!PACKET_TRACKER_ADDITIONAL_FIELDS.isEmpty()) {
                        Map<String, Object> additionalMap = new HashMap<>();
                        for (String key : PACKET_TRACKER_ADDITIONAL_FIELDS.keySet()) {
                            additionalMap.put(key, dataHashMap.get(FieldCategory.DEMO).get(key));
                        }
                        trackerRequestDto.setAdditionalMaps(additionalMap);
                    }
                    trackerUtil.addTrackerEntry(trackerRequestDto);

                    DataProcessorResponseDto processObject = dataProcessorApiFactory.process(dbImportRequest, dataHashMap, setter);

                    if(!IS_ONLY_FOR_QUALITY_CHECK) {
                        if(GlobalConfig.getApplicableActivityList().contains(ActivityName.DATA_POST_PROCESSOR)) {
                            DataPostProcessorResponseDto postProcessorResponseDto = dataPostProcessorApiFactory.postProcess(processObject, setter, startTime.getTime());
                        }
                    } else {
                        ResultDto resultDto = new ResultDto();
                        resultDto.setRegNo(null);
                        resultDto.setRefId(processObject.getRefId());
                        resultDto.setComments("Quality Calculation Completed Successfully");
                        resultDto.setStatus(TrackerStatus.PROCESSED_WITHOUT_UPLOAD);
                        setter.setResult(resultDto);
                    }
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + processObject.getRefId()+ " Process Ended");
                    Long endTime = System.nanoTime();
                    Long timeDifference = endTime-startTime.getTime();
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + processObject.getRefId() + " Time taken to complete " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));
                    TIMECONSUPTIONQUEUE.add(timeDifference);
                }
            };

            if(GlobalConfig.getApplicableActivityList().contains(ActivityName.DATA_EXPORTER)) {
                Activity exportActivity = activity.getActivity(ActivityName.DATA_EXPORTER.name());
                CustomizedThreadPoolExecutor uploadExector = new CustomizedThreadPoolExecutor(uploadMaxThreadPoolCount, uploadMaxRecordsCountPerThreadPool,uploadMaxThreadExecCount, exportActivity.getActivityName().getActivityName(), exportActivity.isMonitorRequired());
                Timer uploaderTimer = new Timer("Uploading Packet");
                uploaderTimer.schedule(new TimerTask() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        String packetId=null;
                        try {
                            if(!uploadProcessStarted) {
                                uploadProcessStarted = true;
                                isUploadInProgress = true;
                                List<String> statusList = new ArrayList<>();
                                statusList.add("READY_TO_SYNC");
                                List<PacketTracker> trackerList =  packetTrackerRepository.findByStatusIn(statusList);

                                if(trackerList.size() <= 0) {
                                    uploadExector.setInputProcessCompleted(true);
                                } else {
                                    uploadExector.setInputProcessCompleted(false);
                                }

                                for(PacketTracker packetTracker : trackerList) {
                                    ByteArrayInputStream bis = new ByteArrayInputStream(clientCryptoFacade.getClientSecurity().isTPMInstance() ? clientCryptoFacade.decrypt(Base64.getDecoder().decode(packetTracker.getRequest())) : Base64.getDecoder().decode(packetTracker.getRequest()));
                                    ObjectInputStream is = new ObjectInputStream(bis);
                                    DataPostProcessorResponseDto responseDto = (DataPostProcessorResponseDto) is.readObject();
                                    is.close();
                                    bis.close();
                                    packetId = responseDto.getRefId();
                                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Data Export for " + (new Gson()).toJson(responseDto));

                                    ThreadUploadController controller = new ThreadUploadController();
                                    controller.setResult(responseDto);
                                    controller.setSetter(setter);
                                    controller.setProcessor(new ThreadUploadProcessor() {
                                        @Override
                                        public void processData(ResultSetter setter, DataPostProcessorResponseDto result) throws Exception {
                                            dataExporterApiFactory.export(result, (new Date()).getTime(), setter);
                                        }
                                    });
                                    uploadExector.ExecuteTask(controller);
                                }
                                isUploadInProgress = false;
                            }

                            LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Upload Batch In-progress firs " + uploadExector.getCurrentPendingCount());
                            LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Upload Batch Is-Upload-Inprogress " + isUploadInProgress);
                            if(uploadExector.getCurrentPendingCount() <= 0 && !isUploadInProgress)
                                uploadProcessStarted = false;
                        } catch (Exception e) {
                            if(uploadExector.getCurrentPendingCount() <= 0)
                                uploadProcessStarted = false;
                            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Upload Error for Packet Id : " + packetId + " - " + e.getMessage() + ExceptionUtils.getStackTrace(e));
                        }
                    }
                }, 0, 5000L);
            }

            if(!enableOnlyPacketUploader)
                dataReaderApiFactory.readData(dbImportRequest, null, fieldsCategoryMap, DataProcessor);

            do {
                Thread.sleep(15000);

                if(!IS_DATABASE_READ_OPERATION) {
                    IS_PACKET_CREATOR_OPERATION = false;
                }
            } while(!GlobalConfig.isThreadPoolCompleted());

            System.out.println("Start Time " + startTime);
            System.out.println("End Time Time " + new Date());
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
            dataReaderApiFactory.disconnectDataReader();
            if(!IS_ONLY_FOR_QUALITY_CHECK)
                trackerUtil.closeStatement();

            qualityWriterFactory.preDestroyProcess();
        }
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Uploaded List : " + (new Gson()).toJson(packetCreatorResponse));
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "DataExtractionServiceImpl :: createPacketFromDataBase():: exit");

        return packetCreatorResponse;
    }

    @Override
    public String refreshQualityAnalysisData() throws Exception {
        qualityWriterFactory.preDestroyProcess();
        return "Quality Analysis Data Refresh Successfully";
    }

    @Override
    public String extractBioDataFromPacket(RegistrationIdRequest registrationIdRequest) throws Exception {
        List<String> ridList = registrationIdRequest.getRids();
        HashMap<String, String> csvMap = qualityWriterFactory.getDataMap();

        if(ridList == null || ridList.isEmpty())
            throw new Exception("No RID Provided in the Request.");

        for(String rid : ridList) {
            HashMap<String, List<HashMap<String, Object>>> capturedBiometrics = new HashMap<>();

            PacketRequest packetRequest = new PacketRequest();
            packetRequest.setBypassCache(true);
            packetRequest.setId(rid);
            packetRequest.setProcess("NEW");
            packetRequest.setSource("REGISTRATION_CLIENT");
            packetRequest.setPerson("individualBiometrics");

            List<String> modalityList = new ArrayList<>();
            modalityList.add("Iris");
            modalityList.add("Finger");
            modalityList.add("Face");
            packetRequest.setModalities(modalityList);

            RequestWrapper<PacketRequest> requestWrapper = new RequestWrapper<>();
            requestWrapper.setRequest(packetRequest);
            requestWrapper.setVersion("1.0");
            requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
            LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Fetching Biometrics from Packet Manager for RID " + rid);
            ResponseWrapper<HashMap<String, List<HashMap<String, Object>>>> responseWrapper = (ResponseWrapper) dataRestClientService.postApi(ApiName.PACKET_BIOMETRIC_READER, null, null, requestWrapper, ResponseWrapper.class, MediaType.APPLICATION_JSON);
            capturedBiometrics = responseWrapper.getResponse();
            List<HashMap<String, Object>> birList = capturedBiometrics.get("segments");

            for(HashMap<String, Object> birObject : birList) {
                birObject.remove("birs");
                ((HashMap)birObject.get("bdbInfo")).remove("creationDate");
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(birObject);
                BIR bir = mapper.readValue(jsonString,
                        new TypeReference<BIR>() {
                        });

                String subType = bir.getBdbInfo().getSubtype().stream().map(String::valueOf).collect(Collectors.joining(" "));
                String type = bir.getBdbInfo().getType().stream().map(String::valueOf).collect(Collectors.joining(" "));
                String key = type + (subType != null && !subType.isEmpty() ? "-" + subType : "");
                LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Processing BIR Type " + type + " And Sub Type " + subType + " for RID " + rid);

                if(bir.getBdb() != null && bir.getBdb().length > 0) {
                    FieldFormatRequest fieldFormatRequest = new FieldFormatRequest();
                    fieldFormatRequest.setFieldName(key.replace(" ", ""));
                    fieldFormatRequest.setSrcFormat(DataFormat.ISO);
                    fieldFormatRequest.getDestFormat().add(DataFormat.JPEG);
                    byte[] convertedImage = convertBiometric(rid, fieldFormatRequest, bir.getBdb(), true, BioSubType.getBioAttribute(subType).getBioAttribute());
                    bir.setBdb(convertedImage);
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Image Convertion Completed for BIR Type " + type + " And Sub Type " + subType + " for RID " + rid);

                    BioSDKRequestWrapper bioSDKrequestWrapper = new BioSDKRequestWrapper();
                    bioSDKrequestWrapper.setSegments(new ArrayList<>());
                    bioSDKrequestWrapper.getSegments().add(bir);
                    bioSDKrequestWrapper.setBiometricType(bir.getBdbInfo().getType().get(0).value());
                    bioSDKrequestWrapper.setFormat(DataFormat.JPEG.getFileFormat());
                    bioSDKrequestWrapper.setInputObject(csvMap);
                    bioSDKrequestWrapper.setIsOnlyForQualityCheck(IS_ONLY_FOR_QUALITY_CHECK);
                    Long startTime = System.nanoTime();

                    try {
                        Double score = Double.parseDouble(bioSDKUtil.calculateQualityScore(bioSDKrequestWrapper, key, rid, startTime));
                        LOGGER.debug("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Quality Score is " + score + " for BIR Type " + type + " And Sub Type " + subType + " for RID " + rid);
                    } catch (Exception e) {
                        LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Exception for RID : " + rid +  " for BIR Type " + type + " And Sub Type " + subType + " Exception : " +  e.getMessage() + ExceptionUtils.getStackTrace(e));
                    }
                } else {
                    csvMap.put(key, "");
                }
            }

            csvMap.put("reg_no", rid);
            csvMap.put("ref_id", rid);
            qualityWriterFactory.writeQualityData(csvMap);
        }
        return null;
    }

    public byte[] convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue, Boolean localStoreRequired, String fieldName) throws Exception {
        if (localStoreRequired) {
            bioConvertorApiFactory.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getOriginalFieldName() , bioValue, fieldFormatRequest.getSrcFormat());
            return bioConvertorApiFactory.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getOriginalFieldName(), bioConvertorApiFactory.convertImage(fieldFormatRequest, bioValue, fieldName), fieldFormatRequest.getDestFormat().get(fieldFormatRequest.getDestFormat().size()-1));
        } else {
            return bioConvertorApiFactory.convertImage(fieldFormatRequest, bioValue, fieldName);
        }
    }

    private void populateTableFields(DBImportRequest dbImportRequest) throws Exception {
        fieldsCategoryMap.clear();

        for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
            String tableName = DEFAULT_TABLE;
            if (fieldFormatRequest.getFieldName().contains(",")) {
                switch(dbImportRequest.getDbType().toString()) {
                    case "MSSQL":
                    case "ORACLE":
                    case "MYSQL":
                    case "POSTGRESQL":
                        for (FieldName fieldName : fieldFormatRequest.getFieldList()) {
                            if(fieldName.getTableName() != null)
                                tableName = fieldName.getTableName();

                            if (!fieldsCategoryMap.containsKey(tableName))
                                fieldsCategoryMap.put(tableName, new HashMap<>());

                            if(fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC))
                                fieldsCategoryMap.get(tableName).put(fieldName.getOriginalFieldName() + " AS " + fieldName.getModifiedFieldName(), fieldFormatRequest.getStaticValue());
                            else
                                fieldsCategoryMap.get(tableName).put(fieldName.getOriginalFieldName(), fieldFormatRequest.getStaticValue());
                        }
                        break;

                    default:
                        throw new Exception("Implementation missing for Database to Read Data DBType :" +  dbImportRequest.getDbType().toString());
                }
            } else {
                FieldName fieldName = fieldFormatRequest.getFieldList().get(0);
                if(fieldName.getTableName() != null)
                    tableName = fieldName.getTableName();

                if (!fieldsCategoryMap.containsKey(tableName))
                    fieldsCategoryMap.put(tableName, new HashMap<>());

                if(fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC))
                    fieldsCategoryMap.get(tableName).put(fieldName.getOriginalFieldName() + " AS " + fieldName.getModifiedFieldName(), fieldFormatRequest.getStaticValue());
                else
                    fieldsCategoryMap.get(tableName).put(fieldName.getOriginalFieldName(), fieldFormatRequest.getStaticValue());
            }

            if(fieldFormatRequest.getPrimaryField() != null)
                fieldsCategoryMap.get(tableName).put(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getPrimaryField()),null);
            if(fieldFormatRequest.getSrcFieldForQualityScore() != null)
                fieldsCategoryMap.get(tableName).put(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore()), null);

            if(fieldFormatRequest.getDocumentAttributes() != null) {
                DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                DocumentValueMap documentValueMap = documentAttributes.getDocumentValueMap();
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentRefNoField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField()) + "' AS " + fieldFormatRequest.getFieldToMap() + "_STATIC_" +  commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentRefNoField()) + " AS " + fieldFormatRequest.getFieldToMap() + "_" + fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentRefNoField()), null);
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentFormatField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField()) + "' AS " + fieldFormatRequest.getFieldToMap() + "_STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentFormatField()) + " AS " + fieldFormatRequest.getFieldToMap() + "_" + fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentFormatField()),null);
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentCodeField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField()) + "' AS " + fieldFormatRequest.getFieldToMap() + "_STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentCodeField()) + " AS " + fieldFormatRequest.getFieldToMap() + "_" + fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentCodeField()), null);

                if(documentValueMap != null) {
                    fieldsCategoryMap.get(tableName).put(fieldFormatRequest.getFieldNameWithoutSchema(documentValueMap.getMapColumnName()), null);
                }
            }
        }
    }
}
