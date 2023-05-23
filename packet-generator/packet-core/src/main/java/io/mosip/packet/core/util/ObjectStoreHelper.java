package io.mosip.packet.core.util;

import java.io.IOException;

import io.mosip.packet.core.constant.RegistrationExceptionConstants;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;

/**
 * @author Manoj SP
 *
 */
@Component
public class ObjectStoreHelper {
	@Value("${mosip.packet.objectstore.account-name}")
	private String objectStoreAccountName;

	@Value("${mosip.packet.objectstore.bucket-name}")
	private String objectStoreBucketName;

	@Value("${mosip.packet.objectstore.adapter-name}")
	private String objectStoreAdapterName;
	
	private ObjectStoreAdapter objectStore;

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}


	public boolean isObjectExists(String objectName) {
		return exists(objectName);
	}

	public byte[] getDemographicObject(String objectName) throws Exception {
		if (!this.isObjectExists(objectName)) {
			throw new Exception(RegistrationExceptionConstants.FILE_NOT_FOUND.getErrorCode() +
					RegistrationExceptionConstants.FILE_NOT_FOUND.getErrorMessage());
		}
		return getObject(objectName);
	}

	public byte[] getBiometricObject(String objectName) throws Exception {
		if (!this.isObjectExists(objectName)) {
			throw new Exception(RegistrationExceptionConstants.FILE_NOT_FOUND.getErrorCode() +
					RegistrationExceptionConstants.FILE_NOT_FOUND.getErrorMessage());
		}
		return getObject(objectName);
	}

	private boolean exists(String objectName) {
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	private byte[] getObject(String objectName) throws Exception {
		try {
		return IOUtils.toByteArray(
				objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName));
		} catch (AmazonS3Exception | FSAdapterException | IOException e) {
			throw new Exception(RegistrationExceptionConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode() +
					RegistrationExceptionConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e);
		}
	}
}
