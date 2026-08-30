/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.TrustChainConstraints;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.engine.api.files.SsrfProtection;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.client.federation.TlsEntityStatementRetriever;
import pl.edu.icm.unity.stdext.attr.ImageAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

@Component
public class FederatedOAuthClientService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, FederatedOAuthClientService.class);

	private final Map<String, CachedChain> chainCache = new ConcurrentHashMap<>();
	private final Map<String, URI> lastFetchedLogoUri = new ConcurrentHashMap<>();
	private final Map<String, LogoRefreshCoordinator> logoRefreshCoordinators = new ConcurrentHashMap<>();

	private final EntityManagement identitiesMan;
	private final AttributesManagement attributesMan;
	private final GroupsManagement groupsMan;
	private final AttributeSupport attributeSupport;
	private final AttributeTypeSupport attrTypeSupport;
	private final URIAccessService uriAccessService;
	private final ExecutorsService executorsService;
	private final Duration logoConnectionTimeout;
	private final Duration logoSocketReadTimeout;
	private final long maxLogoSizeBytes;
	private final boolean restrictInternalDestinations;

	public FederatedOAuthClientService(
			@Qualifier("insecure") EntityManagement identitiesMan,
			@Qualifier("insecure") AttributesManagement attributesMan,
			@Qualifier("insecure") GroupsManagement groupsMan,
			AttributeSupport attributeSupport,
			AttributeTypeSupport attrTypeSupport,
			URIAccessService uriAccessService,
			ExecutorsService executorsService,
			UnityServerConfiguration conf,
			Environment environment)
	{
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.groupsMan = groupsMan;
		this.attributeSupport = attributeSupport;
		this.attrTypeSupport = attrTypeSupport;
		this.uriAccessService = uriAccessService;
		this.executorsService = executorsService;
		this.logoConnectionTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_CONNECTION_TIMEOUT));
		this.logoSocketReadTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT));
		this.maxLogoSizeBytes = conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_SIZE);
		this.restrictInternalDestinations = !environment.acceptsProfiles(Profiles.of(UnityServerConfiguration.PROFILE_TEST));
	}

	public record FederatedClientResolution(long entityId, JWKSet jwks) {}

	public FederatedClientResolution resolveAndRegister(String clientId, OAuthASFederationConfig config)
			throws Exception
	{
		chainCache.entrySet().removeIf(e -> e.getValue().isExpired());
		CachedChain cached = chainCache.get(clientId);
		TrustChain chain;
		if (cached != null && !cached.isExpired())
		{
			chain = cached.chain();
		} else
		{
			chain = resolveAndCacheChain(clientId, config);
			if (chain == null)
				throw new AuthenticationException("Failed to resolve trust chain for " + clientId);
			chainCache.put(clientId, new CachedChain(chain, chain.resolveExpirationTime().toInstant()));
		}

		JSONObject rawRpJson = chain.getLeafConfiguration().getClaimsSet()
				.getMetadata(EntityType.OPENID_RELYING_PARTY);
		if (rawRpJson == null)
			throw new AuthenticationException(
					"No openid_relying_party metadata in federation leaf entity for " + clientId);
		MetadataPolicy policy = chain.resolveCombinedMetadataPolicy(EntityType.OPENID_RELYING_PARTY);
		OIDCClientMetadata rpMeta = OIDCClientMetadata.parse(policy.apply(rawRpJson));
		JWKSet jwkSet = rpMeta.getJWKSet();
		if (jwkSet == null || jwkSet.getKeys().isEmpty())
			throw new AuthenticationException(
					"Empty JWKS in openid_relying_party metadata in federation leaf entity for " + clientId);

		long entityId = resolveOrRegister(clientId, rpMeta, config);
		return new FederatedClientResolution(entityId, jwkSet);
	}

	private long resolveOrRegister(String clientId, OIDCClientMetadata metadata, OAuthASFederationConfig config) throws Exception
	{
		try
		{
			var entity = identitiesMan.getEntity(new EntityParam(new IdentityTaV(UsernameIdentity.ID, clientId)));
			long entityId = entity.getId();
			refreshAttributesIfChanged(entityId, clientId, metadata, config);
			log.info("Federation client {} found in DB, entityId={}", clientId, entityId);
			return entityId;
		} catch (IllegalArgumentException e)
		{
			return registerNewClient(clientId, metadata, config);
		}
	}

	private long registerNewClient(String clientId, OIDCClientMetadata metadata, OAuthASFederationConfig config)
			throws EngineException
	{
		log.info("Auto-registering federation client {}", clientId);
		String oauthGroup = config.clientsGroup();
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, clientId);
		identity.setRemoteIdp(config.trustAnchorId());
		long entityId = identitiesMan.addEntity(identity, EntityState.valid).getEntityId();
		groupsMan.addMemberFromParent(oauthGroup, new EntityParam(entityId));
		for (Attribute attr : FederationClientAttributesMapper.toOAuthAttributes(metadata, oauthGroup, clientId, config.clientDefaults()))
			attributesMan.setAttribute(new EntityParam(entityId), attr);
		setEntityDisplayedName(entityId, FederationClientAttributesMapper.toDisplayName(metadata, clientId));
		updateLogoIfChanged(clientId, entityId, oauthGroup, metadata.getLogoURI(), config.truststoreName());
		log.info("Federation client {} registered as entityId={}", clientId, entityId);
		return entityId;
	}

	private void refreshAttributesIfChanged(long entityId, String clientId, OIDCClientMetadata metadata, OAuthASFederationConfig config)
	{
		String oauthGroup = config.clientsGroup();
		List<Attribute> fresh = FederationClientAttributesMapper.toOAuthAttributes(metadata, oauthGroup, clientId, config.clientDefaults());
		try
		{
			Collection<AttributeExt> existing = attributesMan.getAllAttributes(
					new EntityParam(entityId), true, oauthGroup, null, false);
			Map<String, AttributeExt> existingMap = new java.util.HashMap<>();
			existing.forEach(a -> existingMap.put(a.getName(), a));
			for (Attribute attr : fresh)
			{
				AttributeExt current = existingMap.get(attr.getName());
				if (current == null || !current.getValues().equals(attr.getValues()))
					attributesMan.setAttribute(new EntityParam(entityId), attr);
			}
			setEntityDisplayedName(entityId, FederationClientAttributesMapper.toDisplayName(metadata, clientId));
			updateLogoIfChangedAsync(clientId, entityId, oauthGroup, metadata.getLogoURI(), config.truststoreName());
		} catch (Exception e)
		{
			log.warn("Failed to refresh attributes for federation client entityId={}: {}", entityId, e.getMessage());
		}
	}

	private void setEntityDisplayedName(long entityId, String displayName)
	{
		if (displayName == null)
			return;
		try
		{
			AttributeType atType = attributeSupport.getAttributeTypeWithSingeltonMetadata(
					EntityNameMetadataProvider.NAME);
			if (atType == null)
				return;
			attributesMan.setAttribute(new EntityParam(entityId),
						StringAttribute.of(atType.getName(), "/", displayName));
		} catch (Exception e)
		{
			log.warn("Failed to set displayedName for federation client entityId={}: {}", entityId, e.getMessage());
		}
	}

	/**
	 * Like {@link #updateLogoIfChanged(String, long, String, URI)}, but fetches the logo in the background
	 * instead of blocking the caller. Used when refreshing an already-registered client (which already has
	 * a logo attribute value from a previous fetch), as opposed to first-time registration where the caller
	 * needs the logo to be present by the time it returns. Concurrent requests for the same client are
	 * coalesced: while a fetch is in progress, a newer request replaces any not-yet-started one queued
	 * behind it and runs immediately once the current fetch finishes - so a client whose metadata keeps
	 * changing during a slow fetch ends up with the latest logo, not whichever fetch happened to start.
	 */
	private void updateLogoIfChangedAsync(String clientId, long entityId, String oauthGroup, URI logoUri, String truststoreName)
	{
		if (logoUri == null || logoUri.equals(lastFetchedLogoUri.get(clientId)))
			return;
		logoRefreshCoordinators.computeIfAbsent(clientId, k -> new LogoRefreshCoordinator())
				.submit(new PendingLogoUpdate(clientId, entityId, oauthGroup, logoUri, truststoreName));
	}

	private record PendingLogoUpdate(
			String clientId,
			long entityId,
			String oauthGroup,
			URI logoUri,
			String truststoreName)
	{
	}

	private final class LogoRefreshCoordinator
	{
		private boolean running;
		private PendingLogoUpdate pending;

		synchronized void submit(PendingLogoUpdate update)
		{
			if (running)
			{
				pending = update;
				return;
			}
			running = true;
			dispatch(update);
		}

		private void dispatch(PendingLogoUpdate update)
		{
			executorsService.getExecutionService().execute(() ->
			{
				try
				{
					updateLogoIfChanged(update.clientId(), update.entityId(), update.oauthGroup(), update.logoUri(),
							update.truststoreName());
				}
				finally
				{
					onFinished();
				}
			});
		}

		private synchronized void onFinished()
		{
			PendingLogoUpdate next = pending;
			pending = null;
			if (next == null)
			{
				running = false;
				return;
			}
			dispatch(next);
		}
	}

	private void updateLogoIfChanged(String clientId, long entityId, String oauthGroup, URI logoUri, String truststoreName)
	{
		if (logoUri == null)
			return;
		if (logoUri.equals(lastFetchedLogoUri.get(clientId)))
			return;
		try
		{
			if (restrictInternalDestinations)
				SsrfProtection.assertNoInternalDestination(logoUri);
			RemoteFileData fileData = uriAccessService.readURL(logoUri, truststoreName,
					logoConnectionTimeout, logoSocketReadTimeout, 0, maxLogoSizeBytes);
			ImageType imageType = ImageType.fromMimeType(fileData.mimeType);
			UnityImage image = new UnityImage(fileData.getContents(), imageType);
			ImageAttributeSyntax syntax = (ImageAttributeSyntax) attrTypeSupport
					.getSyntax(attrTypeSupport.getType(OAuthSystemAttributesProvider.CLIENT_LOGO));
			image.scaleDown(syntax.getConfig().getMaxWidth(), syntax.getConfig().getMaxHeight());
			attributesMan.setAttribute(new EntityParam(entityId),
					ImageAttribute.of(OAuthSystemAttributesProvider.CLIENT_LOGO, oauthGroup, image));
			lastFetchedLogoUri.put(clientId, logoUri);
			log.info("Logo updated for federation client entityId={}", entityId);
		} catch (Exception e)
		{
			log.warn("Failed to fetch/update logo for federation client entityId={} from {}: {}", entityId, logoUri,
					e.getMessage());
		}
	}

	TrustChain resolveAndCacheChain(String clientId, OAuthASFederationConfig config) throws Exception
	{
		TrustChainResolver resolver = new TrustChainResolver(
				Map.of(new EntityID(config.trustAnchorId()), config.trustAnchorJwks()),
				TrustChainConstraints.NO_CONSTRAINTS,
				new TlsEntityStatementRetriever(config.validator(),
						config.hostnameCheckingMode() != null
								? config.hostnameCheckingMode()
								: ServerHostnameCheckingMode.FAIL));
		TrustChainSet chains = resolver.resolveTrustChains(new EntityID(clientId));
		TrustChain chain = chains.getShortest();
		if (chain == null)
			throw new AuthenticationException("No valid trust chain found for " + clientId);
		return chain;
	}

	public boolean isKnownFederationClient(String clientId)
	{
		CachedChain cached = chainCache.get(clientId);
		return cached != null && !cached.isExpired();
	}

	public void invalidateChainCache()
	{
		chainCache.clear();
		lastFetchedLogoUri.clear();
	}

	public record CachedChain(TrustChain chain, Instant expiresAt)
	{
		boolean isExpired()
		{
			return Instant.now().isAfter(expiresAt);
		}
	}
}
