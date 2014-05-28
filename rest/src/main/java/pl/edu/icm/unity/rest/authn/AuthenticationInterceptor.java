/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.rest.authn.ext.TLSRetrieval;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationProcessorUtil;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Performs a final authentication, basing on the endpoint's configuration.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationInterceptor extends AbstractPhaseInterceptor<Message>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, AuthenticationInterceptor.class);
	private UnityMessageSource msg;
	protected List<Map<String, BindingAuthn>> authenticators;
	protected UnsuccessfulAuthenticationCounter unsuccessfulAuthenticationCounter;
	protected SessionManagement sessionMan;
	protected AuthenticationRealm realm;
	
	public AuthenticationInterceptor(UnityMessageSource msg, List<Map<String, BindingAuthn>> authenticators,
			AuthenticationRealm realm, SessionManagement sessionManagement)
	{
		super(Phase.PRE_INVOKE);
		this.msg = msg;
		this.realm = realm;
		this.authenticators = authenticators;
		this.unsuccessfulAuthenticationCounter = new UnsuccessfulAuthenticationCounter(
				realm.getBlockAfterUnsuccessfulLogins(), realm.getBlockFor()*1000);
		this.sessionMan = sessionManagement;
	}

	@Override
	public void handleMessage(Message message) throws Fault
	{
		String ip = getClientIP();
		if (unsuccessfulAuthenticationCounter.getRemainingBlockedTime(ip) > 0)
		{
			log.info("Authentication blocked for client with IP " + ip);
			throw new Fault(new Exception("Too many invalid authentication attempts, try again later"));
		}
		
		Map<String, AuthenticationResult> authnCache = new HashMap<String, AuthenticationResult>();
		X509Certificate[] clientCert = TLSRetrieval.getTLSCertificates();
		IdentityTaV tlsId = (clientCert == null) ? null : new IdentityTaV(X500Identity.ID, 
				clientCert[0].getSubjectX500Principal().getName());
		InvocationContext ctx = new InvocationContext(tlsId, realm); 
		InvocationContext.setCurrent(ctx);
		AuthenticationException firstError = null;
		AuthenticatedEntity client = null;
		for (Map<String, BindingAuthn> authenticatorSet: authenticators)
		{
			try
			{
				client = processAuthnSet(authnCache, authenticatorSet);
			} catch (AuthenticationException e)
			{
				if (log.isDebugEnabled())
					log.debug("Authentication set failed to authenticate the client, " +
							"will try another: " + e);
				if (firstError == null)
					firstError = new AuthenticationException(msg.getMessage(e.getMessage()));
				continue;
			}
			break;
		}
		if (client == null)
		{
			log.info("Authentication failed for client");
			unsuccessfulAuthenticationCounter.unsuccessfulAttempt(ip);
			throw new Fault(firstError == null ? new Exception("Authentication failed") : firstError);
		} else
		{
			authnSuccess(client, ip, ctx);
		}
	}
	
	private void authnSuccess(AuthenticatedEntity client, String ip, InvocationContext ctx)
	{
		if (log.isDebugEnabled())
			log.debug("Client was successfully authenticated: [" + 
					client.getEntityId() + "] " + client.getAuthenticatedWith().toString());
		unsuccessfulAuthenticationCounter.successfulAttempt(ip);
		
		LoginSession ls = sessionMan.getCreateSession(client.getEntityId(), realm, 
				"", client.isUsedOutdatedCredential(), null);
		ctx.setLoginSession(ls);
		ctx.addAuthenticatedIdentities(client.getAuthenticatedWith());
	}
	
	private AuthenticatedEntity processAuthnSet(Map<String, AuthenticationResult> authnCache,
			Map<String, BindingAuthn> authenticatorSet) throws AuthenticationException
	{
		List<AuthenticationResult> setResult = new ArrayList<AuthenticationResult>();
		for (Map.Entry<String, BindingAuthn> authenticator: authenticatorSet.entrySet())
		{
			AuthenticationResult result = authnCache.get(authenticator.getKey());
			if (result == null)
			{
				CXFAuthentication myAuth = (CXFAuthentication) authenticator.getValue();
				result = myAuth.getAuthenticationResult();
				authnCache.put(authenticator.getKey(), result);
			}
			setResult.add(result);
		}
		return AuthenticationProcessorUtil.processResults(setResult);
	}
	
	private String getClientIP()
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		return request.getRemoteAddr();
	}
}
