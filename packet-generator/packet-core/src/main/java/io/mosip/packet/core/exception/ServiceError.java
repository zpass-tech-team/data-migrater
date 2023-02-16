//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.mosip.packet.core.exception;

import lombok.Generated;

public class ServiceError {
    private String errorCode;
    private String message;

    public ServiceError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.message = errorMessage;
    }

    public ServiceError() {
    }

    @Generated
    public String getErrorCode() {
        return this.errorCode;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Generated
    public void setMessage(String message) {
        this.message = message;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ServiceError)) {
            return false;
        } else {
            ServiceError other = (ServiceError)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$errorCode = this.getErrorCode();
                Object other$errorCode = other.getErrorCode();
                if (this$errorCode == null) {
                    if (other$errorCode != null) {
                        return false;
                    }
                } else if (!this$errorCode.equals(other$errorCode)) {
                    return false;
                }

                Object this$message = this.getMessage();
                Object other$message = other.getMessage();
                if (this$message == null) {
                    if (other$message != null) {
                        return false;
                    }
                } else if (!this$message.equals(other$message)) {
                    return false;
                }

                return true;
            }
        }
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ServiceError;
    }

    @Generated
    public int hashCode() {
       // int PRIME = true;
        int result = 1;
        Object $errorCode = this.getErrorCode();
        result = result * 59 + ($errorCode == null ? 43 : $errorCode.hashCode());
        Object $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        String var10000 = this.getErrorCode();
        return "ServiceError(errorCode=" + var10000 + ", message=" + this.getMessage() + ")";
    }
}
