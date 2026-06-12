/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
	static final Duration MAX_ASSERTION_LIFETIME = Duration.ofMinutes(5);
	private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, JwtClientAssertionVerifier.class);

	private final ConcurrentHashMap<String, Instant> seenJtis = new ConcurrentHashMap<>();

	void verifyJwt(SignedJWT jwt, JWKSet jwkSet, URI tokenEndpointUri, String clientId)
			throws AuthenticationException
	{
		ParsedClaims parsed;
		try
		{
			parsed = parseAndValidateClaims(jwt, tokenEndpointUri, clientId);
		} catch (ParseException e)
		{
			throw new AuthenticationException("Cannot parse JWT claims: " + e.getMessage());
		}

		if (!verifySignature(jwt, jwkSet))
			throw new AuthenticationException("JWT signature verification failed for client " + clientId);

		checkAndRegisterJti(parsed.jti(), parsed.expiry(), clientId);
	}

	private ParsedClaims parseAndValidateClaims(SignedJWT jwt, URI tokenEndpointUri, String clientId)
			throws ParseException, AuthenticationException
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

		String jti = claims.getJWTID();
		if (jti == null || jti.isBlank())
			throw new AuthenticationException("JWT assertion must contain a jti claim (OIDC Core §9)");

		Date iat = claims.getIssueTime();
		if (iat == null)
			throw new AuthenticationException("JWT assertion must contain an iat claim");
		if (iat.toInstant().isAfter(Instant.now().plus(CLOCK_SKEW)))
			throw new AuthenticationException("JWT assertion iat is in the future");

		Duration lifetime = Duration.between(iat.toInstant(), exp.toInstant());
		if (lifetime.compareTo(MAX_ASSERTION_LIFETIME) > 0)
			throw new AuthenticationException(
					"JWT assertion lifetime " + lifetime.toSeconds() + "s exceeds maximum "
							+ MAX_ASSERTION_LIFETIME.toSeconds() + "s (RFC 7523 §3)");

		return new ParsedClaims(jti, exp.toInstant());
	}

	private void checkAndRegisterJti(String jti, Instant expiry, String clientId)
			throws AuthenticationException
	{
		evictExpiredJtis();
		if (seenJtis.putIfAbsent(jti, expiry) != null)
			throw new AuthenticationException(
					"JWT assertion jti has already been used (replay detected): " + jti);
	}

	private void evictExpiredJtis()
	{
		Instant now = Instant.now();
		seenJtis.entrySet().removeIf(e -> now.isAfter(e.getValue()));
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

	private record ParsedClaims(String jti, Instant expiry) {}
}
