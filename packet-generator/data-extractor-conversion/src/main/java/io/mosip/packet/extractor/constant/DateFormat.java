package io.mosip.packet.extractor.constant;

import lombok.Getter;

@Getter
public enum DateFormat {
    DMY_WITH_HYPEN("dd-MM-yyyy"),
    YMD_WITH_HYPEN("yyyy-MM-dd");

    private final String format;

    DateFormat(String format) {
       this.format = format;
    }
}
