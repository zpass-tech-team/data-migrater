package io.mosip.packet.core.constant;

public enum LoginType {
    PMS("pms"),
    REGPROC("regproc"),
    REGISTRATION("reg"),
    IDREPO("idrepo"),
    USER("user");

    private String loginCode;

    LoginType(String loginCode) {
        this.loginCode=loginCode;
    }

    public String getLoginCode() {
        return loginCode;
    }
}
