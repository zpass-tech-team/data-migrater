package io.mosip.packet.manager.util.mock.sbi.devicehelper.face;

import java.util.Random;

import io.mosip.packet.core.constant.SBIConstant;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.SBIDeviceHelper;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.finger.slap.SBIFingerSlapHelper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SBIFaceHelper extends SBIDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SBIFaceHelper.class);

	private SBIFaceHelper() {
	}

	private static SBIFaceHelper instance;

	public static SBIFaceHelper getInstance(Environment env) {
		if (instance == null)
		{
			synchronized (SBIFaceHelper.class) {
				instance = new SBIFaceHelper();
				instance.setDeviceType(SBIConstant.MOSIP_BIOMETRIC_TYPE_FACE);
				instance.setDeviceSubType(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FACE);
				instance.setEnv(env);
				instance.initDeviceDetails();
			}
		}
		return instance;
	}

	@Override
	public void resetDevices() {
		instance=null;
	}
}