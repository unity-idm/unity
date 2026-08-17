/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import eu.emi.security.authn.x509.X509Credential;

public class CredentialJwkConverter
{
	public static JWK buildPublicJWK(X509Certificate cert) throws JOSEException
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
		throw new JOSEException("Unsupported public key type: " + cert.getPublicKey().getAlgorithm());
	}

	public static JWK buildPrivateJWK(X509Credential credential) throws JOSEException
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
			return new ECKey.Builder(Curve.forECParameterSpec(ecPublicKey.getParams()), ecPublicKey)
					.privateKey(ecKey)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
		}
		throw new JOSEException("Unsupported key type: " + pk.getClass().getSimpleName());
	}
}
