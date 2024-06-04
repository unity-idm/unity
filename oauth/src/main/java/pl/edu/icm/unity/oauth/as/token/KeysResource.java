/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSAlgorithm.Family;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;

/**
 * Informs about keys used by the AS. Free access.
 * 
 * @author K. Benedyczak
 */
@Path(OAuthTokenEndpoint.JWK_PATH)
public class KeysResource extends BaseOAuthResource
{
	private OAuthASProperties config;
	
	public KeysResource(OAuthASProperties config)
	{
		this.config = config;
	}

	@Path("/")
	@GET
	@Produces(JWKSet.MIME_TYPE)	
	public String getKeys() 
	{
		if (!config.getTokenSigner().isPKIEnabled())
			return new JWKSet().toString();
		JWSAlgorithm signAlg = config.getTokenSigner().getSigningAlgorithm();
		JWKSet set;
		if (Family.RSA.contains(signAlg))
		{
			set = new JWKSet(new RSAKey.Builder((RSAPublicKey) config.getTokenSigner().getCredentialCertificate().getPublicKey())
					.keyUse(KeyUse.SIGNATURE)
					.x509CertChain(getCertAsX5CAttribute(config.getTokenSigner().getCredentialCertificateChain()))
					.build());
		} else if (Family.EC.contains(signAlg))
		{
			set = new JWKSet(new ECKey.Builder(Curve.forJWSAlgorithm(signAlg).iterator().next(),
									(ECPublicKey) config.getTokenSigner().getCredentialCertificate().getPublicKey())
									.keyUse(KeyUse.SIGNATURE)
									.x509CertChain(getCertAsX5CAttribute(config.getTokenSigner().getCredentialCertificateChain()))
									.build());
		} else if (Family.HMAC_SHA.contains(signAlg))
		{
			set = new JWKSet();
		} else
		{
			throw new InternalException(
					"Unsupported key in certificate, shouldn't happen");
		}

		return set.toString();
	}
	
	private List<Base64> getCertAsX5CAttribute(X509Certificate[] certs)
	{
		return Stream.of(certs)
				.map(c ->
				{
					try
					{
						return Base64.encode(c.getEncoded());
					} catch (CertificateEncodingException e)
					{
						throw new InternalException("Can not encode certificate", e);
					}
				})
				.collect(Collectors.toList());
	}
}
