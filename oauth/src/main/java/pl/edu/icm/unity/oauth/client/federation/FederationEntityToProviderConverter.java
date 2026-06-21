/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import net.minidev.json.JSONObject;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.config.OAuthFederationConfig;
import pl.edu.icm.unity.oauth.client.config.OAuthFederationProviderDefaults;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.config.RequestACRsMode;
import pl.edu.icm.unity.oauth.client.profile.OpenIdProfileFetcher;

@Component
public class FederationEntityToProviderConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, FederationEntityToProviderConverter.class);

	record FederationProvider(OAuthProviderConfiguration config, Instant expiresAt) {}

	public List<FederationProvider> convert(List<TrustChain> chains, String clientId,
			String clientCredential, boolean enableAssociation, OAuthFederationProviderDefaults providerDefaults,
			OAuthFederationConfig federationConfig)
	{
		return chains.stream()
				.map(chain -> convertOne(chain, clientId, clientCredential, enableAssociation, providerDefaults,
						federationConfig))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	private Optional<FederationProvider> convertOne(TrustChain chain, String clientId, String clientCredential,
			boolean enableAssociation, OAuthFederationProviderDefaults providerDefaults, OAuthFederationConfig federationConfig)
	{
		try
		{
			JSONObject rawOpJson = chain.getLeafConfiguration().getClaimsSet()
					.getMetadata(EntityType.OPENID_PROVIDER);
			if (rawOpJson == null)
				return Optional.empty();
			MetadataPolicy policy = chain.resolveCombinedMetadataPolicy(EntityType.OPENID_PROVIDER);
			OIDCProviderMetadata opMeta = OIDCProviderMetadata.parse(policy.apply(rawOpJson));

			
			String entityId = chain.getLeafConfiguration().getClaimsSet().getSubjectEntityID().getValue();
			OAuthProviderKey key = OAuthProviderKey.fromFederationEntity(entityId);

			String authEndpoint = opMeta.getAuthorizationEndpointURI() != null
					? opMeta.getAuthorizationEndpointURI().toString() : null;
			String tokenEndpoint = opMeta.getTokenEndpointURI() != null
					? opMeta.getTokenEndpointURI().toString() : null;
			String userInfoEndpoint = opMeta.getUserInfoEndpointURI() != null
					? opMeta.getUserInfoEndpointURI().toString() : null;
			String discoveryEndpoint = opMeta.getIssuer() != null
					? opMeta.getIssuer().getValue() + "/.well-known/openid-configuration" : null;
			String scopes = opMeta.getScopes() != null ? opMeta.getScopes().toString() : "openid";
			String name = opMeta.getIssuer() != null ? opMeta.getIssuer().getValue() : entityId;
			URI logoUri = opMeta.getCustomURIParameter("logo_uri");

			String federationId = chain.getTrustAnchorEntityID().getValue();
			String federationName = resolveFederationName(chain, federationId);

			Instant expiresAt = chain.resolveExpirationTime().toInstant();
			OAuthProviderConfiguration providerConfig = OAuthProviderConfiguration.builder()
					.withKey(key)
					.withName(new I18nString(name))
					.withIconUrl(logoUri != null ? new I18nString(logoUri.toString()) : null)
					.withFederationId(federationId)
					.withFederationName(federationName)
					.withProviderType(Providers.custom)
					.withOpenIdConnect(true)
					.withAuthorizationEndpoint(authEndpoint)
					.withAccessTokenEndpoint(tokenEndpoint)
					.withUserInfoEndpoints(userInfoEndpoint != null ? List.of(userInfoEndpoint) : List.of())
					.withOpenIdDiscoveryEndpoint(discoveryEndpoint)
					.withScopes(scopes)
					.withAccessTokenFormat(AccessTokenFormat.standard)
					.withClientId(clientId)
					.withClientAuthnMethod(ClientAuthnMethod.private_key_jwt)
					.withClientCredential(clientCredential)
					.withRequestACRsMode(RequestACRsMode.NONE)
					.withRequestedACRs(List.of())
					.withRequestedACRsAreEssential(false)
					.withRegistrationForm(providerDefaults.registrationForm)
					.withEnableAssociation(enableAssociation)
					.withTranslationProfile(providerDefaults.translationProfile)
					.withUserAttributesResolver(new OpenIdProfileFetcher())
					.withTruststoreName(federationConfig.truststore)
					.withValidator(federationConfig.validator)
					.withHostNameCheckingMode(federationConfig.hostnameCheckingMode)
					.withJwtSigningAlgorithm(federationConfig.jwtSigningAlgorithm)
					.build();
			return Optional.of(new FederationProvider(providerConfig, expiresAt));
		} catch (Exception e)
		{
			log.warn("Failed to convert federation entity to provider config", e);
			return Optional.empty();
		}
	}

	private String resolveFederationName(TrustChain chain, String federationId)
	{
		List<EntityStatement> superiors = chain.getSuperiorStatements();
		if (superiors.isEmpty())
			return federationId;
		EntityStatement trustAnchorStatement = superiors.get(superiors.size() - 1);
		FederationEntityMetadata fedMeta = trustAnchorStatement.getClaimsSet().getFederationEntityMetadata();
		if (fedMeta != null && fedMeta.getOrganizationName() != null)
			return fedMeta.getOrganizationName();
		return federationId;
	}
}
