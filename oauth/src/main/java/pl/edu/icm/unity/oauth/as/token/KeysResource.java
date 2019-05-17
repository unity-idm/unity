/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

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
		JWSAlgorithm signAlg = config.getTokenSigner().getSigningAlgorithm();
		JWKSet set;
		if (Family.RSA.contains(signAlg))
		{
			set = new JWKSet(new RSAKey.Builder((RSAPublicKey) config.getTokenSigner()
					.getCredentialCertificate().getPublicKey())
							.keyUse(KeyUse.SIGNATURE).build());
		} else if (Family.EC.contains(signAlg))
		{
			set = new JWKSet(new ECKey.Builder(
					Curve.forJWSAlgorithm(signAlg).iterator().next(),
					(ECPublicKey) config.getTokenSigner()
							.getCredentialCertificate().getPublicKey())
									.keyUse(KeyUse.SIGNATURE)
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
}
