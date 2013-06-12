/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.impl.util.Base64;

import eu.unicore.security.HTTPAuthNTokens;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.ws.authn.CXFAuthentication;

/**
 * Credential retrieval using username and password from the HTTP Basic Authn.
 * 
 * @author K. Benedyczak
 */
public class HttpBasicRetrieval implements CredentialRetrieval, CXFAuthentication
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WS, HttpBasicRetrieval.class);
	private PasswordExchange credentialExchange;
	
	@Override
	public String getBindingName()
	{
		return CXFAuthentication.NAME;
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
	public AuthenticationResult getAuthenticationResult()
	{
		HTTPAuthNTokens authnTokens = getHTTPCredentials();
		if (authnTokens == null)
			return new AuthenticationResult(Status.notApplicable, null);
		try
		{
			AuthenticatedEntity authenticatedEntity = credentialExchange.checkPassword(
					authnTokens.getUserName(), authnTokens.getPasswd());
			return new AuthenticationResult(Status.success, authenticatedEntity);
		} catch (Exception e)
		{
			return new AuthenticationResult(Status.deny, null);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}
	
	protected HTTPAuthNTokens getHTTPCredentials()
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
		if (aa.length() < 7)
		{
			log.warn("Ignoring too short Authorization header element in " +
					"HTTP request: " + aa);
			return null;
		}
		String encoded = aa.substring(6);
		String decoded = new String(Base64.decode(encoded.getBytes()));
		String []split = decoded.split(":");
		if (split.length > 2)
		{
			log.warn("Ignoring malformed Authorization HTTP header element" +
					" (to many ':' after decode: " + decoded + ")");
			return null;
		}
		if (split.length == 2)
			return new HTTPAuthNTokens(split[0], split[1]);
		else if (split.length == 1)
			return new HTTPAuthNTokens(split[0], null);
		else
		{
			log.warn("Ignoring malformed Authorization HTTP header element" +
			" (empty string after decode)");
			return null;
		}
	}
}
