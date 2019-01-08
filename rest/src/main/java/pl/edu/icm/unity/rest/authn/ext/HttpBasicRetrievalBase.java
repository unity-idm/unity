/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.HTTPAuthNTokens;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

/**
 * Base code for retrieving HTTP BASIC authn data from CXF.
 * 
 * @author K. Benedyczak
 */
public abstract class HttpBasicRetrievalBase extends AbstractCredentialRetrieval<PasswordExchange> 
		implements CredentialRetrieval, CXFAuthentication
{
	/**
	 * When this feature is set on the endpoint then the username and password are supposed to be URL encoded
	 */
	public static String FEATURE_HTTP_BASIC_URLENCODED = "HTTP_BASIC_URLENCODED";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, HttpBasicRetrievalBase.class);
	
	public HttpBasicRetrievalBase(String bindingName)
	{
		super(bindingName);
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
		HTTPAuthNTokens authnTokens = getHTTPCredentials(log, isUrlEncoded(endpointFeatures));
		if (authnTokens == null)
		{
			log.trace("No HTTP BASIC auth header was found");
			return new AuthenticationResult(Status.notApplicable, null);
		}
		log.trace("HTTP BASIC auth header found");
		try
		{
			return credentialExchange.checkPassword(authnTokens.getUserName(), authnTokens.getPasswd(),
					null);
		} catch (Exception e)
		{
			log.trace("HTTP BASIC credential is invalid");
			return new AuthenticationResult(Status.deny, null);
		}

	}
	
	private static HTTPAuthNTokens getHTTPCredentials(Logger log, boolean urlEncoded)
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest req =(HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (req == null)
			return null; 
		String authorizationHeader = req.getHeader("Authorization");
		return HttpBasicParser.getHTTPCredentials(authorizationHeader, log, urlEncoded);
	}
	
	private boolean isUrlEncoded(Properties endpointFeatures)
	{
		return endpointFeatures.containsKey(FEATURE_HTTP_BASIC_URLENCODED);
	}
	
}
