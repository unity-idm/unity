/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;

/**
 * Retrieves JWT from authz HTTP header.
 * 
 * @author K. Benedyczak
 */
public abstract class JWTRetrievalBase extends AbstractCredentialRetrieval<JWTExchange> implements CXFAuthentication
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JWTRetrievalBase.class);
	
	public JWTRetrievalBase(String bindingName)
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
		String token = getToken();
		if (token == null)
			return new AuthenticationResult(Status.notApplicable, null);
		log.debug("JWT token found: " + token);
		try
		{
			return credentialExchange.checkJWT(token);
		} catch (Exception e)
		{
			log.debug("JWT credential validation failed", e);
			return new AuthenticationResult(Status.deny, null);
		}
	}

	protected String getToken()
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
		if (!aa.startsWith("Bearer "))
			return null;
		int firstDot = aa.indexOf('.');
		if (firstDot == -1)
			return null;
		int secDot = aa.indexOf('.', firstDot+1);
		if (secDot == -1)
			return null;
		if (aa.indexOf('.', secDot+1) != -1)
			return null;
		return aa.substring(7);
	}
}
