/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.impl.util.Base64;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import eu.unicore.security.HTTPAuthNTokens;

/**
 * Base code for retrieving HTTP BASIC authn data from CXF.
 * 
 * @author K. Benedyczak
 */
public abstract class HttpBasicRetrievalBase implements CredentialRetrieval, CXFAuthentication
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, HttpBasicRetrievalBase.class);
	protected PasswordExchange credentialExchange;
	
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
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}
	
	@Override
	public AbstractPhaseInterceptor<Message> getInterceptor()
	{
		return null;
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		HTTPAuthNTokens authnTokens = getHTTPCredentials(log);
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
	
	protected HTTPAuthNTokens getHTTPCredentials(Logger log)
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
		if (!aa.startsWith("Basic "))
			return null;
		
		String encoded = aa.substring(6);
		String decoded = new String(Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII)),
				StandardCharsets.US_ASCII);
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
