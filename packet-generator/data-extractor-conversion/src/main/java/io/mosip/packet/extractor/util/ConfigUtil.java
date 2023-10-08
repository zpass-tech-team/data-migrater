package io.mosip.packet.extractor.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.RegistrationConstants;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.config.SyncDataResponseDto;
import io.mosip.packet.core.entity.MachineMaster;
import io.mosip.packet.core.exception.ServiceError;
import io.mosip.packet.core.repository.MachineMasterRepository;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.cryptomanager.util.CryptomanagerUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import static io.mosip.packet.core.constant.GlobalConfig.IS_NETWORK_AVAILABLE;
import static io.mosip.packet.core.constant.GlobalConfig.IS_ONLY_FOR_QUALITY_CHECK;

@Component
@Getter
public class ConfigUtil {

    private String keyIndex;
    private String machineName;
    private String machineId;
    private String centerId;
    private String regClientVersion;
    private String selectedLanguages;
    private String machineSerialNum;

    private ConfigUtil() {
    }

    @Autowired
    private Environment env;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private DataRestClientService restApiClient;

    @Autowired
    private ClientSettingSyncHelper clientSettingSyncHelper;

    @Autowired
    private MachineMasterRepository machineMasterRepository;

    @Autowired
    private KeymanagerService keymanagerService;

    @Autowired
    private CryptomanagerUtils cryptomanagerUtils;

    @Autowired
    private KeymanagerUtil keymanagerUtil;


    private static ConfigUtil configUtil;

    public void loadConfigDetails() throws Exception {
        if (configUtil == null) {
            synchronized (ConfigUtil.class) {
                configUtil = new ConfigUtil();
                configUtil.keyIndex = CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
                configUtil.machineName = InetAddress.getLocalHost().getHostName().toLowerCase();
                configUtil.regClientVersion = env.getProperty("mosip.id.regclient.current.version");
                configUtil.selectedLanguages = env.getProperty("mosip.selected.languages");
                syncClientSettings();
                if(IS_NETWORK_AVAILABLE)
                    fetchPolicy();
                else {
                    System.out.println("Nerwork Not available for Host : " + env.getProperty("mosip.internal.host") + "  Do you want to Continue (Y-Yes, N-No)");
                    Scanner scanner = new Scanner(System.in);
                    String option = scanner.next();

                    if(option.equalsIgnoreCase("n")) {
                        System.exit(1);
                    }
                }
            }
        }
    }

    public static ConfigUtil getConfigUtil() {
        return configUtil;
    }

    @SuppressWarnings("unchecked")
    private void syncClientSettings() throws Exception {
        ResponseWrapper masterSyncResponse = null;

        try {
            ResponseWrapper response = (ResponseWrapper) restApiClient.getApi(ApiName.MASTER_VALIDATOR_SERVICE_NAME, null, "keyindex", configUtil.keyIndex, ResponseWrapper.class);
            IS_NETWORK_AVAILABLE = true;
            String message = getErrorMessage(getErrorList(response));

            if (null != response.getResponse()) {
                saveClientSettings((LinkedHashMap<String, Object>)response.getResponse());
            } else {
                throw new Exception("Machine Not Configured in MOSIP " + message);
            }

            List<MachineMaster> machineMasters = machineMasterRepository.findAll();

            if (machineMasters.size() > 0) {
                configUtil.machineSerialNum = machineMasters.get(0).getSerialNum();
                configUtil.machineId = machineMasters.get(0).getId();
                configUtil.centerId = machineMasters.get(0).getRegCenterId();
            } else {
                throw new Exception("Machine Details Fetching from MOSIP Failed");
            }

            if (!configUtil.machineName.equalsIgnoreCase(machineMasters.get(0).getName()))
                throw new Exception("Machine Name '" + configUtil.machineName + "' not Matching with the MOSIP Configuration");

            if (configUtil.machineId == null || configUtil.machineId.isEmpty())
                throw new Exception("Machine Name '" + configUtil.machineName + "' not Configured in MOSIP System");

            if (configUtil.centerId == null || configUtil.centerId.isEmpty())
                throw new Exception("Registration Center not Configured for Machine Name '" + configUtil.machineName + "' in MOSIP System");
        } catch (Exception e) {
            if(!IS_ONLY_FOR_QUALITY_CHECK)
                throw e;
        }
    }

