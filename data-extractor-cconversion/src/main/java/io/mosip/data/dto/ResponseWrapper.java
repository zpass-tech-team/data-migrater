//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.mosip.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.mosip.data.exception.ServiceError;
import lombok.Generated;

public class ResponseWrapper<T> {
    private String id;
    private String version;
    @JsonFormat(
            shape = Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    private LocalDateTime responsetime = LocalDateTime.now(ZoneId.of("UTC"));
    private Object metadata;
    @NotNull
    @Valid
    private T response;
    private List<ServiceError> errors = new ArrayList();

    @Generated
    public ResponseWrapper() {
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
    public LocalDateTime getResponsetime() {
        return this.responsetime;
    }

    @Generated
    public Object getMetadata() {
        return this.metadata;
    }

    @Generated
    public T getResponse() {
        return this.response;
    }

    @Generated
    public List<ServiceError> getErrors() {
        return this.errors;
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
    public void setResponsetime(LocalDateTime responsetime) {
        this.responsetime = responsetime;
    }

    @Generated
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    @Generated
    public void setResponse(T response) {
        this.response = response;
    }

    @Generated
    public void setErrors(List<ServiceError> errors) {
        this.errors = errors;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ResponseWrapper)) {
            return false;
        } else {
            ResponseWrapper<?> other = (ResponseWrapper)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$id = this.getId();
                Object other$id = other.getId();
                if (this$id == null) {
                    if (other$id != null) {
                        return false;
                    }
                } else if (!this$id.equals(other$id)) {
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

                Object this$responsetime = this.getResponsetime();
                Object other$responsetime = other.getResponsetime();
                if (this$responsetime == null) {
                    if (other$responsetime != null) {
                        return false;
                    }
                } else if (!this$responsetime.equals(other$responsetime)) {
                    return false;
                }

                label62: {
                    Object this$metadata = this.getMetadata();
                    Object other$metadata = other.getMetadata();
                    if (this$metadata == null) {
                        if (other$metadata == null) {
                            break label62;
                        }
                    } else if (this$metadata.equals(other$metadata)) {
                        break label62;
                    }

                    return false;
                }

                label55: {
                    Object this$response = this.getResponse();
                    Object other$response = other.getResponse();
                    if (this$response == null) {
                        if (other$response == null) {
                            break label55;
                        }
                    } else if (this$response.equals(other$response)) {
                        break label55;
                    }

                    return false;
                }

                Object this$errors = this.getErrors();
                Object other$errors = other.getErrors();
                if (this$errors == null) {
                    if (other$errors != null) {
                        return false;
                    }
                } else if (!this$errors.equals(other$errors)) {
                    return false;
                }

                return true;
            }
        }
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ResponseWrapper;
    }

    @Generated
    public int hashCode() {
      //  int PRIME = true;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : $version.hashCode());
        Object $responsetime = this.getResponsetime();
        result = result * 59 + ($responsetime == null ? 43 : $responsetime.hashCode());
        Object $metadata = this.getMetadata();
        result = result * 59 + ($metadata == null ? 43 : $metadata.hashCode());
        Object $response = this.getResponse();
        result = result * 59 + ($response == null ? 43 : $response.hashCode());
        Object $errors = this.getErrors();
        result = result * 59 + ($errors == null ? 43 : $errors.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        String var10000 = this.getId();
        return "ResponseWrapper(id=" + var10000 + ", version=" + this.getVersion() + ", responsetime=" + this.getResponsetime() + ", metadata=" + this.getMetadata() + ", response=" + this.getResponse() + ", errors=" + this.getErrors() + ")";
    }
}
