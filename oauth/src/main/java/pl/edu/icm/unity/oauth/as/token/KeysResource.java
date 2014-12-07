/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.ECKey.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * Informs about keys used by the AS. Free access.
 * 
 * @author K. Benedyczak
 */
@Produces(JWK.MIME_TYPE)
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
	public String getKeys() 
	{
		X509Certificate certificate = config.getCredential().getCertificate();
		JWK jwk;
		if (certificate.getPublicKey() instanceof RSAPublicKey)
		{
			jwk = new RSAKey.Builder((RSAPublicKey) certificate.getPublicKey()).
					keyUse(KeyUse.SIGNATURE).build();
		} else if (certificate.getPublicKey() instanceof ECPublicKey)
		{
			jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey)certificate.getPublicKey()).
					keyUse(KeyUse.SIGNATURE).build();
		} else
			throw new InternalException("Unsupported key in certificate, shouldn't happen");
		JWKSet set = new JWKSet(jwk);
		return set.toString();
	}
}
