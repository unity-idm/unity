/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.text.ParseException;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
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

	private String federationTrustAnchorId;
	private JWKSet federationTrustAnchorJwks;
	private String federationTruststore;
	private X509CertChainValidator federationValidator;
	private ServerHostnameCheckingMode federationHostnameCheckingMode;
	private final PKIManagement pkiManagement;
	private final JwtClientAssertionVerifier jwtVerifier = new JwtClientAssertionVerifier();

	@Autowired
	public FederatedPrivateKeyJwtVerificator(PKIManagement pkiManagement)
	{
		super(NAME, DESC, ClientAssertionExchange.ID);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = JsonUtil.parse("{}");
		if (federationTrustAnchorId != null)
			root.put("federationTrustAnchorId", federationTrustAnchorId);
		if (federationTrustAnchorJwks != null)
			root.put("federationTrustAnchorJwks", federationTrustAnchorJwks.toString());
		if (federationTruststore != null)
			root.put("federationTruststore", federationTruststore);
		if (federationHostnameCheckingMode != null)
			root.put("federationHostnameChecking", federationHostnameCheckingMode.name());
		return JsonUtil.serialize(root);
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		ObjectNode root = JsonUtil.parse(json);
		if (root.has("federationTrustAnchorId"))
			federationTrustAnchorId = root.get("federationTrustAnchorId").asText(null);
		if (root.has("federationTrustAnchorJwks"))
		{
			try
			{
				federationTrustAnchorJwks = JWKSet.parse(root.get("federationTrustAnchorJwks").asText());
			} catch (ParseException e)
			{
				throw new InternalException(
						"Invalid federation trust anchor JWKS in verificator config: " + e.getMessage());
			}
		}
		if (root.has("federationTruststore"))
		{
			federationTruststore = root.get("federationTruststore").asText(null);
			federationValidator = resolveValidator(federationTruststore);
		}
		if (root.has("federationHostnameChecking"))
		{
			try
			{
				federationHostnameCheckingMode = ServerHostnameCheckingMode.valueOf(
						root.get("federationHostnameChecking").asText());
			} catch (IllegalArgumentException e)
			{
				throw new InternalException("Invalid federationHostnameChecking value in verificator config");
			}
		}
	}

	private X509CertChainValidator resolveValidator(String truststoreName) throws InternalException
	{
		if (truststoreName == null || truststoreName.isBlank())
			return null;
		try
		{
			if (!pkiManagement.getValidatorNames().contains(truststoreName))
				return null;
			return pkiManagement.getValidator(truststoreName);
		} catch (Exception e)
		{
			throw new InternalException("Cannot resolve truststore '" + truststoreName + "': " + e.getMessage());
		}
	}

	@Override
	public AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri)
	{
		if (federationTrustAnchorId == null || federationTrustAnchorJwks == null)
		{
			log.warn("Federation trust anchor not configured");
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

		JWKSet jwkSet;
		try
		{
			jwkSet = resolveJwksFromFederation(clientId);
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

	JWKSet resolveJwksFromFederation(String clientId) throws Exception
	{
		TrustChainResolver resolver = new TrustChainResolver(
				Map.of(new EntityID(federationTrustAnchorId), federationTrustAnchorJwks),
				TrustChainConstraints.NO_CONSTRAINTS,
				new TlsEntityStatementRetriever(federationValidator,
						federationHostnameCheckingMode != null
								? federationHostnameCheckingMode
								: ServerHostnameCheckingMode.FAIL));

		TrustChainSet chains = resolver.resolveTrustChains(new EntityID(clientId));
		return extractRpJwksFromChain(chains.getShortest(), clientId);
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
