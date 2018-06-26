/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.WebBrowser;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
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
public class RememberMeHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RememberMeHelper.class);
	
	public static final String REMEMBER_ME_COOKIE_PFX = "REMEMBERME_";
	public static final String REMEMBER_ME_TOKEN_TYPE = "rememberMe";

	private TokensManagement tokenMan;
	private SessionManagement sessionMan;
	
	@Autowired
	public RememberMeHelper(TokensManagement tokenMan, SessionManagement sessionMan)
	{
		this.tokenMan = tokenMan;
		this.sessionMan = sessionMan;
	}

	public Optional<String> getRawRememberMeCookie(HttpServletRequest httpRequest,
			String realmName)
	{
		String cookie = CookieHelper.getCookie(httpRequest,
				RememberMeHelper.getRememberMeCookieName(realmName));
		if (cookie == null || cookie.isEmpty())
			return Optional.empty();

		return Optional.ofNullable(cookie);
	}

	public Optional<RememberMeCookie> getRememberMeUnityCookie(HttpServletRequest httpRequest,
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
				String message = "Remember me cookie it does not contain two remember me tokens, probably we have attack";
				log.debug(message);
				throw new AuthenticationException(message);
			}
		}
		return Optional.ofNullable(rememberMeCookie);

	}

	public Optional<RememberMeToken> getAndCheckRememberMeUnityToken(
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
			String message = "Someone change remember me cookie contents, probably we have attack";
			log.debug(message);
			throw new AuthenticationException(message);

		}

		if (!realm.getRememberMePolicy()
				.equals(unityRememberMeToken.get().getRememberMePolicy()))
		{
			log.debug("The remember me token is invalid, remember me policy was changed");
			return Optional.empty();
		}

		return unityRememberMeToken;

	}

	public Optional<LoginSession> getLoginSessionFromRememberMeToken(
			RememberMeCookie rememberMeCookie, AuthenticationRealm realm)
			throws AuthenticationException
	{

		Optional<RememberMeToken> unityRememberMeToken = getAndCheckRememberMeUnityToken(
				rememberMeCookie, realm);

		if (!unityRememberMeToken.isPresent())
			return Optional.empty();

		return Optional.of(sessionMan.getCreateSession(
				unityRememberMeToken.get().getEntity(), realm, "", null, null,
				new RememberMeInfo(true, true),
				unityRememberMeToken.get().getAuthnOptionId(), true));
	}

	public Optional<RememberMeToken> getRememberMeUnityToken(RememberMeCookie rememberMeCookie)
	{

		Token tokenById = null;
		try
		{
			tokenById = tokenMan.getTokenById(REMEMBER_ME_TOKEN_TYPE,
					rememberMeCookie.rememberMeSeriesToken);
		} catch (IllegalArgumentException e)
		{
			log.debug("Can not get rememberMeToken, token was removed or expired", e);
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

	public void setRememberMeCookieAndUnityToken(AuthenticationRealm realm, long entityId, Date loginTime,
			String auhtnOptionId)
	{

		if (realm.getRememberMePolicy().equals(RememberMePolicy.disallow))
			return;

		log.debug("Set remember me cookie and token");

		UUID rememberMeSeriesToken = UUID.randomUUID();
		UUID rememberMeToken = UUID.randomUUID();

		String rememberMeCookieName = getRememberMeCookieName(realm.getName());
		String rememberMeCookieValue = rememberMeSeriesToken.toString() + "|"
				+ rememberMeToken.toString();

		Cookie unityRememberMeCookie = new Cookie(rememberMeCookieName,
				rememberMeCookieValue);
		unityRememberMeCookie.setPath("/");
		unityRememberMeCookie.setSecure(true);
		unityRememberMeCookie.setHttpOnly(true);
		unityRememberMeCookie.setMaxAge(getAbsoluteRememberMeCookieTTL(realm));

		RememberMeToken unityRememberMeToken = createRememberMeUnityToken(entityId, realm,
				hash(rememberMeToken.toString()), loginTime, auhtnOptionId);

		byte[] serializedToken = null;
		try
		{
			serializedToken = unityRememberMeToken.getSerialized();
		} catch (JsonProcessingException e)
		{
			log.debug("Can not serialize remember me token, skip setting remember me cookie",
					e);
			return;
		}

		Date expiration = new Date(System.currentTimeMillis()
				+ (getAbsoluteRememberMeCookieTTL(realm) * 1000));

		try
		{
			tokenMan.addToken(REMEMBER_ME_TOKEN_TYPE, rememberMeSeriesToken.toString(), new EntityParam(entityId),
					serializedToken, unityRememberMeToken.getLoginTime(),
					expiration);
		} catch (EngineException e)
		{
			log.debug("Can not add remember me token, skip setting remember me cookie",
					e);
			return;
		} 
		
		HttpServletResponse response = (HttpServletResponse) VaadinServletResponse
				.getCurrent();
		response.addCookie(unityRememberMeCookie);

	}

	private RememberMeToken createRememberMeUnityToken(long entityId, AuthenticationRealm realm,
			byte[] rememberMeTokenHash, Date loginTime, String auhtnOptionId)
	{
		WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
		String osName = "unknown";
		if (webBrowser.isLinux())
			osName = "Linux";
		else if (webBrowser.isWindows())
			osName = "Windows";
		else if (webBrowser.isMacOSX())
			osName = "Mac OS X";

		String browser = "unknown";
		if (webBrowser.isFirefox())
			browser = "Firefox";
		else if (webBrowser.isChrome())
			browser = "Chrome";
		else if (webBrowser.isIE())
			browser = "IE";
		else if (webBrowser.isEdge())
			browser = "Edge";

		LoginMachineDetails machineDetails = new LoginMachineDetails(
				webBrowser.getAddress(), osName, browser);

		return new RememberMeToken(entityId, machineDetails, loginTime, auhtnOptionId,
				rememberMeTokenHash, realm.getRememberMePolicy());

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

	public void clearRememberMeCookieAndUnityToken(String realmName, HttpServletRequest request,
			HttpServletResponse httpResponse)
	{
		log.debug("Clear remember me cookie and token");

		Optional<RememberMeCookie> rememberMeCookie;
		try
		{
			rememberMeCookie = getRememberMeUnityCookie(request, realmName);
		} catch (AuthenticationException e)
		{
			log.debug("Can not remove remember me token and cookie, the cookie content is incorrect",
					e);
			return;
		}
		if (rememberMeCookie.isPresent())
		{
			try
			{
				tokenMan.removeToken(REMEMBER_ME_TOKEN_TYPE,
						rememberMeCookie.get().rememberMeSeriesToken);
			} catch (Exception e)
			{
				// ok maybe token is not set or expired
				log.debug("Can not remove remember me token. The token was removed or expired");
			}

		}

		Cookie unityRememberMeCookie = new Cookie(getRememberMeCookieName(realmName), "");
		unityRememberMeCookie.setPath("/");
		unityRememberMeCookie.setSecure(true);
		unityRememberMeCookie.setHttpOnly(true);
		unityRememberMeCookie.setMaxAge(0);
		httpResponse.addCookie(unityRememberMeCookie);
	}

	public void updateRememberMeCookieAndUnityToken(RememberMeCookie rememberMeCookie,
			AuthenticationRealm realm, HttpServletResponse httpResponse)
	{
		log.debug("Update remember me cookie and token");
		Optional<RememberMeToken> unityRememberMeToken = getRememberMeUnityToken(
				rememberMeCookie);

		String rememberMeCookieName = getRememberMeCookieName(realm.getName());
		String rememberMeCookieValue = "";

		Cookie unityRememberMeCookie = new Cookie(rememberMeCookieName,
				rememberMeCookieValue);
		unityRememberMeCookie.setPath("/");
		unityRememberMeCookie.setSecure(true);
		unityRememberMeCookie.setHttpOnly(true);
		unityRememberMeCookie.setMaxAge(0);

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
