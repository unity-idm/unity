/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.AuthNInfo;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.session.*;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static pl.edu.icm.unity.base.audit.AuditEventTag.AUTHN;

/**
 * Implementation of {@link SessionManagement}
 * @author K. Benedyczak
 */
@Component
public class SessionManagementImpl implements SessionManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, SessionManagementImpl.class);
	public static final long DB_ACTIVITY_WRITE_DELAY = 3000;
	public static final String SESSION_TOKEN_TYPE = "session";
	private final TokensManagement tokensManagement;
	private final LoginToHttpSessionBinder sessionBinder;
	private final SessionParticipantTypesRegistry participantTypesRegistry;
	private final EntityDAO entityDAO;
	private final AttributesHelper attributeHelper;
	private final AuditPublisher auditPublisher;
	private final TransactionalRunner tx;

	/**
	 * map of timestamps indexed by session ids, when the last activity update was written to DB.
	 */
	private Map<String, Long> recentUsageUpdates = new WeakHashMap<>();

	@Autowired
	public SessionManagementImpl(TokensManagement tokensManagement, ExecutorsService execService,
			LoginToHttpSessionBinder sessionBinder,
			SessionParticipantTypesRegistry participantTypesRegistry,
			EntityDAO entityDAO, AttributesHelper attributeHelper, AuditPublisher auditPublisher,
			TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.sessionBinder = sessionBinder;
		this.participantTypesRegistry = participantTypesRegistry;
		this.entityDAO = entityDAO;
		this.attributeHelper = attributeHelper;
		this.auditPublisher = auditPublisher;
		this.tx = tx;
		execService.getScheduledService().scheduleWithFixedDelay(new TerminateInactiveSessions(), 
				20, 30, TimeUnit.SECONDS);
	}

	@Override
	@Transactional
	public LoginSession getCreateSession(long loggedEntity, AuthenticationRealm realm, String entityLabel, 
				String outdatedCredentialId, RememberMeInfo rememberMeInfo,
				AuthenticationOptionKey firstFactorOptionId, AuthenticationOptionKey secondFactorOptionId, RemoteAuthnMetadata authnContext,
				Set<AuthenticationMethod> authenticationMethods, Instant authenticationTime)
	{
		try
		{

			try
			{
				LoginSession ret = getOwnedSessionInternal(
						new EntityParam(loggedEntity), realm.getName());
				if (ret != null)
				{
					Date now = new Date();
					ret.setLastUsed(now);
					ret.setRememberMeInfo(rememberMeInfo);
					ret.setOutdatedCredentialId(outdatedCredentialId);
					ret.setLogin1stFactor(new AuthNInfo(firstFactorOptionId, now));
					ret.setLogin2ndFactor(new AuthNInfo(secondFactorOptionId, now));
					ret.setAuthenticationMethods(authenticationMethods);
					ret.setAuthenticationTime(authenticationTime);
					byte[] contents = ret.getTokenContents();
					tokensManagement.updateToken(SESSION_TOKEN_TYPE,
							ret.getId(), null, contents);

					if (log.isDebugEnabled())
						log.info("Using existing session " + ret.getId()
								+ " for logged entity "
								+ ret.getEntityId() + " in realm "
								+ realm.getName());
					return ret;
				}
			} catch (EngineException e)
			{
				throw new InternalException(
						"Can't retrieve current sessions of the "
								+ "authenticated user",
						e);
			}

			return createSession(loggedEntity, realm, entityLabel, outdatedCredentialId,
					rememberMeInfo, firstFactorOptionId, secondFactorOptionId, authnContext, authenticationMethods, authenticationTime);

		} finally
		{
			clearScheduledRemovalStatus(loggedEntity);
		}
	}
	
	/**
	 * If entity is in the state {@link EntityState#onlyLoginPermitted} this method clears the 
	 *  removal of the entity: state is set to enabled and user ordered removal is removed.
	 */
	private void clearScheduledRemovalStatus(long entityId) 
	{
		EntityInformation info = entityDAO.getByKey(entityId);
		if (info.getState() != EntityState.onlyLoginPermitted)
			return;
		log.info("Removing scheduled removal of an account [as the user is being logged] for entity " + 
			entityId);
		info.setState(EntityState.valid);
		info.setRemovalByUserTime(null);
		entityDAO.updateByKey(entityId, info);
	}
	
	@Override
	@Transactional
	public LoginSession createSession(long loggedEntity, AuthenticationRealm realm,
			String entityLabel, String outdatedCredentialId, 
			RememberMeInfo rememberMeInfo, AuthenticationOptionKey firstFactorOptionId,
			AuthenticationOptionKey secondFactorOptionId, RemoteAuthnMetadata authnContext, Set<AuthenticationMethod> authenticationMethods, Instant authenticationTime)
	{
		UUID randomid = UUID.randomUUID();
		String id = randomid.toString();
		Date now = new Date();
		LoginSession ls = new LoginSession(id, now, 
				realm.getMaxInactivity()*1000, loggedEntity, 
				realm.getName(), rememberMeInfo, new AuthNInfo(firstFactorOptionId, now), 
				new AuthNInfo(secondFactorOptionId, now));
		ls.setOutdatedCredentialId(outdatedCredentialId);
		ls.setEntityLabel(entityLabel);
		ls.setFirstFactorRemoteIdPAuthnContext(authnContext);
		ls.setAuthenticationMethods(authenticationMethods);
		ls.setAuthenticationTime(authenticationTime);
		try
		{
			tokensManagement.addToken(SESSION_TOKEN_TYPE, id, new EntityParam(loggedEntity), 
					ls.getTokenContents(), ls.getStarted(), ls.getExpires());
			updateLoginAttributes(loggedEntity, ls.getStarted());
			auditLogSession(ls, loggedEntity, firstFactorOptionId, secondFactorOptionId, realm);
		} catch (Exception e)
		{
			throw new InternalException("Can't create a new session", e);
		}
		log.info("Created a new session {} for logged entity {} ({}) in realm {}", 
				ls.getId(), ls.getEntityLabel(), ls.getEntityId(), realm.getName());
		return ls;
	}

	private void auditLogSession(LoginSession ls, long loggedEntity, AuthenticationOptionKey firstFactorOptionId,
			AuthenticationOptionKey secondFactorOptionId, AuthenticationRealm realm)
	{
		Map<String, String> details = new HashMap<>();
		details.put("firstFactorOption", firstFactorOptionId.toStringEncodedKey());
		if (secondFactorOptionId != null)
			details.put("secondFactorOption", secondFactorOptionId.toStringEncodedKey());
		details.put("realm", realm.getName());
		auditPublisher.log(AuditEventTrigger.builder()
				.type(AuditEventType.SESSION)
				.action(AuditEventAction.ADD)
				.name(ls.getId())
				.details(details)
				.subject(loggedEntity)
				.tags(AUTHN));
	}
	
	@Transactional
	@Override
	public void updateSessionAttributes(String id, SessionManagement.AttributeUpdater updater)
	{
		updateSession(id, session -> updater.updateAttributes(session.getSessionData()));
	}

	@Transactional
	@Override
	public void recordAdditionalAuthentication(String id, AuthenticationOptionKey optionId)
	{
		updateSession(id, session -> session.setAdditionalAuthn(new AuthNInfo(optionId, new Date())));
		log.info("Recorded additional authentication with {} for session {}", optionId, id);	
	}
	
	@Transactional
	@Override
	public void removeSession(String id, boolean soft)
	{
		removeSessionTransactional(id, soft);
	}

	private void removeSessionTransactional(String id, boolean soft)
	{
		sessionBinder.removeLoginSession(id, soft);
		try
		{
			Token tokenToRemove = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
			tokensManagement.removeToken(SESSION_TOKEN_TYPE, id);
			auditPublisher.log(AuditEventTrigger.builder()
					.type(AuditEventType.SESSION)
					.action(AuditEventAction.REMOVE)
					.name(id)
					.subject(tokenToRemove.getOwner())
					.tags(AUTHN));
			log.info("Terminated session {} of entity {}", id, tokenToRemove.getOwner());
		} catch (IllegalArgumentException e)
		{
			//not found - ok
		}
	}

	
	@Override
	public LoginSession getSession(String id)
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
		LoginSession session = token2session(token);
		if (session.isExpiredAt(System.currentTimeMillis()))
			throw new SessionExpiredException();
		log.trace("Returning session {} last used at {} maxInactivity {}", id, session.getLastUsed(), 
				session.getMaxInactivity());
		return session;
	}

	
	private LoginSession getOwnedSessionInternal(EntityParam owner, String realm)
			throws EngineException
	{
		List<Token> tokens = tokensManagement.getOwnedTokens(SESSION_TOKEN_TYPE, owner);
		for (Token token: tokens)
		{
			LoginSession ls = token2session(token);
			if (realm.equals(ls.getRealm()) && !ls.isExpiredAt(System.currentTimeMillis()))
				return ls;
		}
		return null;
	}
	
	@Override
	@Transactional
	public LoginSession getOwnedSession(EntityParam owner, String realm)
			throws EngineException
	{
		LoginSession ret = getOwnedSessionInternal(owner, realm);
		if (ret == null)
			throw new WrongArgumentException("No session for this owner in the given realm");
		return ret;
	}
	
	@Override
	@Transactional
	public void updateSessionActivity(String id)
	{
		Long lastWrite = recentUsageUpdates.get(id);
		if (lastWrite != null)
		{
			if (System.currentTimeMillis() < lastWrite + DB_ACTIVITY_WRITE_DELAY)
				return;
		}
		
		if (!updateSession(id, session -> session.setLastUsed(new Date())))
			throw new SessionExpiredException();

		log.trace("Updated in db session activity timestamp for " + id);
		recentUsageUpdates.put(id, System.currentTimeMillis());
	}
	
	@Override
	public void addSessionParticipant(SessionParticipant... participant)
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		LoginSession ls = invocationContext.getLoginSession();
		try
		{
			SessionParticipants.AddParticipantToSessionTask addTask = 
					new SessionParticipants.AddParticipantToSessionTask(participantTypesRegistry,
					participant); 
			updateSessionAttributes(ls.getId(), addTask);
		} catch (IllegalArgumentException e)
		{
			throw new InternalException("Can not add session participant to the existing session?", e);
		}
	}
	
	private boolean updateSession(String id, Consumer<LoginSession> updater) 
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
		LoginSession session = token2session(token);
		
		if (session.isExpiredAt(System.currentTimeMillis()))
			return false;
		
		updater.accept(session);
		updateCurrentSessionIfMatching(session);
		
		byte[] contents = session.getTokenContents();
		tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents);
		return true;
	}
	
	private void updateCurrentSessionIfMatching(LoginSession changed)
	{
		if (!InvocationContext.hasCurrent())
			return;
		LoginSession current = InvocationContext.getCurrent().getLoginSession();
		if (current == null)
			return;
		if (!changed.getId().equals(current.getId()))
			return;
		
		InvocationContext.getCurrent().setLoginSession(changed);
	}
	
	private LoginSession token2session(Token token)
	{
		LoginSession session = new LoginSession();
		session.deserialize(token);
		return session;
	}
	
	private void updateLoginAttributes(long entityId, Date started) throws EngineException
	{
		String loginTime = LocalDateTime.ofInstant(
				started.toInstant().truncatedTo(ChronoUnit.SECONDS), ZoneId.systemDefault())
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		Attribute lastAuthn = StringAttribute.of(
				LastAuthenticationAttributeTypeProvider.LAST_AUTHENTICATION, "/", loginTime);
		attributeHelper.addSystemAttribute(entityId, lastAuthn, true);
	}
	
	private class TerminateInactiveSessions implements Runnable
	{
		@Override
		public void run()
		{
			List<Token> tokens;
			try
			{
				tokens = tokensManagement.getAllTokens(SESSION_TOKEN_TYPE);
			} catch (Exception e)
			{
				log.warn("Encounterd an error when trying to obtain session tokens from DB. "
						+ "Cleanup will be tried again in the next round.", e);
				return;
			}
			long now = System.currentTimeMillis();
			for (Token t: tokens)
			{
				try
				{
					removeSessionIfExpired(now, t);
				} catch (Exception e)
				{
					log.warn("Removing expired session " + t.getValue() + " failed", e);
				}
			}
		}
		
		private void removeSessionIfExpired(long now, Token t)
		{
			LoginSession session = token2session(t);
			long inactiveFor = now - session.getLastUsed().getTime(); 
			if (inactiveFor > session.getMaxInactivity())
			{
				log.info("Expiring login session " + session + " inactive for: " + 
						inactiveFor);
				try
				{
					tx.runInTransaction(() -> removeSessionTransactional(session.getId(), false));
				} catch (Exception e)
				{
					log.error("Can't expire the session " + session, e);
				}
			}
		}
	}
	
	public static class SessionExpiredException extends IllegalArgumentException
	{
	}
}
