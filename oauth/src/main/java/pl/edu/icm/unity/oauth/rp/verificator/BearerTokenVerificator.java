/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher.ClientAuthnMode;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.verificator.ResultsCache.CacheEntry;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Verificator of bearer access token.
 * 
 * @author K. Benedyczak
 */
public class BearerTokenVerificator extends AbstractRemoteVerificator implements AccessTokenExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, BearerTokenVerificator.class);
	private OAuthRPProperties verificatorProperties;
	private TokenVerificatorProtocol tokenChecker;
	private PKIManagement pkiMan;
	private TokensManagement tokensMan;
	private String translationProfile;
	private CacheProvider cacheProvider;
	private ResultsCache cache;
	
	
	public BearerTokenVerificator(String name, String description,
			TranslationProfileManagement profileManagement,
			PKIManagement pkiMan, InputTranslationEngine trEngine,
			TokensManagement tokensMan, CacheProvider cacheProvider)
	{
		super(name, description, AccessTokenExchange.ID, profileManagement, trEngine);
		this.pkiMan = pkiMan;
		this.tokensMan = tokensMan;
		this.cacheProvider = cacheProvider;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			verificatorProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OAuth RP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			verificatorProperties = new OAuthRPProperties(properties, pkiMan, tokensMan);
			tokenChecker = verificatorProperties.getTokenChecker();
			translationProfile = verificatorProperties.getValue(OAuthRPProperties.TRANSLATION_PROFILE);
			int ttl = -1;
			if (verificatorProperties.isSet(OAuthRPProperties.CACHE_TIME))
				ttl = verificatorProperties.getIntValue(OAuthRPProperties.CACHE_TIME);
			cache = new ResultsCache(cacheProvider.getManager(), ttl);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator(?)", e);
		}
		
	}

	@Override
	public AuthenticationResult checkToken(BearerAccessToken token, SandboxAuthnResultCallback sandboxCallback) 
			throws AuthenticationException
	{
		RemoteAuthnState state = startAuthnResponseProcessing(sandboxCallback, 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_OAUTH);
		try
		{
			return checkTokenInterruptible(token, state);
		} catch (AuthenticationException e)
		{
			finishAuthnResponseProcessing(state, e);
			throw e;
		} catch (Exception e)
		{
			finishAuthnResponseProcessing(state, e);
			throw new AuthenticationException("Authentication error ocurred", e);
		}
	}
	
	public AuthenticationResult checkTokenInterruptible(BearerAccessToken token, RemoteAuthnState state) 
			throws Exception
	{
		CacheEntry cached = cache.getCached(token.getValue());
		if (cached != null)
		{
			TokenStatus status = cached.getTokenStatus();
			if (cached.getTokenStatus().isValid())
			{
				if (!checkScopes(status))
				{
					return new AuthenticationResult(Status.deny, null, null);
				}
				RemotelyAuthenticatedInput input = assembleBaseResult(status, 
						cached.getAttributes(), getName());
				return getResult(input, translationProfile, state);				
			} else
			{
				return new AuthenticationResult(Status.deny, null, null);
			}
		}
		
		TokenStatus status = tokenChecker.checkToken(token);
		if (status.isValid())
		{
			if (!checkScopes(status))
			{
				cache.cache(token.getValue(), status, null);
				return new AuthenticationResult(Status.deny, null, null);
			}
			
			Map<String, String> attrs;
			attrs = getUserProfileInformation(token);
			cache.cache(token.getValue(), status, attrs);
			RemotelyAuthenticatedInput input = assembleBaseResult(status, attrs, getName());
			return getResult(input, translationProfile, state);
		} else
		{
			cache.cache(token.getValue(), status, null);
			return new AuthenticationResult(Status.deny, null, null);
		}
	}

	
	private boolean checkScopes(TokenStatus status)
	{
		List<String> required = verificatorProperties.getListOfValues(OAuthRPProperties.REQUIRED_SCOPES);
		if (!required.isEmpty() && status.getScope() == null)
		{
			log.debug("The token validation didn't provide any scope, but there are required scopes");
			return false;
		}
		required.removeAll(status.getScope().toStringList());
		if (!required.isEmpty())
		{
			log.debug("The following required scopes are not present: " + required);
			return false;
		}
		return true;
	}
	
	private Map<String, String> getUserProfileInformation(BearerAccessToken accessToken) throws AuthenticationException
	{
		boolean openIdMode = verificatorProperties.getBooleanValue(OAuthRPProperties.OPENID_MODE);
		String profileEndpoint = verificatorProperties.getValue(OAuthRPProperties.PROFILE_ENDPOINT);
		ServerHostnameCheckingMode checkingMode = verificatorProperties.getEnumValue(
				OAuthRPProperties.CLIENT_HOSTNAME_CHECKING, 
				ServerHostnameCheckingMode.class);
		X509CertChainValidator validator = verificatorProperties.getValidator();
		Map<String, String> attrs = new HashMap<>();
		if (profileEndpoint == null)
		{
			log.debug("The profile endpoint is not defined, skipping the profile fetching");
			return attrs;
		}
		if (openIdMode)
		{
			try
			{
				UserProfileFetcher.fetchOpenIdUserInfo(accessToken, new URI(profileEndpoint), 
						attrs, checkingMode, validator);
			} catch (Exception e)
			{
				throw new AuthenticationException("Can not fetch user's profile information", e);
			}
		} else
		{
			ClientAuthnMode selectedMethod = verificatorProperties.getEnumValue(
					OAuthRPProperties.CLIENT_AUTHN_MODE, 
					ClientAuthnMode.class);
			try
			{
				UserProfileFetcher.fetchUserInfo(accessToken, selectedMethod, 
						profileEndpoint, attrs, checkingMode, validator);
			} catch (ParseException | IOException e)
			{
				throw new AuthenticationException("Can not fetch user's profile information", e);
			}
		}
		return attrs;
	}
	
	private RemotelyAuthenticatedInput assembleBaseResult(TokenStatus tokenStatus, 
			Map<String, String> attrs, String idpName)
	{
		RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
		for (Map.Entry<String, String> a: attrs.entrySet())
		{
			ret.addAttribute(new RemoteAttribute(a.getKey(), a.getValue()));
		}
		
		if (attrs.containsKey("sub"))
			ret.addIdentity(new RemoteIdentity(attrs.get("sub"), IdentifierIdentity.ID));
		if (tokenStatus.getSubject() != null)
		{
			ret.addIdentity(new RemoteIdentity(tokenStatus.getSubject(), IdentifierIdentity.ID));
		}
		if (tokenStatus.getScope() != null)
			ret.addAttribute(new RemoteAttribute("scope", 
					tokenStatus.getScope().toStringList().toArray()));
		if (attrs.containsKey("sub") && tokenStatus.getSubject() != null && 
				!tokenStatus.getSubject().equals(attrs.get("sub")))
			log.warn("Received subject from the profile endpoint differs from the subject "
					+ "established during access token verification. "
					+ "Will use subject from verification: " + tokenStatus.getSubject() + 
					" ignored: " + attrs.get("sub"));
			
		return ret;
	}

}
