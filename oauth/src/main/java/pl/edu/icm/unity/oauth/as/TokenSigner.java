/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSAlgorithm.Family;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Wrapper for  {@link JWSSigner}. Can signs token using RSA, EC or HMAC algorithm. 
 * @author P.Piernik
 *
 */
public class TokenSigner
{
	private JWSSigner internalSigner;
	private JWSAlgorithm algorithm;
	private X509Credential credential;

	public TokenSigner(OAuthASProperties config, PKIManagement pkiManamgenet)
	{
		String signAlg = config.getSigningAlgorithm();
		algorithm = JWSAlgorithm.parse(signAlg);
		
		
		if (Family.RSA.contains(algorithm))
		{
			setupCredential(config, pkiManamgenet);
			setupRSASigner();
		}

		else if (Family.EC.contains(algorithm))
		{
			setupCredential(config, pkiManamgenet);
			setupECSigner(signAlg);

		} else if (Family.HMAC_SHA.contains(algorithm))
		{
			setupHMACSigner(config, signAlg);
		} else
		{
			throw new ConfigurationException(
					"Unsupported signing algorithm " + signAlg);
		}

	}

	private void setupRSASigner()
	{
		
		PrivateKey pk = credential.getKey();
		if (pk == null || !(pk instanceof RSAPrivateKey))
		{
			throw new ConfigurationException(
					"The private key must be RSA if one of RS signingAlgorithm is used");
		}
		internalSigner = new RSASSASigner(pk);
	}
	
	private void setupECSigner(String signAlg)
	{
		PrivateKey pk = credential.getKey();
		if (pk == null || !(pk instanceof ECPrivateKey))
		{
			throw new ConfigurationException(
					"The private key must be EC if one of ES signingAlgorithm is used");
		}

		try
		{
			internalSigner = new ECDSASigner((ECPrivateKey) pk);
		} catch (JOSEException e)
		{
			throw new ConfigurationException("The EC key is incorrect", e);
		}

		if (!internalSigner.supportedJWSAlgorithms()
				.contains(JWSAlgorithm.parse(signAlg)))
			throw new ConfigurationException(
					"privateKey is not compatible with used ES algorithm");
	}
	
	private void setupHMACSigner(OAuthASProperties config, String signAlg)
	{
		String secret = config.getSigningSecret();
		if (secret == null || secret.isEmpty())
			throw new ConfigurationException(
					"signingSecret is required if one of HS signingAlgorithm is used");
		try
		{
			internalSigner = new MACSigner(config.getSigningSecret());

		} catch (KeyLengthException e)
		{
			throw new ConfigurationException("signingSecret is incorrect", e);
		}

		if (!internalSigner.supportedJWSAlgorithms()
				.contains(JWSAlgorithm.parse(signAlg)))
			throw new ConfigurationException(
					"signingSecret length is not compatible with used HS algorithm");
	}
	
	private void setupCredential(OAuthASProperties config, PKIManagement pkiManamgenet)
	{		
		String credential = config.getValue(OAuthASProperties.CREDENTIAL);
		if (credential == null)
		{
			throw new ConfigurationException(
					"Credential configuration is mandatory when one of RS* or ES* algorithms is set for token signing");
		}
		
		try
		{
			if (!pkiManamgenet.getCredentialNames().contains(credential))
				throw new ConfigurationException("There is no credential named '" + credential + 
						"' which is configured in the OAuth endpoint.");
			this.credential = pkiManamgenet.getCredential(credential);
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't obtain credential names.", e);
		}
	}
	
	public X509Credential getCredential()
	{
		return credential;
	}
	
	public X509Certificate getCredentialCertificate()
	{
		return credential.getCertificate();
	}
	
	public JWSAlgorithm getSigningAlgorithm()
	{
		return algorithm;
	}
	
	public SignedJWT sign(IDTokenClaimsSet idTokenClaims) throws JOSEException, ParseException
	{
		SignedJWT ret = new SignedJWT(new JWSHeader(algorithm),
				idTokenClaims.toJWTClaimsSet());	
		ret.sign(internalSigner);
		return ret;
	}
}
