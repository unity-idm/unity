/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;

/**
 * Base code for retrieving HTTP Bearer token data from CXF.
 * 
 * @author K. Benedyczak
 */
public abstract class BearerRetrievalBase extends AbstractCredentialRetrieval<AccessTokenExchange> implements CXFAuthentication
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, BearerRetrievalBase.class);
	
	public BearerRetrievalBase(String bindingName)
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
		BearerAccessToken authnToken = getTokenCredential(log);
		if (authnToken == null)
		{
			log.trace("No HTTP Bearer access token header was found");
			return LocalAuthenticationResult.failed(new ResolvableError("BearerRetrievalBase.tokenNotFound"), DenyReason.undefinedCredential);
		}
		log.trace("HTTP Bearer access token header found");
		try
		{
			return credentialExchange.checkToken(authnToken);
		} catch (Exception e)
		{
			log.debug("HTTP Bearer access token is invalid or its processing failed", e);
			return LocalAuthenticationResult.failed(e);
		}

	}
	
	protected BearerAccessToken getTokenCredential(Logger log)
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest req =(HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (req == null)
			return null; 
		String aa = req.getHeader("Authorization");
		if (aa == null)
			return null;
		
		try
		{
			return BearerAccessToken.parse(aa);
		} catch (ParseException e)
		{
			log.debug("Received HTTP authorization header, but it is not a valid Bearer access token: " +
					e);
			return null;
		}
	}
}
