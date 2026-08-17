/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;

import com.nimbusds.jose.JWSAlgorithm;
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

public record OAuthProviderConfiguration(
		OAuthProviderKey key,
		Providers providerType,
		I18nString name,
		I18nString iconUrl,
		boolean openIdConnect,
		String federationId,
		String federationName,
		String authorizationEndpoint,
		String accessTokenEndpoint,
		List<String> userInfoEndpoints,
		String openIdDiscoveryEndpoint,
		String clientId,
		String clientSecret,
		ClientAuthnMethod clientAuthnMethod,
		String clientCredential,
		Optional<JWSAlgorithm> jwtSigningAlgorithm,
		Optional<ClientAuthnMode> clientAuthnMode,
		AccessTokenFormat accessTokenFormat,
		String truststoreName,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostNameCheckingMode,
		ClientAuthnMode clientAuthnModeForProfileAccess,
		Method clientHttpMethodForProfileAccess,
		String scopes,
		List<NameValuePair> additionalAuthzParams,
		RequestACRsMode requestACRsMode,
		List<String> requestedACRs,
		boolean requestedACRsAreEssential,
		TranslationProfile translationProfile,
		String registrationForm,
		boolean enableAssociation,
		UserProfileFetcher userAttributesResolver)
{
	public OAuthProviderConfiguration
	{
		userInfoEndpoints = userInfoEndpoints == null ? List.of() : List.copyOf(userInfoEndpoints);
		additionalAuthzParams = additionalAuthzParams == null ? List.of() : List.copyOf(additionalAuthzParams);
		requestedACRs = requestedACRs == null ? List.of() : List.copyOf(requestedACRs);
	}

	public ClientAuthnMode getClientAuthModeFallbackToDefault()
	{
		return clientAuthnMode.orElse(ClientAuthnMode.secretBasic);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private OAuthProviderKey key;
		private Providers providerType;
		private I18nString name;
		private I18nString iconUrl;
		private boolean openIdConnect;
		private String federationId;
		private String federationName;
		private String authorizationEndpoint;
		private String accessTokenEndpoint;
		private List<String> userInfoEndpoints;
		private String openIdDiscoveryEndpoint;
		private String clientId;
		private String clientSecret;
		private ClientAuthnMethod clientAuthnMethod;
		private String clientCredential;
		private Optional<JWSAlgorithm> jwtSigningAlgorithm = Optional.empty();
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

		public Builder withFederationId(String federationId)
		{
			this.federationId = federationId;
			return this;
		}

		public Builder withFederationName(String federationName)
		{
			this.federationName = federationName;
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

		public Builder withJwtSigningAlgorithm(Optional<JWSAlgorithm> jwtSigningAlgorithm)
		{
			this.jwtSigningAlgorithm = jwtSigningAlgorithm;
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
			return new OAuthProviderConfiguration(key, providerType, name, iconUrl, openIdConnect, federationId,
					federationName, authorizationEndpoint, accessTokenEndpoint, userInfoEndpoints,
					openIdDiscoveryEndpoint, clientId, clientSecret, clientAuthnMethod, clientCredential,
					jwtSigningAlgorithm, clientAuthnMode, accessTokenFormat, truststoreName, validator,
					hostNameCheckingMode, clientAuthnModeForProfileAccess, clientHttpMethodForProfileAccess, scopes,
					additionalAuthzParams, requestACRsMode, requestedACRs, requestedACRsAreEssential,
					translationProfile, registrationForm, enableAssociation, userAttributesResolver);
		}
	}
}
