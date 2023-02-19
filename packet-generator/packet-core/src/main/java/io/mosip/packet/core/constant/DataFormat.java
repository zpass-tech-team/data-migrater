package io.mosip.packet.core.constant;

public enum DataFormat {
    JPEG("jpg", "jpg"),
    JP2("jpeg 2000", "jp2"),
    WSQ("wsq","wsq"),
    ISO("ISO", "iso"),
    YMD("yyyy/mm/dd", "yyyy/mm/dd");


    public final String format;
    public final String fileFormat;

    DataFormat(final String format, final String fileFormat) {
        this.format = format;
        this.fileFormat = fileFormat;
    }

    public String getFormat() {
        return format;
    }

    public String getFileFormat() {
        return fileFormat;
    }
}
