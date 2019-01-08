/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.CookieHelper;
import pl.edu.icm.unity.webui.authn.RememberMeToken.LoginMachineDetails;

/**
 * Internal management of remember me cookies and tokens.  
 * @author P.Piernik
 *
 */
@Component
public class RememberMeProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RememberMeProcessor.class);
	
	public static final String REMEMBER_ME_COOKIE_PFX = "REMEMBERME_";
	public static final String REMEMBER_ME_TOKEN_TYPE = "rememberMe";

	private TokensManagement tokenMan;
	private SessionManagement sessionMan;
	private EntityManagement entityMan;
	
	@Autowired
	public RememberMeProcessor(TokensManagement tokenMan, SessionManagement sessionMan, EntityManagement entityMan)
	{
		this.tokenMan = tokenMan;
		this.sessionMan = sessionMan;
		this.entityMan = entityMan;
	}

	public Optional<LoginSession> processRememberedWholeAuthn(HttpServletRequest httpRequest,
			ServletResponse response, String clientIp, AuthenticationRealm realm,
			UnsuccessfulAuthenticationCounter dosGauard)
	{

		return processRememberedFactor(httpRequest, response, clientIp, realm, dosGauard,
				RememberMePolicy.allowForWholeAuthn);
	}

	public Optional<LoginSession> processRememberedSecondFactor(HttpServletRequest httpRequest,
			ServletResponse response, long entityId, String clientIp,
			AuthenticationRealm realm, UnsuccessfulAuthenticationCounter dosGauard)
	{
		Optional<LoginSession> loginSession = processRememberedFactor(httpRequest, response,
				clientIp, realm, dosGauard, RememberMePolicy.allowFor2ndFactor);

		if (loginSession.isPresent())
		{
			if (loginSession.get().getEntityId() != entityId)
			{
				log.warn("Remember me cookie used in second factor authn by entity "
						+ entityId + " is owned by entity "
						+ loginSession.get().getEntityId()
						+ ", may signal malicious action");
				dosGauard.unsuccessfulAttempt(clientIp);
				return Optional.empty();

			}
		}

		return loginSession;
	}
	
	private Optional<LoginSession> processRememberedFactor(HttpServletRequest httpRequest,
			ServletResponse response, String clientIp, AuthenticationRealm realm,
			UnsuccessfulAuthenticationCounter dosGauard, RememberMePolicy policy)
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
			} catch (AuthenticationException e)
			{
				log.warn("Remember me cookie is invalid", e);
				dosGauard.unsuccessfulAttempt(clientIp);
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

	public void addRememberMeCookieAndUnityToken(HttpServletResponse response, AuthenticationRealm realm, String clientIp,
			long entityId, Date loginTime, String firstFactorOptionId, String secondFactorOptionId)
	{
		if (realm.getRememberMePolicy().equals(RememberMePolicy.disallow))
			return;

		UUID rememberMeSeriesToken = UUID.randomUUID();
		UUID rememberMeToken = UUID.randomUUID();

		RememberMeToken unityRememberMeToken = createRememberMeUnityToken(entityId, realm,
				hash(rememberMeToken.toString()), loginTime, clientIp, firstFactorOptionId,
				secondFactorOptionId);

		byte[] serializedToken = null;
		try
		{
			serializedToken = unityRememberMeToken.getSerialized();
		} catch (JsonProcessingException e)
		{
			log.debug("Can not serialize remember me token, skip setting remember me cookie", e);
			return;
		}

		Date expiration = new Date(System.currentTimeMillis()
				+ (getAbsoluteRememberMeCookieTTL(realm) * 1000));

		try
		{
			tokenMan.addToken(REMEMBER_ME_TOKEN_TYPE, rememberMeSeriesToken.toString(),
					new EntityParam(entityId), serializedToken,
					unityRememberMeToken.getLoginTime(), expiration);
		} catch (EngineException e)
		{
			log.debug("Can not add remember me token, skip setting remember me cookie for " + entityId, e);
			return;
		}
		
		String rememberMeCookieValue = rememberMeSeriesToken.toString() + "|"
				+ rememberMeToken.toString();
		Cookie unityRememberMeCookie = getRememberMeRawCookie(realm.getName(), rememberMeCookieValue,
				getAbsoluteRememberMeCookieTTL(realm));		
		log.debug("Adding remember me cookie and token for {}", entityId);
		response.addCookie(unityRememberMeCookie);
	}

	public void removeRememberMeWithWholeAuthn(String realmName,
			HttpServletRequest request, HttpServletResponse httpResponse)
	{

		Optional<RememberMeCookie> unityRememberMeCookie = Optional.empty();
		try
		{
			unityRememberMeCookie = getRememberMeUnityCookie(request, realmName);
		} catch (AuthenticationException e)
		{
			log.debug("Can not remove remember me token, the cookie content is incorrect", e);
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
		
		Cookie rememberMeCookie = getRememberMeRawCookie(realmName, "", 0);
		log.debug("Remove unity remember me cookie");
		httpResponse.addCookie(rememberMeCookie);
	}
	
	private void removeRememberMeUnityToken(String rememberMeSeriesToken)
	{
		try
		{
			tokenMan.removeToken(REMEMBER_ME_TOKEN_TYPE, rememberMeSeriesToken);
			log.debug("Remove remember me unity token " + rememberMeSeriesToken);
		} catch (Exception e)
		{
			// ok maybe token is not set or expired
			log.debug("Can not remove remember me token + " + rememberMeSeriesToken
					+ ". The token was removed or expired");
		}
	}

	private Optional<String> getRawRememberMeCookie(HttpServletRequest httpRequest,
			String realmName)
	{
		String cookie = CookieHelper.getCookie(httpRequest,
				getRememberMeCookieName(realmName));
		if (cookie == null || cookie.isEmpty())
			return Optional.empty();

		return Optional.ofNullable(cookie);
	}

	private Optional<RememberMeCookie> getRememberMeUnityCookie(HttpServletRequest httpRequest,
			String realmName) throws AuthenticationException
	{
		RememberMeCookie rememberMeCookie = null;
		Optional<String> rawRememberMeCookie = getRawRememberMeCookie(httpRequest,
				realmName);
		if (rawRememberMeCookie.isPresent())
		{
			String[] cookieSplit = rawRememberMeCookie.get().split("\\|");
			if (cookieSplit.length == 2)
			{
				rememberMeCookie = new RememberMeCookie(cookieSplit[0],
						cookieSplit[1]);
			} else
			{
				throw new AuthenticationException(
						"Remember me cookie does not contain two remember me tokens, may signal malicious action");
			}
		}
		return Optional.ofNullable(rememberMeCookie);

	}

	private Optional<RememberMeToken> getAndCheckRememberMeUnityToken(
			RememberMeCookie rememberMeCookie, AuthenticationRealm realm)
			throws AuthenticationException
	{
		Optional<RememberMeToken> unityRememberMeToken = getRememberMeUnityToken(
				rememberMeCookie);

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
			log.debug("The remember me token is invalid, remember me policy was changed");
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

		String secondFactorAuthnOptionId = unityRememberMeToken.get()
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
			log.debug("Not setting entity's label as the client is not authorized to read the attribute",
					e);
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
			tokenById = tokenMan.getTokenById(REMEMBER_ME_TOKEN_TYPE,
					rememberMeCookie.rememberMeSeriesToken);
		} catch (IllegalArgumentException e)
		{
			log.debug("Can not get rememberMeToken, token was removed or expired");
		}
		if (tokenById != null)
		{

			try
			{
				return Optional.ofNullable(RememberMeToken
						.getInstanceFromJson(tokenById.getContents()));
			} catch (IllegalArgumentException e)
			{
				log.debug("Can not parse rememberMe token", e);
			}
		}
		return Optional.empty();
	}

	private Cookie getRememberMeRawCookie(String realmName, String value, int maxAge)
	{
		return CookieHelper.setupHttpCookie(getRememberMeCookieName(realmName), value,
				maxAge);
	}

	private RememberMeToken createRememberMeUnityToken(long entityId, AuthenticationRealm realm,
			byte[] rememberMeTokenHash, Date loginTime, String clientIp, String firstFactorOptionId,
			String secondFactorOptionId)
	{
		
		WebBrowser webBrowser = Page.getCurrent() != null ? Page.getCurrent().getWebBrowser() : null;
		
		
		String osName = "unknown";
		String browser = "unknown";
		if (webBrowser != null)
		{
			if (webBrowser.isLinux())
				osName = "Linux";
			else if (webBrowser.isWindows())
				osName = "Windows";
			else if (webBrowser.isMacOSX())
				osName = "Mac OS X";

			if (webBrowser.isFirefox())
				browser = "Firefox";
			else if (webBrowser.isChrome())
				browser = "Chrome";
			else if (webBrowser.isIE())
				browser = "IE";
			else if (webBrowser.isEdge())
				browser = "Edge";
		}
		
		LoginMachineDetails machineDetails = new LoginMachineDetails(
				clientIp, osName, browser);

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

	private static int getAbsoluteRememberMeCookieTTL(AuthenticationRealm realm)
	{
		return 3600 * 24 * realm.getAllowForRememberMeDays();
	}

	private void updateRememberMeCookieAndUnityToken(RememberMeCookie rememberMeCookie,
			AuthenticationRealm realm, HttpServletResponse httpResponse)
	{
		log.debug("Update remember me cookie and token");
		Optional<RememberMeToken> unityRememberMeToken = getRememberMeUnityToken(
				rememberMeCookie);

		Cookie unityRememberMeCookie = getRememberMeRawCookie(realm.getName(), "", 0);

		if (unityRememberMeToken.isPresent())
		{
			String newToken = UUID.randomUUID().toString();
			unityRememberMeToken.get().setRememberMeTokenHash(hash(newToken));
			Date expiration = new Date(System.currentTimeMillis()
					+ (getAbsoluteRememberMeCookieTTL(realm) * 1000));
			byte[] serializedToken = null;
			try
			{
				serializedToken = unityRememberMeToken.get().getSerialized();
			} catch (JsonProcessingException e)
			{
				log.debug("Can not set remember me token, skip setting remember me cookie",
						e);
				return;
			}

			tokenMan.updateToken(REMEMBER_ME_TOKEN_TYPE,
					rememberMeCookie.rememberMeSeriesToken, expiration,
					serializedToken);

			unityRememberMeCookie.setValue(
					rememberMeCookie.rememberMeSeriesToken + "|" + newToken);
			unityRememberMeCookie.setMaxAge(getAbsoluteRememberMeCookieTTL(realm));
		}

		httpResponse.addCookie(unityRememberMeCookie);
	}

	public static class RememberMeCookie
	{
		public final String rememberMeSeriesToken;

		public final String rememberMeToken;

		public RememberMeCookie(String rememberMeSeriesToken, String rememberMeToken)
		{

			this.rememberMeSeriesToken = rememberMeSeriesToken;
			this.rememberMeToken = rememberMeToken;
		}

	}
}
