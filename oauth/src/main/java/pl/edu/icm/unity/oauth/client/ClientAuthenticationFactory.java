/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.net.URI;
import java.security.PrivateKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;

class ClientAuthenticationFactory
{
	private final PKIManagement pkiManagement;

	ClientAuthenticationFactory(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	ClientAuthentication build(OAuthProviderConfiguration providerCfg, URI tokenEndpointURI, ClientAuthnMode mode)
			throws EngineException, JOSEException
	{
		if (ClientAuthnMethod.private_key_jwt == providerCfg.clientAuthnMethod)
		{
			X509Credential cred = pkiManagement.getCredential(providerCfg.clientCredential);
			PrivateKey privateKey = cred.getKey();
			JWSAlgorithm alg = deriveJWSAlgorithm(privateKey);
			return new PrivateKeyJWT(new ClientID(providerCfg.clientId), tokenEndpointURI,
					alg, privateKey, null, null);
		}
		return switch (mode)
		{
			case secretPost -> new ClientSecretPost(new ClientID(providerCfg.clientId),
					new Secret(providerCfg.clientSecret));
			default -> new ClientSecretBasic(new ClientID(providerCfg.clientId),
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
