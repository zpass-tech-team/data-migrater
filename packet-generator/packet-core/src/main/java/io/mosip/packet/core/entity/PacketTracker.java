package io.mosip.packet.core.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Timestamp;

@Entity
@Table(schema = "mosip", name = "packet_tracker")
public class PacketTracker implements Serializable {

    private static final long serialVersionUID = 5759550775532067590L;

    @Id
    @Column(name = "ref_id")
    private String refId;

    @Column(name = "SESSION_KEY")
    private String sessionKey;

    @Column(name = "reg_no")
    private String regNo;

    @Column(name = "ACTIVITY")
    private String activity;

    @Column(name = "status")
    private String status;

    @Column(name = "PROCESS")
    private String process;

    @Column(name = "REQUEST")
    @Lob
    private Blob request;

    @Column(name = "CR_BY")
    protected String crBy;

    @Column(name = "CR_DTIMES")
    protected Timestamp crDtime;

    @Column(name = "UPD_BY")
    protected String updBy;

    @Column(name = "UPD_DTIMES")
    protected Timestamp updDtimes;

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCrBy() {
        return crBy;
    }

    public void setCrBy(String crBy) {
        this.crBy = crBy;
    }

    public Timestamp getCrDtime() {
        return crDtime;
    }

    public void setCrDtime(Timestamp crDtime) {
        this.crDtime = crDtime;
    }

    public String getUpdBy() {
        return updBy;
    }

    public void setUpdBy(String updBy) {
        this.updBy = updBy;
    }

    public Timestamp getUpdDtimes() {
        return updDtimes;
    }

    public void setUpdDtimes(Timestamp updDtimes) {
        this.updDtimes = updDtimes;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public Blob getRequest() {
        return request;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public void setRequest(Blob request) {
        this.request = request;
    }
}
