package io.mosip.packet.manager.util.mock.sbi.devicehelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Random;

import io.mosip.packet.core.constant.SBIConstant;
import io.mosip.packet.core.dto.mockmds.DeviceInfo;
import io.mosip.packet.core.dto.mockmds.DigitalId;
import io.mosip.packet.core.util.mockmds.CryptoUtility;
import io.mosip.packet.core.util.mockmds.FileHelper;
import io.mosip.packet.core.util.mockmds.StringHelper;
import io.mosip.packet.manager.util.mock.sbi.JwtUtility;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public abstract class SBIDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SBIDeviceHelper.class);	

//	private String purpose;
//	private String profileId;
//	private String deviceId;
//	private int deviceSubId;

	private String deviceType;
	private String deviceSubType;

	private DigitalId digitalId;
	private DeviceInfo deviceInfo;
//	private DiscoverDto discoverDto;
//	private DeviceInfoDto deviceInfoDto;
  //  private HashMap<String, String> statusMap = new HashMap<> ();
    //private HashMap<String, Long> delayMap = new HashMap<> ();
//    protected int qualityScore;
//    protected boolean isQualityScoreSet;
//    private boolean scoreFromIso = false;
//    private SBICaptureInfo captureInfo;

//	public abstract long initDevice ();
//	public abstract int deInitDevice ();
// /   public abstract int getLiveStream ();
//    public abstract int getBioCapture (boolean isUsedForAuthenication) throws Exception;

	public Environment env;

	public void initDeviceDetails() {
		setDigitalId (getDigitalId (getDeviceType (), getDeviceSubType ()));
		setDeviceInfo (getDeviceInfo (getDeviceType (), getDeviceSubType (), getDigitalId ()));
	}

	protected DigitalId getDigitalId(String deviceType, String deviceSubType) {
		DigitalId digitalId = null;
		String fileName = null;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			switch (deviceType)
			{
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FINGER:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SLAP:
							fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_FINGER_SLAP_DIGITALID_JSON);
							break;
					}
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FACE:
					if (deviceSubType.equalsIgnoreCase(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FACE))
						fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_FACE_DIGITALID_JSON);
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_IRIS:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_IRIS_DOUBLE:
							fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_IRIS_DOUBLE_DIGITALID_JSON);
							break;
					}
				break;
			}

			if (FileHelper.exists(fileName)) 
			{
				File file = new File(fileName);
				digitalId = objectMapper.readValue(file, DigitalId.class);
				if (digitalId != null)
				{
					digitalId.setDateTime(CryptoUtility.getTimestamp());
				}
				
				return digitalId;
			}	
		} catch (Exception ex) {
        	LOGGER.error("getDigitalId :: deviceType::" + deviceType + " :: deviceSubType::" + deviceSubType , ex);
		}
		return null;
	}
	
	protected DeviceInfo getDeviceInfo(String deviceType, String deviceSubType, DigitalId digitalId) {
		DeviceInfo deviceInfo = null;
		String fileName = null;
		String keyStoreFileName = null;
		String keyAlias = null;
		String keyPwd = null;
		FileInputStream inputStream = null;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			switch (deviceType)
			{
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FINGER:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SLAP:
							fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_FINGER_SLAP_DEVICEINFO_JSON);
							keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEYSTORE_FILE_NAME);
							keyAlias = env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEY_ALIAS);
							keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEYSTORE_PWD);
							break;
					}
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FACE:
					if (deviceSubType.equalsIgnoreCase(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FACE))
					{
						fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_FACE_DEVICEINFO_JSON);
						keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEYSTORE_FILE_NAME);
						keyAlias =  env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEY_ALIAS);
						keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEYSTORE_PWD);
					}
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_IRIS:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_IRIS_DOUBLE:
							fileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_IRIS_DOUBLE_DEVICEINFO_JSON);
							keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEYSTORE_FILE_NAME);
							keyAlias = env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEY_ALIAS);
							keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEYSTORE_PWD);
							break;
					}
				break;
			}

			if (FileHelper.exists(fileName) && FileHelper.exists(keyStoreFileName)) 
			{
				File jsonFile = new File(fileName);
			    File keyStoreFile = new File(keyStoreFileName);
			    KeyStore keystore = null;
			    if (keyStoreFile.exists())
			    {
			    	inputStream = new FileInputStream (keyStoreFile);
					keystore = loadKeyStore (inputStream, keyPwd);			    	
			    }
				
				PrivateKey key = (PrivateKey)keystore.getKey(keyAlias, keyPwd.toCharArray());

	            /* Get certificate of public key */
	            Certificate cert = keystore.getCertificate(keyAlias);

	            /* Here it prints the public key*/
	            //LOGGER.Info("Public Key:");
	            //LOGGER.Info(cert.getPublicKey());

	            /* Here it prints the private key*/
	            //LOGGER.Info("\nPrivate Key:");
	            //LOGGER.Info(key);
	            
				deviceInfo = objectMapper.readValue(jsonFile, DeviceInfo.class);
				if (deviceInfo != null)
				{
					deviceInfo.setDigitalId(getUnsignedDigitalId (digitalId, false));
					deviceInfo.setDeviceStatus(null);
					deviceInfo.setPurpose("Registration");
					deviceInfo.setCallbackId("http://" + env.getProperty(SBIConstant.SERVER_ADDRESS));
					deviceInfo.setDigitalId(getSignedDigitalId (deviceInfo.getDigitalId(), key, cert));
				}
        		return deviceInfo;
			}	
		} catch (Exception ex) {
        	LOGGER.error("getDeviceInfo :: deviceType::" + deviceType + " :: deviceSubType::" + deviceSubType , ex);
		}
		finally
		{
			try { // because close can throw an exception
		        if (inputStream != null) inputStream.close();
		    } catch (IOException ignored) {}
		}
		return null;
	}
	
	public String getSignBioMetricsDataDto(String deviceType, String deviceSubType, String currentBioData) {
		String signedBioMetricsDataDto = null;
		String keyStoreFileName = null;
		String keyAlias = null;
		String keyPwd = null;
		FileInputStream inputStream = null;
		
		try {
			switch (deviceType)
			{
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FINGER:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SLAP:
							keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEYSTORE_FILE_NAME);
							keyAlias = env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEY_ALIAS);
							keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_FINGER_SLAP_KEYSTORE_PWD);
							break;
					}
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_FACE:
					if (deviceSubType.equalsIgnoreCase(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FACE))
					{
						keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEYSTORE_FILE_NAME);
						keyAlias = env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEY_ALIAS);
						keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_FACE_KEYSTORE_PWD);
					}
				break;
				case SBIConstant.MOSIP_BIOMETRIC_TYPE_IRIS:
					switch (deviceSubType)
					{
						case SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_IRIS_DOUBLE:
							keyStoreFileName = FileHelper.getCanonicalPath () + env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEYSTORE_FILE_NAME);
							keyAlias = env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEY_ALIAS);
							keyPwd = env.getProperty(SBIConstant.MOSIP_STREAM_IRIS_DOUBLE_KEYSTORE_PWD);
							break;
					}
				break;
			}

			if (FileHelper.exists(keyStoreFileName)) 
			{
				File keyStoreFile = new File(keyStoreFileName);
			    KeyStore keystore = null;
			    if (keyStoreFile.exists())
			    {
			    	inputStream = new FileInputStream (keyStoreFile);
					keystore = loadKeyStore (inputStream, keyPwd);			    	
			    }
				
				PrivateKey key = (PrivateKey)keystore.getKey(keyAlias, keyPwd.toCharArray());

	            /* Get certificate of public key */
	            Certificate cert = keystore.getCertificate(keyAlias);

	            /* Here it prints the public key*/
	            //LOGGER.Info("Public Key:");
	            //LOGGER.Info(cert.getPublicKey());

	            /* Here it prints the private key*/
	            //LOGGER.Info("\nPrivate Key:");
	            //LOGGER.Info(key);
	            signedBioMetricsDataDto = JwtUtility.getJwt(currentBioData.getBytes("UTF-8"), key, (X509Certificate) cert);
    		
        		return signedBioMetricsDataDto ;
			}	
		} catch (Exception ex) {
        	LOGGER.error("getSignBioMetricsDataDto :: deviceType::" + deviceType + " :: deviceSubType::" + deviceSubType , ex);
		}
		finally
		{
			try { // because close can throw an exception
		        if (inputStream != null) inputStream.close();
		    } catch (IOException ignored) {}
		}		
		return null;
	}

	private String getUnsignedDigitalId (DigitalId digitalId, boolean isBase64URLEncoded)
    {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			if (isBase64URLEncoded)
			{
				return StringHelper.base64UrlEncode (objectMapper.writeValueAsString(digitalId));
			}
			else
			{
				return objectMapper.writeValueAsString(digitalId);
			}
		} catch (Exception ex) {
        	LOGGER.error("getUnsignedDigitalId :: " , ex);
		}
		return null;
    }

	private String getSignedDigitalId (String digitalId, PrivateKey privateKey, Certificate cert)
    {
		try {
			return JwtUtility.getJwt (digitalId.getBytes("UTF-8"), privateKey, (X509Certificate) cert);
		} catch (Exception ex) {
        	LOGGER.error("getSignedDigitalId :: " , ex);
		}
		return null;
    }
	
	private KeyStore loadKeyStore(FileInputStream inputStream, String keystorePwd) throws Exception {
	    KeyStore keyStore = KeyStore.getInstance("JKS");
        // if exists, load
        keyStore.load(inputStream, keystorePwd.toCharArray());

        /*
	    else {
	        // if not exists, create
	        keyStore.load(null, null);
	        keyStore.store(new FileOutputStream(file), keystorePwd.toCharArray());
	    }
	    */
	    return keyStore;
	}

}
