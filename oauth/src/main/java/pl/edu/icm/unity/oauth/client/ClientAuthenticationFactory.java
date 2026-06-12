/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.net.URI;
import java.security.PrivateKey;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;

class ClientAuthenticationFactory
{
	static final long ASSERTION_LIFETIME_MS = 60_000L;

	private final PKIManagement pkiManagement;

	ClientAuthenticationFactory(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	ClientAuthentication build(OAuthProviderConfiguration providerCfg, URI tokenEndpointURI, ClientAuthnMode mode)
			throws EngineException, JOSEException
	{
		return switch (providerCfg.clientAuthnMethod)
		{
			case private_key_jwt -> buildPrivateKeyJwtAuthentication(providerCfg, tokenEndpointURI);
			case client_secret -> buildClientSecretAuthentication(providerCfg, mode);
		};
	}

	private ClientAuthentication buildPrivateKeyJwtAuthentication(OAuthProviderConfiguration providerCfg,
			URI tokenEndpointURI) throws EngineException, JOSEException
	{
		X509Credential cred = pkiManagement.getCredential(providerCfg.clientCredential);
		PrivateKey privateKey = cred.getKey();
		JWSAlgorithm alg = deriveJWSAlgorithm(privateKey);
		Date now = new Date();
		Date exp = new Date(now.getTime() + ASSERTION_LIFETIME_MS);
		JWTAuthenticationClaimsSet claimsSet = new JWTAuthenticationClaimsSet(
				new ClientID(providerCfg.clientId),
				List.of(new Audience(tokenEndpointURI.toString())),
				exp,
				null,
				now,
				new JWTID());
		return new PrivateKeyJWT(claimsSet, alg, privateKey, null, null, null, null);
	}

	private static ClientAuthentication buildClientSecretAuthentication(OAuthProviderConfiguration providerCfg,
			ClientAuthnMode mode)
	{
		return switch (mode)
		{
			case secretPost -> new ClientSecretPost(new ClientID(providerCfg.clientId),
					new Secret(providerCfg.clientSecret));
			case secretBasic -> new ClientSecretBasic(new ClientID(providerCfg.clientId),
					new Secret(providerCfg.clientSecret));
		};
	}

	static JWSAlgorithm deriveJWSAlgorithm(PrivateKey privateKey)
	{
		return switch (privateKey.getAlgorithm())
		{
			case "EC" -> JWSAlgorithm.ES256;
			case "RSA" -> JWSAlgorithm.RS256;
			default -> throw new InternalException("Unsupported key type for private_key_jwt: "
					+ privateKey.getAlgorithm());
		};
	}
}
