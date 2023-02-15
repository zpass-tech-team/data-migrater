package io.mosip.packet.manager.util;

import io.mosip.packet.manager.constants.RegistrationConstants;
import io.mosip.packet.manager.dto.RequestHTTPDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is a general method which gives the response for all httpmethod
 * designators.
 *
 * @author Yaswanth S
 * @since 1.0.0
 */
@Service
public class RestClientUtil {

    /**
     * Rest Template is a interaction with HTTP servers and enforces RESTful systems
     */
    private static final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    @Autowired
    private RestTemplate plainRestTemplate;

    @Autowired
    private Environment environment;

    /**
     * Access resource using restTemplate {@link RestTemplate}
     * Note: restTemplate is synchronous client
     *
     * @param requestHTTPDTO
     * @return
     * @throws RestClientException
     */
    public Map<String, Object> invokeURL(RequestHTTPDTO requestHTTPDTO) throws RestClientException {
        Map<String, Object> responseMap = null;

        plainRestTemplate.setRequestFactory(getHttpRequestFactory());
        ResponseEntity<?> responseEntity = plainRestTemplate.exchange(requestHTTPDTO.getUri(), requestHTTPDTO.getHttpMethod(),
                requestHTTPDTO.getHttpEntity(), requestHTTPDTO.getClazz());

        if (responseEntity != null && responseEntity.hasBody()) {
            responseMap = new LinkedHashMap<>();
            responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, responseEntity.getBody());
            responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, responseEntity.getHeaders());
        }
        return responseMap;
    }


    public Map<String, Object> invokeForToken(RequestHTTPDTO requestHTTPDTO)
            throws RestClientException {
        Map<String, Object> responseMap = null;
        requestHTTPDTO.setHttpEntity(new HttpEntity<>(requestHTTPDTO.getRequestBody(), requestHTTPDTO.getHttpHeaders()));
        plainRestTemplate.setRequestFactory(getHttpRequestFactory());
        ResponseEntity<?> responseEntity = plainRestTemplate.exchange(requestHTTPDTO.getUri(), requestHTTPDTO.getHttpMethod(),
                requestHTTPDTO.getHttpEntity(), requestHTTPDTO.getClazz());

        if (responseEntity != null && responseEntity.hasBody()) {
            responseMap = new LinkedHashMap<>();
            responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, responseEntity.getBody());
            responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, responseEntity.getHeaders());
        }
        return responseMap;
    }

    public SimpleClientHttpRequestFactory getHttpRequestFactory() {
        requestFactory.setReadTimeout(
                Integer.parseInt(getEnvironmentProperty(RegistrationConstants.HTTP_API_READ_TIMEOUT)));
        requestFactory.setConnectTimeout(
                Integer.parseInt(getEnvironmentProperty(RegistrationConstants.HTTP_API_WRITE_TIMEOUT)));
        return requestFactory;
    }

    public boolean isConnectedToSyncServer(String serviceUrl) throws MalformedURLException, URISyntaxException {
        plainRestTemplate.setRequestFactory(getHttpRequestFactory());
        ResponseEntity responseEntity = plainRestTemplate.getForEntity(new URL(serviceUrl).toURI(), String.class);
        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    private String getEnvironmentProperty(String property) {
        return environment.getProperty(property);
    }
}