    private void saveClientSettings(LinkedHashMap<String, Object> masterSyncResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(masterSyncResponse);
        SyncDataResponseDto syncDataResponseDto = mapper.readValue(jsonString,
                new TypeReference<SyncDataResponseDto>() {
                });

        String response = null;
        try {
            response = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
        } catch (Throwable runtimeException) {
            throw new Exception(runtimeException.getMessage());
        }
    }

    private static String getErrorMessage(List<Object> errorList) {

        return errorList != null && errorList.get(0) != null
                ? (String) ((ServiceError)errorList.get(0)).getErrorCode() + " : " + ((ServiceError)errorList.get(0)).getMessage()
                : null;
    }

    private static List<Object> getErrorList(ResponseWrapper syncReponse) {

        return syncReponse.getErrors() != null && syncReponse.getErrors().size() > 0
                ? (List<Object>) syncReponse.getErrors()
                : null;
    }

    public void fetchPolicy() throws Exception {
//        if (!serviceDelegateUtil.isNetworkAvailable()) {
 //           return setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);
//        }
//TODO Need to Implement System online or not before starting process

        String stationId = configUtil.machineId;
        String centerId = stationId != null ? configUtil.centerId : null;
        validate(centerId, stationId);
        String centerMachineId = centerId.concat(RegistrationConstants.UNDER_SCORE).concat(stationId);

        String certificateData = getCertificateFromServer(centerMachineId); //fetch policy key from server
        KeyPairGenerateResponseDto certificateDto = getKeyFromLocalDB(centerMachineId); //get policy key from DB
        //compare downloaded and saved one, if different then save it
        if(certificateDto == null || !Arrays.equals(cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(certificateData)),
                cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(certificateDto.getCertificate())))) {
            UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
            uploadCertRequestDto.setApplicationId(RegistrationConstants.REG_APP_ID);
            uploadCertRequestDto.setCertificateData(certificateData);
            uploadCertRequestDto.setReferenceId(centerMachineId);
            keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
        }
    }

    private boolean validate(String centerId, String machineId)
            throws Exception {
        if (centerId == null || machineId == null)
                throw new Exception("Machine ID & Center ID are Empty");

        return true;
    }

    private String getCertificateFromServer(String centerMachineId) throws Exception {
        List<String> queryParm = new ArrayList<>();
        queryParm.add(RegistrationConstants.GET_CERT_APP_ID);
        queryParm.add(RegistrationConstants.REF_ID);

        List<String> queryParmValue = new ArrayList<>();
        queryParmValue.add(RegistrationConstants.REG_APP_ID);
        queryParmValue.add(centerMachineId);

        ResponseWrapper responseWrapper = (ResponseWrapper) restApiClient
                .getApi(ApiName.GET_CERTIFICATE, null, queryParm, queryParmValue, ResponseWrapper.class);

        if(null != responseWrapper.getResponse()) {
            LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) responseWrapper.getResponse();
            return responseMap.get(RegistrationConstants.CERTIFICATE).toString();
        }

        if(responseWrapper.getErrors() != null &&
                responseWrapper.getErrors().size() > 0 ) {
            throw new Exception("Error : " + responseWrapper.getErrors());
//            LOGGER.error("Get Policy key from server failed with error {}", publicKeySyncResponse.get(RegistrationConstants.ERRORS));
        }

        throw new Exception("Failed to Policy Sync");
    }

    private KeyPairGenerateResponseDto getKeyFromLocalDB(String refId) {
        try {
            KeyPairGenerateResponseDto certificateDto = keymanagerService
                    .getCertificate(RegistrationConstants.REG_APP_ID, Optional.of(refId));

            if(certificateDto != null && certificateDto.getCertificate() != null)
                return certificateDto;

        } catch (Exception ex) {
 //           LOGGER.error("Error Fetching policy key from DB", ex);
        }
        return null;
    }

}
