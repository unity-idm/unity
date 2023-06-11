/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.oauth.client.console;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;

/**
 * Base OAuth configuration bean
 * @author P.Piernik
 *
 */
public class OAuthBaseConfiguration
{
	private String clientId;
	private String clientSecret;
	private ClientAuthnMode clientAuthenticationMode;
	private ClientAuthnMode clientAuthenticationModeForProfile;
	private ClientHttpMethod clientHttpMethodForProfileAccess;
	private ServerHostnameCheckingMode clientHostnameChecking;
	private String clientTrustStore;
	private TranslationProfile translationProfile;
	private String profileEndpoint;
	
	public OAuthBaseConfiguration()
	{
		translationProfile = TranslationProfileGenerator.generateEmbeddedEmptyInputProfile();
		setClientHostnameChecking(ServerHostnameCheckingMode.FAIL);
		setClientHttpMethodForProfileAccess(ClientHttpMethod.get);
		setClientAuthenticationMode(ClientAuthnMode.secretBasic);
		setClientAuthenticationModeForProfile(ClientAuthnMode.secretBasic);
	}
	
	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public String getClientSecret()
	{
		return clientSecret;
	}

	public void setClientSecret(String clientSecret)
	{
		this.clientSecret = clientSecret;
	}

	public ClientAuthnMode getClientAuthenticationMode()
	{
		return clientAuthenticationMode;
	}

	public void setClientAuthenticationMode(ClientAuthnMode clientAuthenticationMode)
	{
		this.clientAuthenticationMode = clientAuthenticationMode;
	}

	public ClientAuthnMode getClientAuthenticationModeForProfile()
	{
		return clientAuthenticationModeForProfile;
	}

	public void setClientAuthenticationModeForProfile(ClientAuthnMode clientAuthenticationModeForProfile)
	{
		this.clientAuthenticationModeForProfile = clientAuthenticationModeForProfile;
	}

	public ClientHttpMethod getClientHttpMethodForProfileAccess()
	{
		return clientHttpMethodForProfileAccess;
	}

	public void setClientHttpMethodForProfileAccess(ClientHttpMethod clientHttpMethodForProfileAccess)
	{
		this.clientHttpMethodForProfileAccess = clientHttpMethodForProfileAccess;
	}

	public ServerHostnameCheckingMode getClientHostnameChecking()
	{
		return clientHostnameChecking;
	}

	public void setClientHostnameChecking(ServerHostnameCheckingMode clientHostnameChecking)
	{
		this.clientHostnameChecking = clientHostnameChecking;
	}

	public String getClientTrustStore()
	{
		return clientTrustStore;
	}

	public void setClientTrustStore(String clientTrustStore)
	{
		this.clientTrustStore = clientTrustStore;
	}

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public String getProfileEndpoint()
	{
		return profileEndpoint;
	}

	public void setProfileEndpoint(String profileEndpoint)
	{
		this.profileEndpoint = profileEndpoint;
	}	
}
