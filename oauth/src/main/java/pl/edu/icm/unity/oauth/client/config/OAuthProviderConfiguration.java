/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;

import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;

public class OAuthProviderConfiguration
{
	public final OAuthProviderKey key;
	public final Providers providerType;
	public final I18nString name;
	public final I18nString iconUrl;
	public final boolean openIdConnect;

	public final String authorizationEndpoint;
	public final String accessTokenEndpoint;
	public final List<String> userInfoEndpoints;
	public final String openIdDiscoveryEndpoint;

	public final String clientId;
	public final String clientSecret;
	public final ClientAuthnMethod clientAuthnMethod;
	public final String clientCredential;
	public final Optional<ClientAuthnMode> clientAuthnMode;
	public final AccessTokenFormat accessTokenFormat;

	public final String truststoreName;
	public final X509CertChainValidator validator;
	public final ServerHostnameCheckingMode hostNameCheckingMode;
	public final ClientAuthnMode clientAuthnModeForProfileAccess;
	public final Method clientHttpMethodForProfileAccess;

	public final String scopes;
	public final List<NameValuePair> additionalAuthzParams;

	public final RequestACRsMode requestACRsMode;
	public final List<String> requestedACRs;
	public final boolean requestedACRsAreEssential;

	public final TranslationProfile translationProfile;
	public final String registrationForm;
	public final boolean enableAssociation;
	public final UserProfileFetcher userAttributesResolver;

	private OAuthProviderConfiguration(Builder builder)
	{
		this.key = builder.key;
		this.providerType = builder.providerType;
		this.name = builder.name;
		this.iconUrl = builder.iconUrl;
		this.openIdConnect = builder.openIdConnect;
		this.authorizationEndpoint = builder.authorizationEndpoint;
		this.accessTokenEndpoint = builder.accessTokenEndpoint;
		this.userInfoEndpoints = builder.userInfoEndpoints == null
				? Collections.emptyList()
				: List.copyOf(builder.userInfoEndpoints);
		this.openIdDiscoveryEndpoint = builder.openIdDiscoveryEndpoint;
		this.clientId = builder.clientId;
		this.clientSecret = builder.clientSecret;
		this.clientAuthnMethod = builder.clientAuthnMethod;
		this.clientCredential = builder.clientCredential;
		this.clientAuthnMode = builder.clientAuthnMode;
		this.accessTokenFormat = builder.accessTokenFormat;
		this.truststoreName = builder.truststoreName;
		this.validator = builder.validator;
		this.hostNameCheckingMode = builder.hostNameCheckingMode;
		this.clientAuthnModeForProfileAccess = builder.clientAuthnModeForProfileAccess;
		this.clientHttpMethodForProfileAccess = builder.clientHttpMethodForProfileAccess;
		this.scopes = builder.scopes;
		this.additionalAuthzParams = builder.additionalAuthzParams == null
				? Collections.emptyList()
				: List.copyOf(builder.additionalAuthzParams);
		this.requestACRsMode = builder.requestACRsMode;
		this.requestedACRs = builder.requestedACRs == null
				? Collections.emptyList()
				: List.copyOf(builder.requestedACRs);
		this.requestedACRsAreEssential = builder.requestedACRsAreEssential;
		this.translationProfile = builder.translationProfile;
		this.registrationForm = builder.registrationForm;
		this.enableAssociation = builder.enableAssociation;
		this.userAttributesResolver = builder.userAttributesResolver;
	}

