/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.session.SessionManagementEE8;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;
import pl.edu.icm.unity.store.api.TokenDAO.TokenNotFoundException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal management of remember me cookies and tokens.  
 * @author P.Piernik
 *
 */
@Component
class RememberMeProcessorImpl implements RememberMeProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RememberMeProcessorImpl.class);

	public static final String REMEMBER_ME_COOKIE_PFX = "REMEMBERME_";

	private final TokensManagement tokenMan;
	private final SessionManagementEE8 sessionMan;
	private final EntityManagement entityMan;

	@Autowired
	RememberMeProcessorImpl(TokensManagement tokenMan, SessionManagementEE8 sessionMan, EntityManagement entityMan)
	{
		this.tokenMan = tokenMan;
		this.sessionMan = sessionMan;
		this.entityMan = entityMan;
	}

	@Override
	public Optional<LoginSession> processRememberedWholeAuthn(HttpServletRequest httpRequest,
			ServletResponse response, String clientIp, AuthenticationRealm realm,
			UnsuccessfulAuthenticationCounter dosGuard)
	{

		return processRememberedFactor(httpRequest, response, clientIp, realm, dosGuard,
				RememberMePolicy.allowForWholeAuthn);
	}

	@Override
	public Optional<LoginSession> processRememberedSecondFactor(HttpServletRequest httpRequest,
			ServletResponse response, long entityId, String clientIp,
			AuthenticationRealm realm, UnsuccessfulAuthenticationCounter dosGuard)
	{
		Optional<LoginSession> loginSession = processRememberedFactor(httpRequest, response,
				clientIp, realm, dosGuard, RememberMePolicy.allowFor2ndFactor);

		if (loginSession.isPresent())
		{
			if (loginSession.get().getEntityId() != entityId)
			{
				log.warn("Remember me cookie used in second factor authn by entity "
						+ entityId + " is owned by entity "
						+ loginSession.get().getEntityId()
						+ ", may signal malicious action");
				dosGuard.unsuccessfulAttempt(clientIp);
				return Optional.empty();

			}
		}

		return loginSession;
	}
	
	private Optional<LoginSession> processRememberedFactor(HttpServletRequest httpRequest,
			ServletResponse response, String clientIp, AuthenticationRealm realm,
			UnsuccessfulAuthenticationCounter dosGuard, RememberMePolicy policy)
	{
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		Optional<LoginSession> loginSessionFromRememberMe = Optional.empty();
		Optional<RememberMeCookie> rememberMeCookie = Optional.empty();
		if (realm.getRememberMePolicy().equals(policy))
		{

			try
			{
				rememberMeCookie = getRememberMeUnityCookie(httpRequest,
						realm.getName());
				if (rememberMeCookie.isPresent())
				{
					loginSessionFromRememberMe = getLoginSessionFromRememberMeToken(
							rememberMeCookie.get(), realm,
							policy.equals(RememberMePolicy.allowForWholeAuthn));
				}
			} catch (CookieParseException e)
			{
				log.warn("Remember me cookie can not be parsed", e);
				dosGuard.unsuccessfulAttempt(clientIp);
			} catch (AuthenticationException e)
			{
				log.warn("Remember me cookie is invalid", e);
				dosGuard.unsuccessfulAttempt(clientIp);
			}

		} else
		{
			return Optional.empty();
		}

		if (!loginSessionFromRememberMe.isPresent())
		{
			if (rememberMeCookie.isPresent())
			{
				removeRememberMeCookie(realm.getName(), httpResponse);
				removeRememberMeUnityToken(rememberMeCookie.get().rememberMeSeriesToken);	
			}	
			return Optional.empty();
		} else
		{
			updateRememberMeCookieAndUnityToken(rememberMeCookie.get(), realm,
					httpResponse);
		}

		return loginSessionFromRememberMe;
	}

	@Override
	public void addRememberMeCookieAndUnityToken(HttpServletResponse response, AuthenticationRealm realm, 
			LoginMachineDetails machineDetails,
			long entityId, Date loginTime, AuthenticationOptionKey firstFactorOptionId, 
			AuthenticationOptionKey secondFactorOptionId)
	{
		if (realm.getRememberMePolicy().equals(RememberMePolicy.disallow))
			return;

		UUID rememberMeSeriesToken = UUID.randomUUID();
		UUID rememberMeToken = UUID.randomUUID();

		RememberMeToken unityRememberMeToken = createRememberMeUnityToken(entityId, realm,
				hash(rememberMeToken.toString()), loginTime, machineDetails, firstFactorOptionId,
				secondFactorOptionId);

		byte[] serializedToken = null;
		try
		{
			serializedToken = unityRememberMeToken.getSerialized();
		} catch (JsonProcessingException e)
		{
			log.warn("Can not serialize remember me token, skip setting remember me cookie", e);
			return;
		}

		Duration absoluteRememberMeCookieTTL = getAbsoluteRememberMeCookieTTL(realm);
		Date expiration = getExpirationDateAfter(absoluteRememberMeCookieTTL);

		try
		{
			tokenMan.addToken(REMEMBER_ME_TOKEN_TYPE, rememberMeSeriesToken.toString(),
					new EntityParam(entityId), serializedToken,
					unityRememberMeToken.getLoginTime(), expiration);
		} catch (EngineException e)
		{
			log.warn("Can not add remember me token, skip setting remember me cookie for " + entityId, e);
			return;
		}
		
		String rememberMeCookieValue = new RememberMeCookie(rememberMeSeriesToken.toString(), 
				rememberMeToken.toString()).toHttpCookieValue();
		Cookie unityRememberMeCookie = buildRememberMeHttpCookie(realm.getName(), rememberMeCookieValue,
				absoluteRememberMeCookieTTL);		
		log.info("Adding remember me cookie and token for {}", entityId);
		response.addCookie(unityRememberMeCookie);
	}

	private Date getExpirationDateAfter(Duration durationFromNow)
	{
		return new Date(System.currentTimeMillis() + durationFromNow.toMillis());
	}
	
	@Override
	public void removeRememberMeWithWholeAuthn(String realmName,
			HttpServletRequest request, HttpServletResponse httpResponse)
	{

		Optional<RememberMeCookie> unityRememberMeCookie = Optional.empty();
		try
		{
			unityRememberMeCookie = getRememberMeUnityCookie(request, realmName);
		} catch (CookieParseException e)
		{
			log.warn("Can not remove remember me token, the cookie content is incorrect", e);
			removeRememberMeCookie(realmName, httpResponse);
			
		}
		if (unityRememberMeCookie.isPresent())
		{

			Optional<RememberMeToken> rememberMeUnityToken = getRememberMeUnityToken(
					unityRememberMeCookie.get());
			if (rememberMeUnityToken.isPresent())
			{
				if (rememberMeUnityToken.get().getRememberMePolicy()
						.equals(RememberMePolicy.allowForWholeAuthn))
				{
					removeRememberMeCookie(realmName, httpResponse);
					removeRememberMeUnityToken(unityRememberMeCookie.get().rememberMeSeriesToken);
				}
			}else
			{
				removeRememberMeCookie(realmName, httpResponse);
			}
		} 

	}
	
	private void removeRememberMeCookie(String realmName, HttpServletResponse httpResponse)
	{
		httpResponse.addCookie(buildRememberMeHttpCookieCleaner(realmName));
	}
	
	private void removeRememberMeUnityToken(String rememberMeSeriesToken)
	{
		try
		{
			tokenMan.removeToken(REMEMBER_ME_TOKEN_TYPE, rememberMeSeriesToken);
			log.debug("Remove remember me unity token " + rememberMeSeriesToken);
		} catch (Exception e)
		{
			log.info("Can not remove remember me token {}. The token was removed or expired", 
					rememberMeSeriesToken, e);
		}
	}

	private Optional<String> getHttpRememberMeCookieValue(HttpServletRequest httpRequest, String realmName)
	{
		String cookie = CookieHelper.getCookie(httpRequest, getRememberMeCookieName(realmName));
		return cookie == null || cookie.isEmpty() ? Optional.empty() : Optional.ofNullable(cookie);
	}

	private Optional<RememberMeCookie> getRememberMeUnityCookie(HttpServletRequest httpRequest, String realmName) 
	{
		Optional<String> httpRememberMeCookieValue = getHttpRememberMeCookieValue(httpRequest, realmName);
		return httpRememberMeCookieValue.map(RememberMeCookie::parseHttpCookieValue);
	}

	private Optional<RememberMeToken> getAndCheckRememberMeUnityToken(
			RememberMeCookie rememberMeCookie, AuthenticationRealm realm)
			throws AuthenticationException
	{
		Optional<RememberMeToken> unityRememberMeToken = getRememberMeUnityToken(rememberMeCookie);

		if (!unityRememberMeToken.isPresent())
			return Optional.empty();

		if (!Arrays.equals(unityRememberMeToken.get().getRememberMeTokenHash(),
				hash(rememberMeCookie.rememberMeToken)))
		{
			throw new AuthenticationException("Someone change remember me cookie contents, may signal malicious action");

		}

		if (!realm.getRememberMePolicy()
				.equals(unityRememberMeToken.get().getRememberMePolicy()))
		{
			log.info("The remember me token is invalid, remember me policy was changed");
			return Optional.empty();
		}

		return unityRememberMeToken;

	}

	private Optional<LoginSession> getLoginSessionFromRememberMeToken(
			RememberMeCookie rememberMeCookie, AuthenticationRealm realm,
			boolean firstFactorSkipped) throws AuthenticationException
	{

		Optional<RememberMeToken> unityRememberMeToken = getAndCheckRememberMeUnityToken(
				rememberMeCookie, realm);

		if (!unityRememberMeToken.isPresent())
			return Optional.empty();

		AuthenticationOptionKey secondFactorAuthnOptionId = unityRememberMeToken.get()
				.getSecondFactorAuthnOptionId();
		long entityId = unityRememberMeToken.get().getEntity();
		String label = getLabel(entityId);
		
		LoginSession session = sessionMan.createSession(entityId,
				realm, 
				label, 
				null, 
				new RememberMeInfo(firstFactorSkipped,
						secondFactorAuthnOptionId != null),
				unityRememberMeToken.get().getFirstFactorAuthnOptionId(),
				secondFactorAuthnOptionId);
		return Optional.of(session);
	}

	private String getLabel(long entityId)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entityId));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute", e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return "";
	}
	
	private Optional<RememberMeToken> getRememberMeUnityToken(RememberMeCookie rememberMeCookie)
	{
		Token tokenById = null;
		try
		{
			tokenById = tokenMan.getTokenById(REMEMBER_ME_TOKEN_TYPE, rememberMeCookie.rememberMeSeriesToken);
		} catch (TokenNotFoundException e)
		{
			log.debug("Can not get rememberMeToken, token was removed or expired");
		}
		if (tokenById != null)
		{

			try
			{
				return Optional.ofNullable(RememberMeToken.getInstanceFromJson(tokenById.getContents()));
			} catch (IllegalArgumentException e)
			{
				log.warn("Can not parse rememberMe token", e);
			}
		}
		return Optional.empty();
	}

	private Cookie buildRememberMeHttpCookie(String realmName, String value, Duration maxAge)
	{
		return CookieHelper.setupHttpCookie(getRememberMeCookieName(realmName), value,
				(int)maxAge.get(ChronoUnit.SECONDS));
	}

	private Cookie buildRememberMeHttpCookieCleaner(String realmName)
	{
		return CookieHelper.setupHttpCookie(getRememberMeCookieName(realmName), "", 0);
	}
	
	private RememberMeToken createRememberMeUnityToken(long entityId, AuthenticationRealm realm,
			byte[] rememberMeTokenHash, Date loginTime, LoginMachineDetails machineDetails, 
			AuthenticationOptionKey firstFactorOptionId,
			AuthenticationOptionKey secondFactorOptionId)
	{
		return new RememberMeToken(entityId, machineDetails, loginTime, firstFactorOptionId,
				secondFactorOptionId, rememberMeTokenHash,
				realm.getRememberMePolicy());
	}

	private byte[] hash(String current)
	{
		byte[] currentBytes = current.getBytes(StandardCharsets.UTF_8);
		SHA256Digest digest = new SHA256Digest();
		digest.update(currentBytes, 0, currentBytes.length);
		byte[] hashed = new byte[digest.getDigestSize()];
		digest.doFinal(hashed, 0);
		return hashed;
	}

	public static String getRememberMeCookieName(String realmName)
	{
		return REMEMBER_ME_COOKIE_PFX + realmName;
	}

	private static Duration getAbsoluteRememberMeCookieTTL(AuthenticationRealm realm)
	{
		return Duration.ofDays(realm.getAllowForRememberMeDays());
	}

	private void updateRememberMeCookieAndUnityToken(RememberMeCookie rememberMeCookie,
			AuthenticationRealm realm, HttpServletResponse httpResponse)
	{
		log.debug("Update remember me cookie and token");
		Optional<RememberMeToken> unityRememberMeToken = getRememberMeUnityToken(rememberMeCookie);

		if (unityRememberMeToken.isPresent())
		{
			String newToken = UUID.randomUUID().toString();
			unityRememberMeToken.get().setRememberMeTokenHash(hash(newToken));
			Duration absoluteRememberMeCookieTTL = getAbsoluteRememberMeCookieTTL(realm);
			Date expiration = getExpirationDateAfter(absoluteRememberMeCookieTTL);
			byte[] serializedToken = null;
			try
			{
				serializedToken = unityRememberMeToken.get().getSerialized();
			} catch (JsonProcessingException e)
			{
				log.warn("Can not set remember me token, skip setting remember me cookie", e);
				return;
			}

			tokenMan.updateToken(REMEMBER_ME_TOKEN_TYPE,
					rememberMeCookie.rememberMeSeriesToken, expiration, serializedToken);

			String updatedValue = new RememberMeCookie(rememberMeCookie.rememberMeSeriesToken, newToken)
						.toHttpCookieValue();
			httpResponse.addCookie(buildRememberMeHttpCookie(realm.getName(), 
					updatedValue, absoluteRememberMeCookieTTL));
		} else
		{
			removeRememberMeCookie(realm.getName(), httpResponse);
		}
	}

	private static class RememberMeCookie
	{
		private final String rememberMeSeriesToken;
		private final String rememberMeToken;

		private RememberMeCookie(String rememberMeSeriesToken, String rememberMeToken)
		{

			this.rememberMeSeriesToken = rememberMeSeriesToken;
			this.rememberMeToken = rememberMeToken;
		}
		
		static RememberMeCookie parseHttpCookieValue(String httpCookieValue)
		{
			String[] cookieSplit = httpCookieValue.split("\\|");
			if (cookieSplit.length == 2)
			{
				return new RememberMeCookie(cookieSplit[0], cookieSplit[1]);
			} else
			{
				throw new CookieParseException();
			}
		}
		
		String toHttpCookieValue()
		{
			return rememberMeSeriesToken + "|" + rememberMeToken;
		}
	}
	
	private static class CookieParseException extends RuntimeException
	{
	}
}
