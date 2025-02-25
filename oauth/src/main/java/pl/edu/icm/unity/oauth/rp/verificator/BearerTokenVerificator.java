/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.remote.*;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.profile.OpenIdProfileFetcher;
import pl.edu.icm.unity.oauth.client.profile.PlainProfileFetcher;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.verificator.ResultsCache.CacheEntry;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Verificator of bearer access token.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class BearerTokenVerificator extends AbstractRemoteVerificator implements AccessTokenExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, BearerTokenVerificator.class);
	
	public static final String NAME = "oauth-rp";
	public static final String DESC = "Verifies OAuth access tokens against an OAuth Authorization Server";
	
	private OAuthRPProperties verificatorProperties;
	private TokenVerificatorProtocol tokenChecker;
	private PKIManagement pkiMan;
	private OAuthAccessTokenRepository tokensDAO;
	private TranslationProfile translationProfile;
	private ResultsCache cache;
	
	@Autowired
	public BearerTokenVerificator(PKIManagement pkiMan, OAuthAccessTokenRepository tokensDAO, 
			RemoteAuthnResultTranslator processor)
	{
		super(NAME, DESC, AccessTokenExchange.ID, processor);
		this.pkiMan = pkiMan;
		this.tokensDAO = tokensDAO;
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
			verificatorProperties = new OAuthRPProperties(properties, pkiMan, tokensDAO);
			tokenChecker = verificatorProperties.getTokenChecker();
			translationProfile = getTranslationProfile(verificatorProperties, CommonWebAuthnProperties.TRANSLATION_PROFILE,
					CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
			int ttl = -1;
			if (verificatorProperties.isSet(OAuthRPProperties.CACHE_TIME))
				ttl = verificatorProperties.getIntValue(OAuthRPProperties.CACHE_TIME);
			cache = new ResultsCache(ttl);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator(?)", e);
		}
		
	}

	@Override
	public AuthenticationResult checkToken(BearerAccessToken token) 
			throws AuthenticationException
	{
		try
		{
			return checkTokenInterruptible(token);
		} catch (AuthenticationException e)
		{
			throw e;
		} catch (Exception e)
		{
			throw new AuthenticationException("Authentication error ocurred", e);
		}
	}
	
	public AuthenticationResult checkTokenInterruptible(BearerAccessToken token) 
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
					return LocalAuthenticationResult.failed();
				}
				RemotelyAuthenticatedInput input = assembleBaseResult(status, 
						cached.getAttributes(), getName());
				updateInvocationContext(status);				
				return getResultForNonInteractiveAuthn(input, translationProfile);				
			} else
			{
				return LocalAuthenticationResult.failed();
			}
		}
		
		TokenStatus status = tokenChecker.checkToken(token);
		if (status.isValid())
		{
			if (!checkScopes(status))
			{
				cache.cache(token.getValue(), status, null);
				return LocalAuthenticationResult.failed();
			}
			
			AttributeFetchResult attrs;
			attrs = getUserProfileInformation(token);
			cache.cache(token.getValue(), status, attrs);
			RemotelyAuthenticatedInput input = assembleBaseResult(status, attrs, getName());
			updateInvocationContext(status);				
			return getResultForNonInteractiveAuthn(input, translationProfile);
		} else
		{
			cache.cache(token.getValue(), status, null);
			return LocalAuthenticationResult.failed();
		}
	}

	private void updateInvocationContext(TokenStatus status)
	{
		InvocationContext current = InvocationContext.getCurrent();
		current.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		current.setScopes(status.getScope().toStringList());
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
	
	private AttributeFetchResult getUserProfileInformation(BearerAccessToken accessToken) throws AuthenticationException
	{
		boolean openIdMode = verificatorProperties.isSetOpenIdMode();
		String profileEndpoint = verificatorProperties.getValue(OAuthRPProperties.PROFILE_ENDPOINT);
		AttributeFetchResult ret = new AttributeFetchResult();
		if (profileEndpoint == null)
		{
			log.debug("The profile endpoint is not defined, skipping the profile fetching");
			return ret;
		}
		UserProfileFetcher profileFetcher = openIdMode ? new OpenIdProfileFetcher() : 
			new PlainProfileFetcher();
		
		try
		{
			ret = profileFetcher.fetchProfile(accessToken, profileEndpoint, verificatorProperties, 
					new HashMap<>());
		} catch (Exception e)
		{
			throw new AuthenticationException("Can not fetch user's profile information", e);
		}
		return ret;
	}
	
	private RemotelyAuthenticatedInput assembleBaseResult(TokenStatus tokenStatus, 
			AttributeFetchResult attrs, String idpName)
	{
		RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
		
		Map<String, List<String>> attributes = attrs.getAttributes();
		for (Entry<String, List<String>> a: attributes.entrySet())
		{
			ret.addAttribute(new RemoteAttribute(a.getKey(), a.getValue().toArray()));
		}
		
		if (attributes.containsKey("sub"))
			ret.addIdentity(new RemoteIdentity(attributes.get("sub").get(0), IdentifierIdentity.ID));
		if (tokenStatus.getSubject() != null)
		{
			ret.addIdentity(new RemoteIdentity(tokenStatus.getSubject(), IdentifierIdentity.ID));
		}
		if (tokenStatus.getScope() != null)
			ret.addAttribute(new RemoteAttribute("scope", 
					tokenStatus.getScope().toStringList().toArray()));
		if (attributes.containsKey("sub") && tokenStatus.getSubject() != null && 
				!tokenStatus.getSubject().equals(attributes.get("sub").get(0)))
			log.warn("Received subject from the profile endpoint differs from the subject "
					+ "established during access token verification. "
					+ "Will use subject from verification: " + tokenStatus.getSubject() + 
					" ignored: " + attributes.get("sub").get(0));
			
		ret.setRawAttributes(attrs.getRawAttributes());
		ret.setAuthenticationTime(Instant.now());
		return ret;
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
	
	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.UNKNOWN;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<BearerTokenVerificator> factory) throws EngineException
		{
			super(NAME, DESC, factory);
		}
	}
}
