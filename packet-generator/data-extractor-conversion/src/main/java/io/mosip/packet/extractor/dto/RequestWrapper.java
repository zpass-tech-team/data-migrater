//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.mosip.packet.extractor.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class RequestWrapper<T> {
    private String id;
    private String version;
    @Schema(
            description = "Request Timestamp",
            example = "2018-12-10T06:12:52.994Z",
            required = true
    )
    private String requesttime;
    private Object metadata;
    @NotNull
    @Valid
    private T request;

    @Generated
    public RequestWrapper() {
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public String getVersion() {
        return this.version;
    }

    @Generated
    public String getRequesttime() {
        return this.requesttime;
    }

    @Generated
    public Object getMetadata() {
        return this.metadata;
    }

    @Generated
    public T getRequest() {
        return this.request;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setVersion(String version) {
        this.version = version;
    }

    @Generated
    public void setRequesttime(String requesttime) {
        this.requesttime = requesttime;
    }

    @Generated
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    @Generated
    public void setRequest(T request) {
        this.request = request;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RequestWrapper)) {
            return false;
        } else {
            RequestWrapper<?> other = (RequestWrapper)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label71: {
                    Object this$id = this.getId();
                    Object other$id = other.getId();
                    if (this$id == null) {
                        if (other$id == null) {
                            break label71;
                        }
                    } else if (this$id.equals(other$id)) {
                        break label71;
                    }

                    return false;
                }

                Object this$version = this.getVersion();
                Object other$version = other.getVersion();
                if (this$version == null) {
                    if (other$version != null) {
                        return false;
                    }
                } else if (!this$version.equals(other$version)) {
                    return false;
                }

                label57: {
                    Object this$requesttime = this.getRequesttime();
                    Object other$requesttime = other.getRequesttime();
                    if (this$requesttime == null) {
                        if (other$requesttime == null) {
                            break label57;
                        }
                    } else if (this$requesttime.equals(other$requesttime)) {
                        break label57;
                    }

                    return false;
                }

                Object this$metadata = this.getMetadata();
                Object other$metadata = other.getMetadata();
                if (this$metadata == null) {
                    if (other$metadata != null) {
                        return false;
                    }
                } else if (!this$metadata.equals(other$metadata)) {
                    return false;
                }

                Object this$request = this.getRequest();
                Object other$request = other.getRequest();
                if (this$request == null) {
                    if (other$request == null) {
                        return true;
                    }
                } else if (this$request.equals(other$request)) {
                    return true;
                }

                return false;
            }
        }
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof RequestWrapper;
    }

    @Generated
    public int hashCode() {
        //int PRIME = true;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : $version.hashCode());
        Object $requesttime = this.getRequesttime();
        result = result * 59 + ($requesttime == null ? 43 : $requesttime.hashCode());
        Object $metadata = this.getMetadata();
        result = result * 59 + ($metadata == null ? 43 : $metadata.hashCode());
        Object $request = this.getRequest();
        result = result * 59 + ($request == null ? 43 : $request.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        String var10000 = this.getId();
        return "RequestWrapper(id=" + var10000 + ", version=" + this.getVersion() + ", requesttime=" + this.getRequesttime() + ", metadata=" + this.getMetadata() + ", request=" + this.getRequest() + ")";
    }
}