	public ClientAuthnMode getClientAuthModeFallbackToDefault()
	{
		return clientAuthnMode.orElse(ClientAuthnMode.secretBasic);
	}
	
	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(accessTokenEndpoint, accessTokenFormat, additionalAuthzParams, authorizationEndpoint,
				clientAuthnMethod, clientAuthnMode, clientAuthnModeForProfileAccess, clientCredential,
				clientHttpMethodForProfileAccess, clientId, clientSecret, enableAssociation, hostNameCheckingMode,
				iconUrl, key, name, openIdConnect, openIdDiscoveryEndpoint, providerType, registrationForm,
				requestACRsMode, requestedACRs, requestedACRsAreEssential, scopes, translationProfile,
				truststoreName, userInfoEndpoints, validator);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OAuthProviderConfiguration other = (OAuthProviderConfiguration) obj;
		return Objects.equals(accessTokenEndpoint, other.accessTokenEndpoint)
				&& accessTokenFormat == other.accessTokenFormat
				&& Objects.equals(additionalAuthzParams, other.additionalAuthzParams)
				&& Objects.equals(authorizationEndpoint, other.authorizationEndpoint)
				&& clientAuthnMethod == other.clientAuthnMethod
				&& Objects.equals(clientAuthnMode, other.clientAuthnMode)
				&& clientAuthnModeForProfileAccess == other.clientAuthnModeForProfileAccess
				&& Objects.equals(clientCredential, other.clientCredential)
				&& Objects.equals(clientHttpMethodForProfileAccess, other.clientHttpMethodForProfileAccess)
				&& Objects.equals(clientId, other.clientId)
				&& Objects.equals(clientSecret, other.clientSecret)
				&& enableAssociation == other.enableAssociation
				&& hostNameCheckingMode == other.hostNameCheckingMode
				&& Objects.equals(iconUrl, other.iconUrl)
				&& Objects.equals(key, other.key)
				&& Objects.equals(name, other.name)
				&& openIdConnect == other.openIdConnect
				&& Objects.equals(openIdDiscoveryEndpoint, other.openIdDiscoveryEndpoint)
				&& providerType == other.providerType
				&& Objects.equals(registrationForm, other.registrationForm)
				&& requestACRsMode == other.requestACRsMode
				&& Objects.equals(requestedACRs, other.requestedACRs)
				&& requestedACRsAreEssential == other.requestedACRsAreEssential
				&& Objects.equals(scopes, other.scopes)
				&& Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(truststoreName, other.truststoreName)
				&& Objects.equals(userInfoEndpoints, other.userInfoEndpoints)
				&& Objects.equals(validator, other.validator);
	}

	public static final class Builder
	{
		private OAuthProviderKey key;
		private Providers providerType;
		private I18nString name;
		private I18nString iconUrl;
		private boolean openIdConnect;
		private String authorizationEndpoint;
		private String accessTokenEndpoint;
		private List<String> userInfoEndpoints;
		private String openIdDiscoveryEndpoint;
		private String clientId;
		private String clientSecret;
		private ClientAuthnMethod clientAuthnMethod;
		private String clientCredential;
		private Optional<ClientAuthnMode> clientAuthnMode = Optional.empty();
		private AccessTokenFormat accessTokenFormat;
		private String truststoreName;
		private X509CertChainValidator validator;
		private ServerHostnameCheckingMode hostNameCheckingMode;
		private ClientAuthnMode clientAuthnModeForProfileAccess;
		private Method clientHttpMethodForProfileAccess;
		private String scopes;
		private List<NameValuePair> additionalAuthzParams;
		private RequestACRsMode requestACRsMode;
		private List<String> requestedACRs;
		private boolean requestedACRsAreEssential;
		private TranslationProfile translationProfile;
		private String registrationForm;
		private boolean enableAssociation;
		private UserProfileFetcher userAttributesResolver;

		private Builder() {}

		public Builder withKey(OAuthProviderKey key)
		{
			this.key = key;
			return this;
		}

		public Builder withProviderType(Providers providerType)
		{
			this.providerType = providerType;
			return this;
		}

		public Builder withName(I18nString name)
		{
			this.name = name;
			return this;
		}

		public Builder withIconUrl(I18nString iconUrl)
		{
			this.iconUrl = iconUrl;
			return this;
		}

		public Builder withOpenIdConnect(boolean openIdConnect)
		{
			this.openIdConnect = openIdConnect;
			return this;
		}

		public Builder withAuthorizationEndpoint(String authorizationEndpoint)
		{
			this.authorizationEndpoint = authorizationEndpoint;
			return this;
		}

		public Builder withAccessTokenEndpoint(String accessTokenEndpoint)
		{
			this.accessTokenEndpoint = accessTokenEndpoint;
			return this;
		}

		public Builder withUserInfoEndpoints(List<String> userInfoEndpoints)
		{
			this.userInfoEndpoints = userInfoEndpoints;
			return this;
		}

		public Builder withOpenIdDiscoveryEndpoint(String openIdDiscoveryEndpoint)
		{
			this.openIdDiscoveryEndpoint = openIdDiscoveryEndpoint;
			return this;
		}

		public Builder withClientId(String clientId)
		{
			this.clientId = clientId;
			return this;
		}

		public Builder withClientSecret(String clientSecret)
		{
			this.clientSecret = clientSecret;
			return this;
		}

		public Builder withClientAuthnMethod(ClientAuthnMethod clientAuthnMethod)
		{
			this.clientAuthnMethod = clientAuthnMethod;
			return this;
		}

		public Builder withClientCredential(String clientCredential)
		{
			this.clientCredential = clientCredential;
			return this;
		}

		public Builder withClientAuthnMode(Optional<ClientAuthnMode> clientAuthnMode)
		{
			this.clientAuthnMode = clientAuthnMode;
			return this;
		}

		public Builder withAccessTokenFormat(AccessTokenFormat accessTokenFormat)
		{
			this.accessTokenFormat = accessTokenFormat;
			return this;
		}

		public Builder withTruststoreName(String truststoreName)
		{
			this.truststoreName = truststoreName;
			return this;
		}

		public Builder withValidator(X509CertChainValidator validator)
		{
			this.validator = validator;
			return this;
		}

		public Builder withHostNameCheckingMode(ServerHostnameCheckingMode hostNameCheckingMode)
		{
			this.hostNameCheckingMode = hostNameCheckingMode;
			return this;
		}

		public Builder withClientAuthnModeForProfileAccess(ClientAuthnMode clientAuthnModeForProfileAccess)
		{
			this.clientAuthnModeForProfileAccess = clientAuthnModeForProfileAccess;
			return this;
		}

		public Builder withClientHttpMethodForProfileAccess(Method clientHttpMethodForProfileAccess)
		{
			this.clientHttpMethodForProfileAccess = clientHttpMethodForProfileAccess;
			return this;
		}

		public Builder withScopes(String scopes)
		{
			this.scopes = scopes;
			return this;
		}

		public Builder withAdditionalAuthzParams(List<NameValuePair> additionalAuthzParams)
		{
			this.additionalAuthzParams = additionalAuthzParams;
			return this;
		}

		public Builder withRequestACRsMode(RequestACRsMode requestACRsMode)
		{
			this.requestACRsMode = requestACRsMode;
			return this;
		}

		public Builder withRequestedACRs(List<String> requestedACRs)
		{
			this.requestedACRs = requestedACRs;
			return this;
		}

		public Builder withRequestedACRsAreEssential(boolean requestedACRsAreEssential)
		{
			this.requestedACRsAreEssential = requestedACRsAreEssential;
			return this;
		}

		public Builder withTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public Builder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		public Builder withEnableAssociation(boolean enableAssociation)
		{
			this.enableAssociation = enableAssociation;
			return this;
		}

		public Builder withUserAttributesResolver(UserProfileFetcher userAttributesResolver)
		{
			this.userAttributesResolver = userAttributesResolver;
			return this;
		}

		public OAuthProviderConfiguration build()
		{
			return new OAuthProviderConfiguration(this);
		}
	}
}
