/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.TrustChainConstraints;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.minidev.json.JSONObject;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.federation.TlsEntityStatementRetriever;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@PrototypeComponent
public class FederatedPrivateKeyJwtVerificator extends AbstractVerificator implements ClientAssertionExchange
{
	public static final String NAME = "private-key-jwt-federated";
	public static final String DESC = "Verifies OAuth2 client JWT assertions using OpenID Federation";
	private static final String[] IDENTITY_TYPES = {UsernameIdentity.ID};
	private static final ResolvableError GENERIC_ERROR =
			new ResolvableError("FederatedPrivateKeyJwtVerificator.invalidAssertion");
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, FederatedPrivateKeyJwtVerificator.class);

	private final OAuthEndpointsCoordinator coordinator;
	private final AttributesManagement attributesManagement;
	private final JwtClientAssertionVerifier jwtVerifier = new JwtClientAssertionVerifier();
	final Map<String, CachedChain> chainCache = new ConcurrentHashMap<>();

	@Autowired
	public FederatedPrivateKeyJwtVerificator(OAuthEndpointsCoordinator coordinator,
			@Qualifier("insecure") AttributesManagement attributesManagement)
	{
		super(NAME, DESC, ClientAssertionExchange.ID);
		this.coordinator = coordinator;
		this.attributesManagement = attributesManagement;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "{}";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		chainCache.clear();
	}

	@Override
	public AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri)
	{
		OAuthASFederationConfig federationConfig = coordinator.getFederationConfig(tokenEndpointUri.toString())
				.orElse(null);
		if (federationConfig == null || !federationConfig.membershipEnabled())
		{
			log.debug("Federation membership not enabled for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		if (federationConfig.trustAnchorId() == null || federationConfig.trustAnchorJwks() == null)
		{
			log.warn("Federation trust anchor not configured for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		SignedJWT jwt;
		try
		{
			jwt = SignedJWT.parse(clientAssertion);
		} catch (Exception e)
		{
			log.debug("Failed to parse client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		String clientId;
		try
		{
			clientId = jwt.getJWTClaimsSet().getSubject();
			if (clientId == null)
			{
				log.debug("client_assertion JWT has no sub claim");
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
		} catch (Exception e)
		{
			log.debug("Failed to read claims from client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveIdentity(clientId, IDENTITY_TYPES, null);
		} catch (Exception e)
		{
			log.info("Client entity not found for client_id: {}", clientId);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		try
		{
			Collection<AttributeExt> authnMethodAttrs = attributesManagement.getAttributes(
					new EntityParam(resolved.getEntityId()), federationConfig.clientsGroup(),
					OAuthSystemAttributesProvider.CLIENT_AUTHN_METHOD);
			if (authnMethodAttrs.isEmpty() || !ClientAuthnMethod.private_key_jwt.toString()
					.equals(authnMethodAttrs.iterator().next().getValues().get(0)))
			{
				log.info("Client {} does not have private_key_jwt authentication method configured", clientId);
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
		} catch (Exception e)
		{
			log.warn("Cannot read authentication method attribute for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		JWKSet jwkSet;
		try
		{
			jwkSet = resolveJwksFromFederation(clientId, federationConfig);
		} catch (Exception e)
		{
			log.info("Failed to resolve JWKS from federation for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		try
		{
			jwtVerifier.verifyJwt(jwt, jwkSet, tokenEndpointUri, clientId);
		} catch (AuthenticationException e)
		{
			log.info("JWT assertion verification failed for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), clientId, null);
		return LocalAuthenticationResult.successful(ae, getAuthenticationMethod());
	}

	JWKSet resolveJwksFromFederation(String clientId, OAuthASFederationConfig config) throws Exception
	{
		chainCache.entrySet().removeIf(e -> e.getValue().isExpired());
		CachedChain cached = chainCache.get(clientId);
		if (cached != null && !cached.isExpired())
			return extractRpJwksFromChain(cached.chain(), clientId);

		TrustChain chain = resolveChain(clientId, config);
		chainCache.put(clientId, new CachedChain(chain, chain.resolveExpirationTime().toInstant()));
		return extractRpJwksFromChain(chain, clientId);
	}

	TrustChain resolveChain(String clientId, OAuthASFederationConfig config) throws Exception
	{
		TrustChainResolver resolver = new TrustChainResolver(
				Map.of(new EntityID(config.trustAnchorId()), config.trustAnchorJwks()),
				TrustChainConstraints.NO_CONSTRAINTS,
				new TlsEntityStatementRetriever(config.validator(),
						config.hostnameCheckingMode() != null
								? config.hostnameCheckingMode()
								: ServerHostnameCheckingMode.FAIL));
		TrustChainSet chains = resolver.resolveTrustChains(new EntityID(clientId));
		return chains.getShortest();
	}

	record CachedChain(TrustChain chain, Instant expiresAt)
	{
		boolean isExpired()
		{
			return Instant.now().isAfter(expiresAt);
		}
	}

	static JWKSet extractRpJwksFromChain(TrustChain chain, String clientId) throws Exception
	{
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
		return jwkSet;
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
		public Factory(ObjectFactory<FederatedPrivateKeyJwtVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
}
