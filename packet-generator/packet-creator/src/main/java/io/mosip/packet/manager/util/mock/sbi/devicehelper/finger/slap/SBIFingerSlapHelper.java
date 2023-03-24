package io.mosip.packet.manager.util.mock.sbi.devicehelper.finger.slap;

import org.slf4j.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.packet.core.constant.SBIConstant;
import org.slf4j.LoggerFactory;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.SBIDeviceHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
@Getter
@Setter
public class SBIFingerSlapHelper extends SBIDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SBIFingerSlapHelper.class);

	private SBIFingerSlapHelper() {
	}

	private static SBIFingerSlapHelper instance;

	//synchronized method to control simultaneous access
	public static SBIFingerSlapHelper getInstance(Environment env) {
		if (instance == null)
		{
			synchronized (SBIFingerSlapHelper.class) {
				instance = new SBIFingerSlapHelper();
				instance.setDeviceType(SBIConstant.MOSIP_BIOMETRIC_TYPE_FINGER);
				instance.setDeviceSubType(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SLAP);
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
