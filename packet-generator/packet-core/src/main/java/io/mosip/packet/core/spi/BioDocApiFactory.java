package io.mosip.packet.core.spi;

public interface BioDocApiFactory {
    public byte[] getBioData(byte[] byteval, String fieldName);
    public byte[] getDocData(byte[] byteval, String fieldName);
}
