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
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher.ClientAuthnMode;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
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
	private String translationProfile;
	
	public BearerTokenVerificator(String name, String description,
			TranslationProfileManagement profileManagement,
			PKIManagement pkiMan,
			InputTranslationEngine trEngine)
	{
		super(name, description, AccessTokenExchange.ID, profileManagement, trEngine);
		this.pkiMan = pkiMan;
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
			verificatorProperties = new OAuthRPProperties(properties, pkiMan);
			tokenChecker = verificatorProperties.getTokenChecker();
			translationProfile = verificatorProperties.getValue(OAuthRPProperties.TRANSLATION_PROFILE);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth RP verificator(?)", e);
		}
		
	}

	@Override
	public AuthenticationResult checkToken(BearerAccessToken token) throws AuthenticationException
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
	
	public AuthenticationResult checkTokenInterruptible(BearerAccessToken token) throws Exception
	{
		TokenStatus status = tokenChecker.checkToken(token);
		if (status.isValid())
		{
			Map<String, String> attrs = getUserProfileInformation(token);
			RemotelyAuthenticatedInput input = assembleBaseResult(status, attrs, getName());
			return getResult(input, translationProfile);
		} else
		{
			return new AuthenticationResult(Status.deny, null, null);
		}
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
