/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;

class JwtClientAssertionVerifier
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, JwtClientAssertionVerifier.class);

	static void verifyJwt(SignedJWT jwt, JWKSet jwkSet, URI tokenEndpointUri, String clientId)
			throws AuthenticationException
	{
		try
		{
			var claims = jwt.getJWTClaimsSet();

			Date exp = claims.getExpirationTime();
			if (exp == null || exp.before(new Date()))
				throw new AuthenticationException("JWT assertion is expired or has no expiry");

			List<String> audience = claims.getAudience();
			if (audience == null || !audience.contains(tokenEndpointUri.toString()))
				throw new AuthenticationException("JWT audience does not contain token endpoint URI");

			String iss = claims.getIssuer();
			if (!clientId.equals(iss))
				throw new AuthenticationException("JWT issuer does not match client_id");

		} catch (ParseException e)
		{
			throw new AuthenticationException("Cannot parse JWT claims: " + e.getMessage());
		}

		if (!verifySignature(jwt, jwkSet))
			throw new AuthenticationException("JWT signature verification failed for client " + clientId);
	}

	private static boolean verifySignature(SignedJWT jwt, JWKSet jwkSet)
	{
		JWSAlgorithm alg = jwt.getHeader().getAlgorithm();
		String kid = jwt.getHeader().getKeyID();

		List<JWK> candidates = kid != null && jwkSet.getKeyByKeyId(kid) != null
				? List.of(jwkSet.getKeyByKeyId(kid))
				: jwkSet.getKeys();

		for (JWK jwk : candidates)
		{
			try
			{
				JWSVerifier verifier = buildVerifier(jwk, alg);
				if (verifier != null && jwt.verify(verifier))
					return true;
			} catch (JOSEException e)
			{
				log.trace("Verification attempt with key {} failed", jwk.getKeyID(), e);
			}
		}
		return false;
	}

	private static JWSVerifier buildVerifier(JWK jwk, JWSAlgorithm alg) throws JOSEException
	{
		if (jwk instanceof RSAKey rsaKey && JWSAlgorithm.Family.RSA.contains(alg))
			return new RSASSAVerifier(rsaKey.toRSAPublicKey());
		if (jwk instanceof ECKey ecKey && JWSAlgorithm.Family.EC.contains(alg))
			return new ECDSAVerifier(ecKey.toECPublicKey());
		return null;
	}
}
