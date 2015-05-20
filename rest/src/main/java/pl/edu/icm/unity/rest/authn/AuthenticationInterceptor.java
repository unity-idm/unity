/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Performs a final authentication, basing on the endpoint's configuration.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationInterceptor extends AbstractPhaseInterceptor<Message>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, AuthenticationInterceptor.class);
	private UnityMessageSource msg;
	private AuthenticationProcessor authenticationProcessor;
	protected List<AuthenticationOption> authenticators;
	protected UnsuccessfulAuthenticationCounter unsuccessfulAuthenticationCounter;
	protected SessionManagement sessionMan;
	protected AuthenticationRealm realm;
	protected Set<String> notProtectedPaths = new HashSet<String>();
	
	public AuthenticationInterceptor(UnityMessageSource msg, AuthenticationProcessor authenticationProcessor, 
			List<AuthenticationOption> authenticators,
			AuthenticationRealm realm, SessionManagement sessionManagement, Set<String> notProtectedPaths)
	{
		super(Phase.PRE_INVOKE);
		this.msg = msg;
		this.authenticationProcessor = authenticationProcessor;
		this.realm = realm;
		this.authenticators = authenticators;
		this.unsuccessfulAuthenticationCounter = new UnsuccessfulAuthenticationCounter(
				realm.getBlockAfterUnsuccessfulLogins(), realm.getBlockFor()*1000);
		this.sessionMan = sessionManagement;
		this.notProtectedPaths.addAll(notProtectedPaths);
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
		
		if (isToNotProtected(message))
			return;
		
		for (AuthenticationOption authenticatorSet: authenticators)
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
	
	private boolean isToNotProtected(Message message)
	{
		try
		{
			String addressPath = (String) message.get(Message.REQUEST_URI);
			for (String notProtected: notProtectedPaths)
				if (addressPath.equals(notProtected))
				{
					log.debug("Request to a not protected address - " + addressPath 
							+ " - invocation will proceed without authentication");
					return true;
				}
		} catch (Exception e)
		{
			log.error("Can not establish the destination address", e);
		}
		return false;
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
		ls.addAuthenticatedIdentities(client.getAuthenticatedWith());
	}
	
	private AuthenticatedEntity processAuthnSet(Map<String, AuthenticationResult> authnCache,
			AuthenticationOption authenticationOption) throws AuthenticationException
	{
		AuthenticationResult result = processAuthenticator(authnCache, 
					(CXFAuthentication) authenticationOption.getPrimaryAuthenticator());
		
		PartialAuthnState state = authenticationProcessor.processPrimaryAuthnResult(result, 
				authenticationOption);
		if (state.isSecondaryAuthenticationRequired())
		{
			AuthenticationResult result2 = processAuthenticator(authnCache, 
					(CXFAuthentication) state.getSecondaryAuthenticator()); 
			return authenticationProcessor.finalizeAfterSecondaryAuthentication(state, result2);
		} else
		{
			return authenticationProcessor.finalizeAfterPrimaryAuthentication(state);
		}
	}

	private AuthenticationResult processAuthenticator(Map<String, AuthenticationResult> authnCache,
			CXFAuthentication myAuth) throws AuthenticationException
	{
		String authId = myAuth.getAuthenticatorId();
		AuthenticationResult result = authnCache.get(authId);
		if (result == null)
		{
			log.trace("Processing authenticator " + authId);
			result = myAuth.getAuthenticationResult();
			authnCache.put(authId, result);
			log.trace("Authenticator " + authId + " returned " + result);
		} else
		{
			log.trace("Using cached result of " + authId + ": " + result);
		}
		return result;
	}
	
	private String getClientIP()
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		return request.getRemoteAddr();
	}
}
