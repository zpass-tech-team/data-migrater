package io.mosip.packet.core.config.biosdk;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.packet.core.spi.BioSdkApiFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "mosip.biometric.sdk.providers")
@Component
public class BioSDKConfig {

    @Getter
    @Setter
    private Map<String, Map<String, String>> finger;

    @Getter
    @Setter
    private Map<String, Map<String, String>> iris;

    @Getter
    @Setter
    private Map<String, Map<String, String>> face;

    @Value("${mosip.biometric.sdk.providers.default.finger.organization-name}")
    private String defaultFingerBioSDKName;

    @Value("${mosip.biometric.sdk.providers.default.iris.organization-name}")
    private String defaultIrisBioSDKName;

    @Value("${mosip.biometric.sdk.providers.default.face.organization-name}")
    private String defaultFaceBioSDKName;
    @Autowired
    private List<BioSdkApiFactory> bioSdkApiFactoryList;

    @Getter
    private Map<BiometricType, BioSdkApiFactory> defaultBioSDK;

    @Getter
    private Map<BiometricType, Map<String, BioSdkApiFactory>> bioSDKList;

    @PostConstruct
    private void loadConfiguration() throws Exception {
        bioSDKList = new HashMap<>();
        defaultBioSDK = new HashMap<>();
        bioSDKList.put(BiometricType.FINGER, new HashMap<>());
        bioSDKList.put(BiometricType.FACE, new HashMap<>());
        bioSDKList.put(BiometricType.IRIS, new HashMap<>());
        String organizationName = null, className=null;


        try {
            for(Map.Entry entry : finger.entrySet()) {
                Map<String, String> entryMap = (Map)entry.getValue();
                organizationName = entryMap.get("organization-name");
                className = entryMap.get("classname");

                bioSDKList.get(BiometricType.FINGER).put(organizationName, getBioSDKApiFactory(className));

                if(organizationName.equals(defaultFingerBioSDKName))
                    defaultBioSDK.put(BiometricType.FINGER, bioSDKList.get(BiometricType.FINGER).get(organizationName));
            }

            for(Map.Entry entry : face.entrySet()) {
                Map<String, String> entryMap = (Map)entry.getValue();
                organizationName = entryMap.get("organization-name");
                className = entryMap.get("classname");

                bioSDKList.get(BiometricType.FACE).put(organizationName, getBioSDKApiFactory(className));

                if(organizationName.equals(defaultFaceBioSDKName))
                    defaultBioSDK.put(BiometricType.FACE, bioSDKList.get(BiometricType.FACE).get(organizationName));
            }

            for(Map.Entry entry : iris.entrySet()) {
                Map<String, String> entryMap = (Map)entry.getValue();
                organizationName = entryMap.get("organization-name");
                className = entryMap.get("classname");

                bioSDKList.get(BiometricType.IRIS).put(organizationName, getBioSDKApiFactory(className));

                if(organizationName.equals(defaultIrisBioSDKName))
                    defaultBioSDK.put(BiometricType.IRIS, bioSDKList.get(BiometricType.IRIS).get(organizationName));
            }
        } catch (Exception e) {
            throw new Exception(e.getLocalizedMessage() + " for Organization : " + organizationName + " and ClassName : " + className);
        }


    }

    private BioSdkApiFactory getBioSDKApiFactory(String className) throws Exception {
        for(BioSdkApiFactory factory : bioSdkApiFactoryList) {
            if(factory.getClass().getName().equals(className))
                return factory;
        }

        throw new Exception("Implementation of API Factory not found");
    }
}
