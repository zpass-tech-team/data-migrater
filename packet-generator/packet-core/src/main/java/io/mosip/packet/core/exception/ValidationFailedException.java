package io.mosip.packet.core.exception;
	

/**
 * The Class ApisResourceAccessException.
 * 
 * @author M1049387
 */
public class ValidationFailedException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new apis resource access exception.
	 */
	public ValidationFailedException() {
		super();
	}

	/**
	 * Instantiates a new apis resource access exception.
	 *
	 * @param message the message
	 */
	public ValidationFailedException(String message) {
		super(PlatformErrorMessages.PRT_RCT_UNKNOWN_RESOURCE_EXCEPTION.getCode(), message);
	}

	public ValidationFailedException(String code, String message) {
		super(code, message);
	}

	/**
	 * Instantiates a new apis resource access exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public ValidationFailedException(String message, Throwable cause) {
		super(PlatformErrorMessages.PRT_RCT_UNKNOWN_RESOURCE_EXCEPTION.getCode(), message, cause);
	}

    public ValidationFailedException(String code, String message, Exception e) {
    }
}