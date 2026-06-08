/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedMap;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

@PrototypeComponent
public class HttpClientAssertionRetrieval
		extends AbstractCredentialRetrieval<ClientAssertionExchange>
		implements CredentialRetrieval, JAXRSAuthentication
{
	public static final String NAME = "rest-jwt-client-assertion";
	public static final String DESC = "OAuth2 client JWT assertion retrieval";
	public static final String JWT_BEARER_TYPE =
			"urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

	private static final ResolvableError NOT_FOUND_ERROR =
			new ResolvableError("HttpClientAssertionRetrieval.assertionNotFound");
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, HttpClientAssertionRetrieval.class);

	public HttpClientAssertionRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	}

	@Override
	public AbstractPhaseInterceptor<Message> getInterceptor()
	{
		return null;
	}

	@Override
	public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
	{
		Message message = getCurrentCxfMessage();
		if (message == null)
			return LocalAuthenticationResult.failed(NOT_FOUND_ERROR, DenyReason.undefinedCredential);

		HttpServletRequest req = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (req == null)
			return LocalAuthenticationResult.failed(NOT_FOUND_ERROR, DenyReason.undefinedCredential);

		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> formParams =
				(MultivaluedMap<String, String>) message.get(FormUtils.FORM_PARAM_MAP);
		if (formParams == null)
		{
			log.trace("No form parameters in request");
			return LocalAuthenticationResult.failed(NOT_FOUND_ERROR, DenyReason.undefinedCredential);
		}

		String assertionType = decode(formParams.getFirst("client_assertion_type"));
		String assertion = decode(formParams.getFirst("client_assertion"));

		if (assertion == null || assertionType == null)
		{
			log.trace("No client_assertion in request");
			return LocalAuthenticationResult.failed(NOT_FOUND_ERROR, DenyReason.undefinedCredential);
		}

		if (!JWT_BEARER_TYPE.equals(assertionType))
		{
			log.debug("Unsupported client_assertion_type: {}", assertionType);
			return LocalAuthenticationResult.failed(NOT_FOUND_ERROR, DenyReason.undefinedCredential);
		}

		URI tokenEndpointUri = URI.create(req.getRequestURL().toString());
		log.trace("Found client_assertion, verifying");
		return credentialExchange.verifyClientAssertion(assertion, tokenEndpointUri);
	}

	protected Message getCurrentCxfMessage()
	{
		return PhaseInterceptorChain.getCurrentMessage();
	}

	private static String decode(String value)
	{
		if (value == null)
			return null;
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<HttpClientAssertionRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<HttpClientAssertionRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, ClientAssertionExchange.ID);
		}
	}
}
