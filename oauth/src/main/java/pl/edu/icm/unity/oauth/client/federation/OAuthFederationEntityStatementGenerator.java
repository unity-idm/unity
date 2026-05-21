/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.oauth.as.token.KeyIdExtractor;

class OAuthFederationEntityStatementGenerator
{
	static EntityStatement generate(OAuthFederationEntityStatementConfig config) throws Exception
	{
		JWKSet federationJwkSet = new JWKSet(buildPublicJWK(config.federationCredential().getCertificate()));

		EntityID entityID = new EntityID(config.entityId());
		Date now = new Date();
		Date exp = new Date(now.getTime() + config.validitySeconds() * 1000L);

		EntityStatementClaimsSet claims = new EntityStatementClaimsSet(entityID, entityID, now, exp, federationJwkSet);

		if (config.superiorEntityId() != null && !config.superiorEntityId()
				.isEmpty())
			claims.setAuthorityHints(List.of(new EntityID(config.superiorEntityId())));

		OIDCClientMetadata rpMetadata = new OIDCClientMetadata();
		rpMetadata.setRedirectionURI(new URI(config.callbackUrl()));
		rpMetadata.setResponseTypes(Set.of(ResponseType.CODE));
		rpMetadata.setGrantTypes(Set.of(GrantType.AUTHORIZATION_CODE));
		rpMetadata.setClientRegistrationTypes(List.of(ClientRegistrationType.AUTOMATIC));
		rpMetadata.setJWKSet(buildRpJwkSet(config));

		OIDCClientInformation rpInfo = new OIDCClientInformation(new ClientID(config.entityId()), rpMetadata);
		claims.setRPInformation(rpInfo);

		JWK signingJWK = buildPrivateJWK(config.federationCredential());
		return EntityStatement.sign(claims, signingJWK);
	}

	private static JWKSet buildRpJwkSet(OAuthFederationEntityStatementConfig config) throws JOSEException
	{
		X509Credential authCred = config.authenticationCredential();
		if (authCred == null)
			return new JWKSet(buildPublicJWK(config.federationCredential().getCertificate()));

		String authKid = KeyIdExtractor.getKeyId(authCred.getCertificate());
		String fedKid = KeyIdExtractor.getKeyId(config.federationCredential().getCertificate());
		if (authKid.equals(fedKid))
			return new JWKSet(buildPublicJWK(config.federationCredential().getCertificate()));

		return new JWKSet(buildPublicJWK(authCred.getCertificate()));
	}

	private static JWK buildPublicJWK(X509Certificate cert) throws JOSEException
	{
		String kid = KeyIdExtractor.getKeyId(cert);
		if (cert.getPublicKey() instanceof RSAPublicKey rsaPublicKey)
		{
			return new RSAKey.Builder(rsaPublicKey).keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
		} else if (cert.getPublicKey() instanceof ECPublicKey ecPublicKey)
		{
			return new ECKey.Builder(Curve.forECParameterSpec(ecPublicKey.getParams()), ecPublicKey)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
		}
		throw new JOSEException("Unsupported public key type: " + cert.getPublicKey()
				.getAlgorithm());
	}

	private static JWK buildPrivateJWK(X509Credential credential) throws JOSEException
	{
		X509Certificate cert = credential.getCertificate();
		PrivateKey pk = credential.getKey();
		String kid = KeyIdExtractor.getKeyId(cert);

		if (pk instanceof RSAPrivateKey rsaKey && cert.getPublicKey() instanceof RSAPublicKey rsaPublicKey)
		{
			return new RSAKey.Builder(rsaPublicKey).privateKey(rsaKey)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
		} else if (pk instanceof ECPrivateKey ecKey && cert.getPublicKey() instanceof ECPublicKey ecPublicKey)
		{
			return new ECKey.Builder(Curve.forECParameterSpec(ecPublicKey.getParams()), ecPublicKey).privateKey(ecKey)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
		}
		throw new JOSEException("Unsupported key type: " + pk.getClass()
				.getSimpleName());
	}
}
