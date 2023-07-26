package io.mosip.packet.data.biosdk.bqat.constant;

public enum BQATModalityType {
    FINGER("fingerprint"),
    FACE("face"),
    IRIS("iris"),
    SPEECH("speech");

    public final String modality;

    BQATModalityType(String modality) {
        this.modality = modality;
    }

    public String getModality() {
        return modality;
    }
}
