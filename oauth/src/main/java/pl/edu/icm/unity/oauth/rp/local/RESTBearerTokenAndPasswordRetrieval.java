/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.security.HTTPAuthNTokens;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicParser;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;

@PrototypeComponent
public class RESTBearerTokenAndPasswordRetrieval extends AbstractCredentialRetrieval<AccessTokenAndPasswordExchange>
		implements CXFAuthentication
{
	public static final String NAME = "rest-oauth-bearer-password";
	public static final String DESC = "RESTBearerTokenAndPasswordRetrieval.desc";

	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTBearerTokenAndPasswordRetrieval.class);

	public RESTBearerTokenAndPasswordRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String config)
	{

	}

	@Override
	public Interceptor<? extends Message> getInterceptor()
	{
		return null;
	}

	@Override
	public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
	{
		Map<String, String> httpCredentialsHeader = getHttpCredentials();

		BearerAccessToken authnToken = getTokenCredential(httpCredentialsHeader.get("Bearer"));
		if (authnToken == null)
		{
			log.debug("No HTTP Bearer access token header was found");
			return LocalAuthenticationResult.failed(new ResolvableError("BearerRetrievalBase.tokenNotFound"),
					DenyReason.undefinedCredential);
		}
		log.trace("HTTP Bearer access token header found");

		HTTPAuthNTokens httpCredentials;
		try
		{
			httpCredentials = HttpBasicParser.getHTTPCredentials(httpCredentialsHeader.get("Basic"), log,
					HttpBasicRetrievalBase.isUrlEncoded(endpointFeatures));
		} catch (Exception e)
		{
			log.debug("Invalid HTTP BASIC auth header was found");
			return LocalAuthenticationResult.failed(new ResolvableError("RESTBearerTokenAndPasswordRetrieval.invalidBasicAuth"));
		}

		if (httpCredentials == null)
		{
			log.debug("No HTTP BASIC auth header was found");
			return LocalAuthenticationResult.failed(new ResolvableError("RESTBearerTokenAndPasswordRetrieval.basicAuthNotFound"),
					DenyReason.undefinedCredential);
		}
		log.trace("HTTP BASIC auth header found");

		try
		{
			return credentialExchange.checkTokenAndPassword(authnToken, httpCredentials.getUserName(),
					httpCredentials.getPasswd());
		} catch (AuthenticationException e)
		{
			return LocalAuthenticationResult.failed(e);
		}

	}
	
	Map<String, String> getHttpCredentials()
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest req = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (req == null)
			return null;
		String aa = req.getHeader("Authorization");
		if (aa == null)
			return Collections.emptyMap();

		Map<String, String> creds = new HashMap<>();
		for (String cred : aa.split(","))
		{
			String[] splitedCred = cred.split(" ");
			creds.put(splitedCred[0], cred);
		}

		return creds;
	}

	public BearerAccessToken getTokenCredential(String token)
	{
		try
		{
			return BearerAccessToken.parse(token);
		} catch (ParseException e)
		{
			log.debug("Received HTTP authorization header, but it is not a valid Bearer access token: " + e);
			return null;
		}
	}

	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<RESTBearerTokenAndPasswordRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<RESTBearerTokenAndPasswordRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, AccessTokenAndPasswordExchange.ID);
		}
	}

}
