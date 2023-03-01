package io.mosip.packet.manager.util.mock.sbi.devicehelper.iris.binacular;


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
public class SBIIrisDoubleHelper extends SBIDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SBIIrisDoubleHelper.class);

	private SBIIrisDoubleHelper() {
	}

	private static SBIIrisDoubleHelper instance;

	public static SBIIrisDoubleHelper getInstance(Environment env) {
		if (instance == null)
		{
			synchronized (SBIIrisDoubleHelper.class) {
				instance = new SBIIrisDoubleHelper();
				instance.setDeviceType(SBIConstant.MOSIP_BIOMETRIC_TYPE_IRIS);
				instance.setDeviceSubType(SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_IRIS_DOUBLE);
				instance.setEnv(env);
				instance.initDeviceDetails();
			}
		}
		return instance;
	}
}
