package io.mosip.packet.manager.util;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.packet.manager.constants.RegistrationConstants;
import io.mosip.packet.manager.dto.RequestHTTPDTO;
import io.mosip.packet.manager.exception.ConnectionException;
import io.mosip.packet.manager.exception.RegBaseCheckedException;
import io.mosip.packet.manager.exception.RegistrationExceptionConstants;
import io.mosip.packet.manager.dto.AuthTokenDTO;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;


/**
 * The Class RestClientAuthAdvice checks whether the invoking REST service
 * should required authentication. If required then the auth service is invoked
 * to get the token.
 *
 * @author Balaji Sridharan
 * @author Mahesh Kumar
 */
@Aspect
@Component
public class RestClientAuthAdvice {

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private AuthTokenUtilService authTokenUtilService;


    public RequestHTTPDTO addAuthZToken(RequestHTTPDTO requestHTTPDTO) throws RegBaseCheckedException, ConnectionException {

        try {
            if (requestHTTPDTO.isRequestSignRequired()) {
                addRequestSignature(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getRequestBody());
            }

            if (requestHTTPDTO.isAuthRequired()) {
                String authZToken = getAuthZToken(requestHTTPDTO);
                setAuthHeaders(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getAuthZHeader(), authZToken);
            }

            requestHTTPDTO.setHttpEntity(new HttpEntity<>(requestHTTPDTO.getRequestBody(), requestHTTPDTO.getHttpHeaders()));
            return requestHTTPDTO;

        } catch (RegBaseCheckedException regBaseCheckedException) {
            throw regBaseCheckedException;
        } catch (Throwable throwable) {
            throw new RegBaseCheckedException("UNKNOWN_ERROR", throwable.getMessage());
        }
    }

    private String getAuthZToken(RequestHTTPDTO requestHTTPDTO)
            throws RegBaseCheckedException {
        AuthTokenDTO authZToken = authTokenUtilService.getAuthToken();
        return authZToken.getCookie();
    }

    private void addRequestSignature(HttpHeaders httpHeaders, Object requestBody) throws RegBaseCheckedException {
        try {
            httpHeaders.add("request-signature", String.format("Authorization:%s", CryptoUtil
                    .encodeToURLSafeBase64(clientCryptoFacade.getClientSecurity().signData(JsonUtils.javaObjectToJsonString(requestBody).getBytes()))));
            httpHeaders.add(RegistrationConstants.KEY_INDEX, CryptoUtil.computeFingerPrint(
                    clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null));
        } catch (JsonProcessingException jsonProcessingException) {
            throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorCode(),
                    RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorMessage(),
                    jsonProcessingException);
        }
    }

    private void setAuthHeaders(HttpHeaders httpHeaders, String authHeader, String authZCookie) {
        String[] arrayAuthHeaders = null;
        if (authHeader != null) {
            arrayAuthHeaders = authHeader.split(":");
            if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.REST_OAUTH)) {
                httpHeaders.add(RegistrationConstants.COOKIE, authZCookie);
            } else if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.AUTH_TYPE)) {
                httpHeaders.add(arrayAuthHeaders[0], arrayAuthHeaders[1]);
            }
        }
    }
}
