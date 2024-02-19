
package io.mosip.packet.core.exception;

// TODO: Auto-generated Javadoc
/**
 * The Enum PRTPlatformErrorMessages.
 *
 * @author M1047487
 */
public enum PlatformErrorMessages {


	/** The PRT rct unknown resource exception. */
	PRT_RCT_UNKNOWN_RESOURCE_EXCEPTION(PlatformConstants.PRT_PRINT_PREFIX + "001", "Unknown resource provided"),
	/** The PRT bdd abis abort. */
	PRT_BDD_ABIS_ABORT(PlatformConstants.PRT_PRINT_PREFIX + "002",
			"ABIS for the Reference ID and Request ID was Abort"),
	MGR_PKT_CRT_IGNORE_EXCEPTION(PlatformConstants.PRT_PRINT_PREFIX + "003", "Packet Rejected for Creation due to %s poor quality");



	/** The error message. */
	private final String errorMessage; 

	/** The error code. */
	private final String errorCode;

	/**
	 * Instantiates a new platform error messages.
	 *
	 * @param errorCode
	 *            the error code
	 * @param errorMsg
	 *            the error msg
	 */
	private PlatformErrorMessages(String errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.errorMessage = errorMsg;
	}

	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getMessage() {
		return this.errorMessage;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getCode() {
		return this.errorCode;
	}

}