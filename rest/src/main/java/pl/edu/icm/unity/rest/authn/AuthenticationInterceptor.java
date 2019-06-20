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
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.authn.ext.TLSRetrieval;
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
	protected List<AuthenticationFlow> authenticators;
	protected UnsuccessfulAuthenticationCounter unsuccessfulAuthenticationCounter;
	protected SessionManagement sessionMan;
	protected AuthenticationRealm realm;
	protected Set<String> notProtectedPaths = new HashSet<String>();
	private Properties endpointProperties;
	
	public AuthenticationInterceptor(UnityMessageSource msg, AuthenticationProcessor authenticationProcessor, 
			List<AuthenticationFlow> authenticators,
			AuthenticationRealm realm, SessionManagement sessionManagement, Set<String> notProtectedPaths,
			Properties endpointProperties)
	{
		super(Phase.PRE_INVOKE);
		this.msg = msg;
		this.authenticationProcessor = authenticationProcessor;
		this.realm = realm;
		this.authenticators = authenticators;
		this.endpointProperties = endpointProperties;
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
		
		Map<String, AuthenticationResult> authnCache = new HashMap<>();
		X509Certificate[] clientCert = TLSRetrieval.getTLSCertificates();
		IdentityTaV tlsId = (clientCert == null) ? null : new IdentityTaV(X500Identity.ID, 
				clientCert[0].getSubjectX500Principal().getName());
		InvocationContext ctx = new InvocationContext(tlsId, realm, authenticators); 
		InvocationContext.setCurrent(ctx);
		AuthenticationException firstError = null;
		EntityWithAuthenticators client = null;
		
		if (isToNotProtected(message))
			return;
		
		for (AuthenticationFlow authenticatorFlow: authenticators)
		{
			try
			{
				client = processAuthnFlow(authnCache, authenticatorFlow);
			} catch (AuthenticationException e)
			{
				if (log.isDebugEnabled())
					log.debug("Authentication set failed to authenticate the client using flow "
							+ authenticatorFlow.getId() + ", "
							+ "will try another: " + e);
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
	
	private void authnSuccess(EntityWithAuthenticators client, String ip, InvocationContext ctx)
	{
		if (log.isDebugEnabled())
			log.debug("Client was successfully authenticated: [" + 
					client.entity.getEntityId() + "] " + client.entity.getAuthenticatedWith().toString());
		unsuccessfulAuthenticationCounter.successfulAttempt(ip);
		
		LoginSession ls = sessionMan.getCreateSession(client.entity.getEntityId(), realm, 
				"", client.entity.getOutdatedCredentialId(), new RememberMeInfo(false, false), 
				client.firstFactor, client.secondFactor);
		ctx.setLoginSession(ls);
		ls.addAuthenticatedIdentities(client.entity.getAuthenticatedWith());
		ls.setRemoteIdP(client.entity.getRemoteIdP());
	}
	
	private EntityWithAuthenticators processAuthnFlow(Map<String, AuthenticationResult> authnCache,
			AuthenticationFlow authenticationFlow) throws AuthenticationException
	{
		PartialAuthnState state = null;
		AuthenticationException firstError = null;
		for (AuthenticatorInstance authn : authenticationFlow.getFirstFactorAuthenticators())
		{
			try
			{
				AuthenticationResult result = processAuthenticator(authnCache,
						(CXFAuthentication) authn.getRetrieval());
				state = authenticationProcessor.processPrimaryAuthnResult(result,
						authenticationFlow, authn.getRetrieval().getAuthenticatorId());
			} catch (AuthenticationException e)
			{
				if (firstError == null)
					firstError = new AuthenticationException(e.getMessage());
				continue;
			}
			break;
		}

		if (state == null)
		{
			throw firstError == null
					? new AuthenticationException("Authentication failed")
					: firstError;
		}

		if (state.isSecondaryAuthenticationRequired())
		{
			CXFAuthentication secondFactorAuthn = (CXFAuthentication) state.getSecondaryAuthenticator();
			AuthenticationResult result2 = processAuthenticator(authnCache, secondFactorAuthn);
			AuthenticatedEntity entity = authenticationProcessor.finalizeAfterSecondaryAuthentication(state,
					result2);
			return new EntityWithAuthenticators(entity, state.getFirstFactorOptionId(), 
					secondFactorAuthn.getAuthenticatorId());			
		} else
		{
			AuthenticatedEntity entity = authenticationProcessor.finalizeAfterPrimaryAuthentication(state, false);
			return new EntityWithAuthenticators(entity, state.getFirstFactorOptionId(), null);
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
			result = myAuth.getAuthenticationResult(endpointProperties);
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
		return HTTPRequestContext.getCurrent().getClientIP();
	}
	
	private static class EntityWithAuthenticators
	{
		private final AuthenticatedEntity entity;
		private final String firstFactor;
		private final String secondFactor;

		EntityWithAuthenticators(AuthenticatedEntity entity, String firstFactor,
				String secondFactor)
		{
			this.entity = entity;
			this.firstFactor = firstFactor;
			this.secondFactor = secondFactor;
		}
	}
}
