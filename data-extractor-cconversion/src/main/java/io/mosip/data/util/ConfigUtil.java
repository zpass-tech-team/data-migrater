package io.mosip.data.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.data.constant.ApiName;
import io.mosip.data.constant.RegistrationConstants;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.config.SyncDataResponseDto;
import io.mosip.data.entity.MachineMaster;
import io.mosip.data.repository.MachineMasterRepository;
import io.mosip.data.service.DataRestClientService;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.*;

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

    private static ConfigUtil configUtil;

    public void loadConfigDetails() throws Exception {
        if (configUtil == null) {
            synchronized (ConfigUtil.class) {
                configUtil = new ConfigUtil();
                configUtil.keyIndex = CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
                configUtil.machineName = InetAddress.getLocalHost().getHostName().toLowerCase();
                configUtil.regClientVersion = env.getProperty("mosip.id.regclient.current.version");
                configUtil.selectedLanguages = env.getProperty("mosip.selected.languages");
                createDatabase();
                syncClientSettings();
            }
        }
    }

    public void createDatabase() throws Exception {
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(env.getProperty("spring.datasource.url"), env.getProperty("spring.datasource.username"), env.getProperty("spring.datasource.password"));

                org.apache.derby.tools.ij.runScript(connection, ConfigUtil.class.getClassLoader().getResourceAsStream("initial.sql"), "UTF-8", System.out, "UTF-8");

                Path path = Paths.get(System.getProperty("user.dir"), "external_db.sql");
                if(path.toFile().exists()) {
                    org.apache.derby.tools.ij.runScript(connection, new FileInputStream(path.toFile()), "UTF-8", System.out, "UTF-8");
                }
            } finally {
                if (connection != null)
                    connection.close();
            }
    }

    public static ConfigUtil getConfigUtil() {
        return configUtil;
    }

    @SuppressWarnings("unchecked")
    private void syncClientSettings() throws Exception {
        ResponseWrapper masterSyncResponse = null;

        ResponseWrapper response = (ResponseWrapper) restApiClient.getApi(ApiName.MASTER_VALIDATOR_SERVICE_NAME, null, "keyindex", configUtil.keyIndex, ResponseWrapper.class);

        String message = getErrorMessage(getErrorList(response));

        if (null != response.getResponse()) {
            saveClientSettings((LinkedHashMap<String, Object>)response.getResponse());
        } else {
            throw new Exception("Machine Not Configured in MOSIP " + message);
        }

        List<MachineMaster> machineMasters = machineMasterRepository.findAll();
        configUtil.machineSerialNum = machineMasters.get(0).getSerialNum();
        configUtil.machineId = machineMasters.get(0).getId();
        configUtil.centerId = machineMasters.get(0).getRegCenterId();

        if (!configUtil.machineName.equalsIgnoreCase(machineMasters.get(0).getName()))
            throw new Exception("Machine Name '" + configUtil.machineName + "' not Matching with the MOSIP Configuration");

        if (configUtil.machineId == null || configUtil.machineId.isEmpty())
            throw new Exception("Machine Name '" + configUtil.machineName + "' not Configured in MOSIP System");

        if (configUtil.centerId == null || configUtil.centerId.isEmpty())
            throw new Exception("Registration Center not Configured for Machine Name '" + configUtil.machineName + "' in MOSIP System");
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

    private static String getErrorMessage(List<Map<String, Object>> errorList) {

        return errorList != null && errorList.get(0) != null
                ? (String) errorList.get(0).get(RegistrationConstants.ERROR_CODE) + " : " + errorList.get(0).get(RegistrationConstants.MESSAGE_CODE)
                : null;
    }

    private static List<Map<String, Object>> getErrorList(ResponseWrapper syncReponse) {

        return syncReponse.getErrors() != null && syncReponse.getErrors().size() > 0
                ? (List<Map<String, Object>>) syncReponse.getErrors()
                : null;
    }
}
