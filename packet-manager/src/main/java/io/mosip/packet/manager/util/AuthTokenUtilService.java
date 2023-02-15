package io.mosip.packet.manager.util;

import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.manager.constants.RegistrationConstants;
import io.mosip.packet.manager.dto.AuthTokenDTO;
import io.mosip.packet.manager.dto.RequestHTTPDTO;
import io.mosip.packet.manager.exception.RegBaseCheckedException;
import io.mosip.packet.manager.exception.RegistrationExceptionConstants;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static io.mosip.packet.manager.constants.RegistrationConstants.*;


/**
 * @author Anusha Sunkada
 * @since 1.1.3
 */
@Service
public class AuthTokenUtilService {

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private RestClientUtil restClientUtil;

    @Autowired
    private Environment environment;

    @Autowired
    private ServiceDelegateUtil serviceDelegateUtil;


    public AuthTokenDTO getAuthToken() throws RegBaseCheckedException {

        try {
            String timestamp = DateUtils.formatToISOString(LocalDateTime.now(ZoneOffset.UTC));
            String header = String.format("{\"kid\" : \"%s\"}", CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getSigningPublicPart(), null));
            String payload = String.format("{\"userId\" : \"%s\", \"password\": \"%s\", \"authType\":\"%s\", \"timestamp\" : \"%s\"}",
                    getEnvironmentProperty(AUTH_USERNAME), getEnvironmentProperty(AUTH_PASSWORD), AUTH_TYPE_NEW, timestamp);

            byte[] signature = clientCryptoFacade.getClientSecurity().signData(payload.getBytes());
            String data = String.format("%s.%s.%s", CryptoUtil.encodeToURLSafeBase64(header.getBytes()),
                    CryptoUtil.encodeToURLSafeBase64(payload.getBytes()), CryptoUtil.encodeToURLSafeBase64(signature));

            RequestHTTPDTO requestHTTPDTO = getRequestHTTPDTO(data, timestamp);
            setTimeout(requestHTTPDTO);
            setURI(requestHTTPDTO, new HashMap<>(), getEnvironmentProperty("auth_by_password", RegistrationConstants.SERVICE_URL));
            Map<String, Object> responseMap = restClientUtil.invokeForToken(requestHTTPDTO);

            JSONObject jsonObject = getAuthTokenResponse(responseMap);
            AuthTokenDTO authTokenDTO = new AuthTokenDTO();
            authTokenDTO.setCookie(String.format("Authorization=%s", jsonObject.getString("token")));
            authTokenDTO.setLoginMode("PASSWORD");

            return authTokenDTO;

        } catch (DataIntegrityViolationException e) {
            throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTH_TOKEN_SAVE_FAILED.getErrorCode(),
                    RegistrationExceptionConstants.AUTH_TOKEN_SAVE_FAILED.getErrorMessage());
        } catch (Exception exception) {
            throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                    exception.getMessage(), exception);
        }
    }

    @Timed
    private JSONObject getAuthTokenResponse(Map<String, Object> responseMap) throws RegBaseCheckedException {
        if (responseMap.get(RegistrationConstants.REST_RESPONSE_BODY) != null) {
            Map<String, Object> respBody = (Map<String, Object>) responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
            if (respBody.get("response") != null) {
                byte[] decryptedData = clientCryptoFacade.decrypt(CryptoUtil.decodeURLSafeBase64((String) respBody.get("response")));
                return new JSONObject(new String(decryptedData));
            }

            if (respBody.get("errors") != null) {
                List<LinkedHashMap<String, Object>> errorMap = (List<LinkedHashMap<String, Object>>) respBody
                        .get(RegistrationConstants.ERRORS);
                if (!errorMap.isEmpty()) {
                    throw new RegBaseCheckedException((String) errorMap.get(0).get("errorCode"),
                            (String) errorMap.get(0).get("message"));
                }
            }
        }
        throw new RegBaseCheckedException(
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
    }

    private RequestHTTPDTO getRequestHTTPDTO(String data, String timestamp) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("id", "");
        requestBody.put("version", "");
        requestBody.put("request", data);
        requestBody.put("requesttime", timestamp);

        RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
        requestHTTPDTO.setClazz(Object.class);
        requestHTTPDTO.setRequestBody(requestBody);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestHTTPDTO.setHttpHeaders(headers);
        requestHTTPDTO.setIsSignRequired(false);
        requestHTTPDTO.setRequestSignRequired(false);
        requestHTTPDTO.setHttpMethod(HttpMethod.POST);
        return requestHTTPDTO;
    }

    private void setTimeout(RequestHTTPDTO requestHTTPDTO) {
        // Timeout in milli second
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(
                Integer.parseInt(getEnvironmentProperty(RegistrationConstants.HTTP_API_READ_TIMEOUT)));
        requestFactory.setConnectTimeout(
                Integer.parseInt(getEnvironmentProperty(RegistrationConstants.HTTP_API_WRITE_TIMEOUT)));
        requestHTTPDTO.setSimpleClientHttpRequestFactory(requestFactory);
    }

    private void setURI(RequestHTTPDTO requestHTTPDTO, Map<String, String> requestParams, String url) {
        url = serviceDelegateUtil.prepareURLByHostName(url);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
        if (requestParams != null) {
            Set<String> set = requestParams.keySet();
            for (String queryParamName : set) {
                uriComponentsBuilder.queryParam(queryParamName, requestParams.get(queryParamName));
            }
        }
        URI uri = uriComponentsBuilder.build().toUri();
        requestHTTPDTO.setUri(uri);
    }

    private String getEnvironmentProperty(String property) {
        return environment.getProperty(property);
    }

    private String getEnvironmentProperty(String serviceName, String serviceComponent) {
        return environment.getProperty(serviceName.concat(RegistrationConstants.DOT).concat(serviceComponent));
    }
}
