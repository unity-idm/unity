/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import static pl.edu.icm.unity.base.authn.AuthenticationOptionKey.authenticatorOnlyKey;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.MDC;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.DefaultUnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.MDCKeys;
import pl.edu.icm.unity.rest.authn.ext.TLSRetrieval;
import pl.edu.icm.unity.stdext.identity.X500Identity;

/**
 * Performs a final authentication, basing on the endpoint's configuration.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationInterceptor extends AbstractPhaseInterceptor<Message>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, AuthenticationInterceptor.class);
	private MessageSource msg;
	private AuthenticationProcessor authenticationProcessor;
	protected List<AuthenticationFlow> authenticators;
	protected UnsuccessfulAuthenticationCounter UnsuccessfulAuthenticationCounterImpl;
	protected SessionManagement sessionMan;
	protected AuthenticationRealm realm;
	protected final Set<String> notProtectedPaths;
	protected final Set<String> optionalAuthnPaths;
	private Properties endpointProperties;
	private final EntityManagement entityMan;
	
	public AuthenticationInterceptor(MessageSource msg, AuthenticationProcessor authenticationProcessor, 
			List<AuthenticationFlow> authenticators,
			AuthenticationRealm realm, SessionManagement sessionManagement, 
			Set<String> notProtectedPaths,
			Set<String> optionalAuthnPaths,
			Properties endpointProperties, EntityManagement entityMan)
	{
		super(Phase.PRE_INVOKE);
		this.msg = msg;
		this.authenticationProcessor = authenticationProcessor;
		this.realm = realm;
		this.authenticators = authenticators;
		this.endpointProperties = endpointProperties;
		this.entityMan = entityMan;
		this.UnsuccessfulAuthenticationCounterImpl = new DefaultUnsuccessfulAuthenticationCounter(
				realm.getBlockAfterUnsuccessfulLogins(), realm.getBlockFor()*1000);
		this.sessionMan = sessionManagement;
		this.notProtectedPaths = ImmutableSet.copyOf(notProtectedPaths);
		this.optionalAuthnPaths = ImmutableSet.copyOf(optionalAuthnPaths);
	}

	@Override
	public void handleMessage(Message message) throws Fault
	{
		String ip = getClientIP();
		if (UnsuccessfulAuthenticationCounterImpl.getRemainingBlockedTime(ip) > 0)
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
		AuthenticationException authenticationError = null;
		Optional<EntityWithAuthenticators> client = Optional.empty();
		
		if (isToNotProtected(message))
			return;
		
		for (AuthenticationFlow authenticatorFlow: authenticators)
		{
			log.debug("Client authentication attempt using flow " + authenticatorFlow.getId());
			
			try
			{
				client = processAuthnFlow(authnCache, authenticatorFlow);
			} catch (AuthenticationException e)
			{
				if (log.isDebugEnabled())
					log.debug("Authentication set failed to authenticate the client using flow "
							+ authenticatorFlow.getId() + ", " + e);
					authenticationError = new AuthenticationException(msg.getMessage(e.getMessage()));
				break;
			}
			
			if (client.isPresent())
				break;
		}
		
		if (client.isEmpty())
		{
			if (isToOptionallyAuthenticatedPath(message) && authenticationError == null)
			{
				log.debug("Request to an address with optional authentication - {} - "
						+ "invocation will proceed without authentication", 
						message.get(Message.REQUEST_URI));
				return;
			} else
			{
				log.info("Authentication failed for client");
				UnsuccessfulAuthenticationCounterImpl.unsuccessfulAttempt(ip);
				throw new Fault(authenticationError == null ? new Exception("Authentication failed") : authenticationError);
			}
		} else
		{
			authnSuccess(client.get(), ip, ctx);
		}
	}

	private boolean isToOptionallyAuthenticatedPath(Message message)
	{
		return isToSpecialPath(message, optionalAuthnPaths);
	}
	
	private boolean isToNotProtected(Message message)
	{
		boolean notProtected = isToSpecialPath(message, notProtectedPaths);
		if (notProtected)
			log.debug("Request to a not protected address - {} - invocation will proceed without authentication", 
					message.get(Message.REQUEST_URI));
		return notProtected;
	}

	private boolean isToSpecialPath(Message message, Set<String> paths)
	{
		String addressPath = (String) message.get(Message.REQUEST_URI);
		if (addressPath == null)
		{
			log.error("Can not establish the destination address");
			return false;
		}
		for (String notProtected: paths)
			if (addressPath.equals(notProtected))
				return true;
		return false;
	}

	
	private void authnSuccess(EntityWithAuthenticators client, String ip, InvocationContext ctx)
	{
		if (log.isDebugEnabled())
			log.info("Client was successfully authenticated: [" + 
					client.entity.getEntityId() + "] " + client.entity.getAuthenticatedWith().toString());
		UnsuccessfulAuthenticationCounterImpl.successfulAttempt(ip);
		
		String label = getLabel(client.entity.getEntityId());
		LoginSession ls = sessionMan.getCreateSession(client.entity.getEntityId(), realm, 
				label, client.entity.getOutdatedCredentialId(), new RememberMeInfo(false, false), 
				client.firstFactor, client.secondFactor);
		ctx.setLoginSession(ls);
		ls.addAuthenticatedIdentities(client.entity.getAuthenticatedWith());
		ls.setRemoteIdP(client.entity.getRemoteIdP());
		MDC.put(MDCKeys.ENTITY_ID.key, ls.getEntityId());
		MDC.put(MDCKeys.USER.key, ls.getEntityLabel());
	}
	
	private String getLabel(long entityId)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entityId));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute",
					e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return null;
	}
	
	private Optional<EntityWithAuthenticators> processAuthnFlow(Map<String, AuthenticationResult> authnCache,
			AuthenticationFlow authenticationFlow) throws AuthenticationException
	{
		PartialAuthnState state = null;
		for (AuthenticatorInstance authn : authenticationFlow.getFirstFactorAuthenticators())
		{
			log.debug("Client authentication attempt using authenticator {}", authn.getMetadata().getId());
			
			try
			{
				AuthenticationResult result = processAuthenticator(authnCache,
						(CXFAuthentication) authn.getRetrieval());
				if (result.getStatus().equals(Status.deny) && (result.getDenyReason().isPresent()
						&& result.getDenyReason().get().equals(DenyReason.undefinedCredential)))
				{
					log.debug("Not defined credential for {}", authn.getMetadata().getId());
					continue;
				}

				state = authenticationProcessor.processPrimaryAuthnResult(result, authenticationFlow,
						authenticatorOnlyKey(authn.getRetrieval().getAuthenticatorId()));
			} catch (AuthenticationException e)
			{
				throw new AuthenticationException(e.getMessage());
			}
			break;
		}

		if (state == null)
		{
			return Optional.empty();		
		}

		if (state.isSecondaryAuthenticationRequired())
		{
			CXFAuthentication secondFactorAuthn = (CXFAuthentication) state.getSecondaryAuthenticator();
			AuthenticationResult result2 = processAuthenticator(authnCache, secondFactorAuthn);
			AuthenticatedEntity entity = authenticationProcessor.finalizeAfterSecondaryAuthentication(state,
					result2);
			return Optional.of(new EntityWithAuthenticators(entity, state.getFirstFactorOptionId(), 
					authenticatorOnlyKey(secondFactorAuthn.getAuthenticatorId())));			
		} else
		{
			AuthenticatedEntity entity = authenticationProcessor.finalizeAfterPrimaryAuthentication(state, false);
			return  Optional.of(new EntityWithAuthenticators(entity, state.getFirstFactorOptionId(), null));
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
		private final AuthenticationOptionKey firstFactor;
		private final AuthenticationOptionKey secondFactor;

		EntityWithAuthenticators(AuthenticatedEntity entity, AuthenticationOptionKey firstFactor,
				AuthenticationOptionKey secondFactor)
		{
			this.entity = entity;
			this.firstFactor = firstFactor;
			this.secondFactor = secondFactor;
		}
	}
}
