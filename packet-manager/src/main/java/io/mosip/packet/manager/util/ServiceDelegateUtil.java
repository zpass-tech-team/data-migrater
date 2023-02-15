package io.mosip.packet.manager.util;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.manager.constants.RegistrationConstants;
import io.mosip.packet.manager.dto.RequestHTTPDTO;
import io.mosip.packet.manager.exception.ConnectionException;
import io.mosip.packet.manager.exception.RegBaseCheckedException;
import io.mosip.packet.manager.exception.RegistrationExceptionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class .it invokes with different classes to get the response
 * 
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
@Component("serviceDelegateUtil")
public class ServiceDelegateUtil {

	@Value("${mosip.hostname}")
	private String MOSIP_HOSTNAME_PLACEHOLDER;

	@Autowired
	private RestClientAuthAdvice restClientAuthAdvice;

	@Autowired
	private RestClientUtil restClientUtil;

	@Autowired
	private Environment environment;

	public boolean isNetworkAvailable() throws Exception {
		try {
			String healthCheckUrl = getEnvironmentProperty(RegistrationConstants.HEALTH_CHECK_URL);
			Assert.notNull(healthCheckUrl, "Property mosip.reg.healthcheck.url missing");
			String serviceUrl = prepareURLByHostName(healthCheckUrl);
			return restClientUtil.isConnectedToSyncServer(serviceUrl);
		} catch (Exception exception) {
			throw new Exception("No Internet Access");
		}
	}

	/**
	 * prepare and trigger POST request.
	 *
	 * @param serviceName
	 *            service to be invoked
	 * @param object
	 *            request type
	 * @param triggerPoint
	 *            system or user driven invocation
	 * @return Object requiredType of object response Body
	 * @throws RegBaseCheckedException
	 *             generalised exception with errorCode and errorMessage
	 * @throws ConnectionException
	 *             when client error, server error, access error
	 */
	public Object post(String serviceName, Object object, String triggerPoint) throws RegBaseCheckedException, ConnectionException {
		RequestHTTPDTO requestDto;
		Object responseBody = null;
		Map<String, Object> responseMap = null;

		try {
			requestDto = preparePOSTRequest(serviceName, object);
			requestDto.setAuthRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_REQUIRED)));
			requestDto.setAuthZHeader(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_HEADER));
			requestDto.setIsSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.SIGN_REQUIRED)));
			requestDto.setTriggerPoint(triggerPoint);
			requestDto.setRequestSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.REQUEST_SIGN_REQUIRED)));
			requestDto = restClientAuthAdvice.addAuthZToken(requestDto);
			responseMap = restClientUtil.invokeURL(requestDto);
		} catch (RestClientException e) {
			throw new ConnectionException(RegistrationExceptionConstants.ACCESS_ERROR.getErrorCode(),
					RegistrationExceptionConstants.ACCESS_ERROR.getErrorMessage(), e);
		}

		if (isResponseValid(responseMap, RegistrationConstants.REST_RESPONSE_BODY)) {
			responseBody = responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
		}

		return responseBody;
	}

	/**
	 * Prepare POST request.
	 *
	 * @param serviceName
	 *            service to be invoked
	 * @param object
	 *            request type
	 * @return RequestHTTPDTO requestHTTPDTO with required data
	 */
	private RequestHTTPDTO preparePOSTRequest(final String serviceName, final Object object) {
		//LOGGER.debug("Preparing post request for web-service");

		// DTO need to to be prepared
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();

		// prepare httpDTO except rquest type and uri build
		prepareRequest(requestHTTPDTO, serviceName, object);

		// URI creation
		setURI(requestHTTPDTO, null, getEnvironmentProperty(serviceName, RegistrationConstants.SERVICE_URL));

		// RequestType
		requestHTTPDTO.setClazz(Object.class);
		return requestHTTPDTO;
	}

	/**
	 * Sets the URI.
	 *
	 * @param requestHTTPDTO
	 *            the request HTTPDTO
	 * @param requestParams
	 *            the request params
	 * @param url
	 *            the url
	 */
	private void setURI(RequestHTTPDTO requestHTTPDTO, Map<String, String> requestParams, String url) {
		// BuildURIComponent
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);

		if (requestParams == null) {
			requestParams = new HashMap<>();
		}
		/** Adding "version" as requestparam for all the API calls to support upgrade */
		requestParams.put(RegistrationConstants.VERSION, RegistrationConstants.MANIFEST_VERSION);
		
		Set<String> set = requestParams.keySet();
		for (String queryParamName : set) {
			uriComponentsBuilder.queryParam(queryParamName, requestParams.get(queryParamName));

		}
		
		URI uri = uriComponentsBuilder.build().toUri();

		requestHTTPDTO.setUri(uri);
	}

	/**
	 * Setup of headers
	 * 
	 * @param httpHeaders
	 *            http headers
	 * @param headers
	 *            headers
	 */
	private void setHeaders(HttpHeaders httpHeaders, String headers) {
		if(headers == null || headers.trim().isEmpty())
			return;

		String[] header = headers.split(",");
		String[] headerValues = null;

			for (String subheader : header) {
				if(subheader.trim().isEmpty())
					continue;
					headerValues = subheader.split(":");
					if (headerValues[0].equalsIgnoreCase("timestamp")) {
						headerValues[1] = DateUtils.formatToISOString(LocalDateTime.now());
					} else if (headerValues[0].equalsIgnoreCase("Center-Machine-RefId")) {
						headerValues[1] = String
								.valueOf(getEnvironmentProperty(RegistrationConstants.CENTER_ID))
								.concat(RegistrationConstants.UNDER_SCORE).concat(String
										.valueOf(getEnvironmentProperty(RegistrationConstants.STATION_ID)));
					} else if (headerValues[0].equalsIgnoreCase("authorization")) {
						headerValues[1] = "auth";
					} else if (headerValues[0].equalsIgnoreCase("signature")) {
						headerValues[1] = "sign";
					}
					httpHeaders.add(headerValues[0], headerValues[1]);
			}
			httpHeaders.add("Cache-Control", "no-cache,max-age=0");
	}

	/**
	 * @param requestHTTPDTO
	 *            create requestedHTTPDTO
	 * @param serviceName
	 *            service name to be called
	 * @param requestBody
	 *            object to be included in HTTP entities
	 */
	private void prepareRequest(RequestHTTPDTO requestHTTPDTO, String serviceName, Object requestBody) {
		requestHTTPDTO.setHttpMethod(
				HttpMethod.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.HTTPMETHOD)));
		requestHTTPDTO.setHttpHeaders(new HttpHeaders());
		requestHTTPDTO.setRequestBody(requestBody);
		// Headers
		setHeaders(requestHTTPDTO.getHttpHeaders(), getEnvironmentProperty(serviceName, RegistrationConstants.HEADERS));
	}

	public String prepareURLByHostName(String url) {
		String mosipHostNameVal = getEnvironmentProperty(RegistrationConstants.MOSIP_HOSTNAME);
		Assert.notNull(mosipHostNameVal, "mosip.hostname is missing");
		return (url != null) ? url.replace(MOSIP_HOSTNAME_PLACEHOLDER, mosipHostNameVal)
				: url;
	}

	private String getEnvironmentProperty(String serviceName, String serviceComponent) {
		return environment.getProperty(serviceName.concat(RegistrationConstants.DOT).concat(serviceComponent));
	}

	private String getEnvironmentProperty(String property) {
		return environment.getProperty(property);
	}

	private boolean isResponseValid(Map<String, Object> responseMap, String key) {
		return !(null == responseMap || responseMap.isEmpty() || !responseMap.containsKey(key));
	}
}
