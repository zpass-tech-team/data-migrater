package io.mosip.packet.data.biosdk.bqat.constant;

public enum BQATFileType {
    JPEQ("jpeg"),
    JPG("jpg"),
    PNG("png"),
    BMP("bmp"),
    WSQ("wsq"),
    JP2("jp2"),
    WAV("wav");

    public final String fileType;

    BQATFileType(String fileType) {
        this.fileType=fileType;
    }

    public String getType() {
        return fileType;
    }
}
