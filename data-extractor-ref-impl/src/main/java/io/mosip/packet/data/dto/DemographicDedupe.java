package io.mosip.packet.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DemographicDedupe implements Serializable {

    private String regId;

    private String process;

    private Integer iteration;

    private String name;

    private String dob;

    private String gender;

    private String nrcId;

    private String phone;

    private String email;

    private String postalCode;

    private String langCode;

}
