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

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

public class JwtClientAssertionVerifier
{
	public static final Duration DEFAULT_MAX_ASSERTION_LIFETIME = Duration.ofMinutes(5);
	public static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(30);
	private static final int MAX_TRACKED_JTIS = 1000000;
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, JwtClientAssertionVerifier.class);

	private volatile Duration clockSkew = DEFAULT_CLOCK_SKEW;
	private volatile Duration maxAssertionLifetime = DEFAULT_MAX_ASSERTION_LIFETIME;
	private volatile Cache<String, Boolean> seenJtis = buildJtiCache();

	void setClockSkew(Duration clockSkew)
	{
		if (clockSkew == null || clockSkew.isNegative())
			throw new IllegalArgumentException("Clock skew must not be null or negative");
		this.clockSkew = clockSkew;
		this.seenJtis = buildJtiCache();
	}

	void setMaxAssertionLifetime(Duration maxAssertionLifetime)
	{
		if (maxAssertionLifetime == null || maxAssertionLifetime.isNegative())
			throw new IllegalArgumentException("Max assertion lifetime must not be null or negative");
		this.maxAssertionLifetime = maxAssertionLifetime;
		this.seenJtis = buildJtiCache();
	}

	/**
	 * The replay-guard cache's TTL is a conservative fixed upper bound
	 * ({@code maxAssertionLifetime + clockSkew}) rather than each JWT's exact {@code exp}: Guava's
	 * cache (like most off-the-shelf expiring caches) only supports one TTL per cache, not a custom
	 * per-entry expiry policy. Since {@code parseAndValidateClaims} already rejects any assertion
	 * whose {@code exp - iat} exceeds {@code maxAssertionLifetime}, no jti ever needs to be retained
	 * longer than this bound - entries with a shorter actual lifetime just get evicted a little later
	 * than strictly necessary, which is harmless for replay protection (never too early, only ever
	 * equal or later). Rebuilt (dropping current entries) whenever the settings change, so a stale,
	 * too-short TTL is never left in place after reconfiguration.
	 */
	private Cache<String, Boolean> buildJtiCache()
	{
		return CacheBuilder.newBuilder()
				.expireAfterWrite(maxAssertionLifetime.plus(clockSkew))
				.maximumSize(MAX_TRACKED_JTIS)
				.build();
	}

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

		checkAndRegisterJti(parsed.jti());
	}

	private ParsedClaims parseAndValidateClaims(SignedJWT jwt, URI tokenEndpointUri, String clientId)
			throws ParseException, AuthenticationException
	{
		var claims = jwt.getJWTClaimsSet();

		Date exp = claims.getExpirationTime();
		if (exp == null || exp.before(Date.from(Instant.now().minus(clockSkew))))
			throw new AuthenticationException("JWT assertion is expired or has no expiry");

		List<String> audience = claims.getAudience();
		if (audience == null || !audience.contains(tokenEndpointUri.toString()))
			throw new AuthenticationException("JWT audience does not contain token endpoint URI");

		String iss = claims.getIssuer();
		if (!clientId.equals(iss))
			throw new AuthenticationException("JWT issuer does not match client_id");

		String sub = claims.getSubject();
		if (!clientId.equals(sub))
			throw new AuthenticationException(
					"JWT subject does not match client_id (RFC 7523 §3, OIDC Core §9)");

		String jti = claims.getJWTID();
		if (jti == null || jti.isBlank())
			throw new AuthenticationException("JWT assertion must contain a jti claim (OIDC Core §9)");

		Date iat = claims.getIssueTime();
		if (iat == null)
			throw new AuthenticationException("JWT assertion must contain an iat claim");
		if (iat.toInstant().isAfter(Instant.now().plus(clockSkew)))
			throw new AuthenticationException("JWT assertion iat is in the future");

		Date nbf = claims.getNotBeforeTime();
		if (nbf != null && nbf.toInstant().isAfter(Instant.now().plus(clockSkew)))
			throw new AuthenticationException("JWT assertion is not yet valid (nbf is in the future)");

		Duration lifetime = Duration.between(iat.toInstant(), exp.toInstant());
		if (lifetime.compareTo(maxAssertionLifetime) > 0)
			throw new AuthenticationException(
					"JWT assertion lifetime " + lifetime.toSeconds() + "s exceeds maximum "
							+ maxAssertionLifetime.toSeconds() + "s (RFC 7523 §3)");

		return new ParsedClaims(jti);
	}

	/**
	 * Atomically registers a jti as seen, rejecting a replay. {@code Cache.asMap()} exposes a
	 * {@link java.util.concurrent.ConcurrentMap} view backed by the cache, so {@code putIfAbsent}
	 * both performs the atomic replay check and participates in the cache's normal write-time/size
	 * bookkeeping - no separate eviction sweep or cap check is needed: {@code expireAfterWrite}
	 * reclaims expired entries and {@code maximumSize} evicts (approximately-LRU) once
	 * {@link #MAX_TRACKED_JTIS} is reached, instead of hard-failing every client sharing this
	 * authenticator.
	 */
	private void checkAndRegisterJti(String jti) throws AuthenticationException
	{
		if (seenJtis.asMap().putIfAbsent(jti, Boolean.TRUE) != null)
			throw new AuthenticationException(
					"JWT assertion jti has already been used (replay detected): " + jti);
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

	private record ParsedClaims(String jti) {}
}
