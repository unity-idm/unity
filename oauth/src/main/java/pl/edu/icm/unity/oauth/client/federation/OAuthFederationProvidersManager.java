/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.config.OAuthProviders;
import pl.edu.icm.unity.oauth.client.federation.FederationEntityToProviderConverter.FederationProvider;

@Component
public class OAuthFederationProvidersManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthFederationProvidersManager.class);

	private final OAuthFederationService federationService;
	private final FederationEntityToProviderConverter converter;

	private final Map<String, InstanceState> stateByAuthenticator = new ConcurrentHashMap<>();

	public OAuthFederationProvidersManager(OAuthFederationService federationService,
			FederationEntityToProviderConverter converter)
	{
		this.federationService = federationService;
		this.converter = converter;
	}

	public void setConfiguration(String authenticatorId, String clientId, OAuthClientConfiguration config)
	{
		stateByAuthenticator.compute(authenticatorId, (id, existing) ->
		{
			if (existing != null && existing.consumerId != null)
				federationService.unregisterConsumer(existing.consumerId);

			if (!config.federationMembershipEnabled || config.federationTrustAnchorId == null)
				return new InstanceState(null, config.providers());

			try
			{
				OAuthFederationConfig fedConfig = OAuthFederationConfig.from(config);
				String consumerId = federationService.preregisterConsumer(config.federationTrustAnchorId);
				federationService.registerConsumer(consumerId, fedConfig.refreshInterval(), fedConfig,
						(chains, cid) -> onUpdatedFederation(authenticatorId, clientId, chains, config));
				return new InstanceState(consumerId, config.providers());
			} catch (ParseException e)
			{
				log.error("Failed to parse federation config for authenticator {}: {}",
						authenticatorId, e.getMessage());
				return new InstanceState(null, config.providers());
			}
		});
	}

	public void removeConfiguration(String authenticatorId)
	{
		stateByAuthenticator.compute(authenticatorId, (id, existing) ->
		{
			if (existing != null && existing.consumerId != null)
				federationService.unregisterConsumer(existing.consumerId);
			return null;
		});
	}

	public OAuthProviders getCombinedProviders(String authenticatorId)
	{
		InstanceState state = stateByAuthenticator.get(authenticatorId);
		if (state == null)
			return new OAuthProviders(List.of());
		return state.effectiveProviders();
	}

	private synchronized void onUpdatedFederation(String authenticatorId, String clientId,
			List<TrustChain> chains, OAuthClientConfiguration config)
	{
		List<FederationProvider> fromFederation = converter.convert(chains, clientId,
				config.authenticationCredential, config.federationTranslationProfile,
				config.federationRegistrationForm, config.defaultEnableAssociation);
		log.debug("Updated {} federation providers for authenticator {}", fromFederation.size(), authenticatorId);

		InstanceState state = stateByAuthenticator.get(authenticatorId);
		if (state == null)
			return;

		Map<OAuthProviderKey, Instant> expiryMap = new ConcurrentHashMap<>();
		fromFederation.forEach(fp -> expiryMap.put(fp.config().key, fp.expiresAt()));

		OAuthProviders combined = config.providers()
				.replaceFederation(fromFederation.stream().map(FederationProvider::config).toList())
				.overrideWithStatic(config.providers());
		stateByAuthenticator.put(authenticatorId,
				new InstanceState(state.consumerId, combined, expiryMap));
	}

	private record InstanceState(
			String consumerId,
			OAuthProviders combinedProviders,
			Map<OAuthProviderKey, Instant> federationExpiry)
	{
		InstanceState(String consumerId, OAuthProviders combinedProviders)
		{
			this(consumerId, combinedProviders, Collections.emptyMap());
		}

		OAuthProviders effectiveProviders()
		{
			Instant now = Instant.now();
			List<pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration> nonExpired =
					combinedProviders.getAll().stream()
							.filter(p -> !p.key.isFromFederation()
									|| !federationExpiry.containsKey(p.key)
									|| now.isBefore(federationExpiry.get(p.key)))
							.toList();
			return new OAuthProviders(nonExpired);
		}
	}
}
