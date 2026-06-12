/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;

class JwtClientAssertionVerifierTest
{
	private static final String CLIENT_ID = "https://client.example.com";
	private static final URI TOKEN_URI = URI.create("https://as.example.com/token");

	private RSAKey rsaKey;
	private JWKSet jwkSet;
	private JwtClientAssertionVerifier verifier;

	@BeforeEach
	void setUp() throws Exception
	{
		rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		jwkSet = new JWKSet(rsaKey.toPublicJWK());
		verifier = new JwtClientAssertionVerifier();
	}

	@Test
	void shouldAcceptValidJwt() throws Exception
	{
		SignedJWT jwt = buildValidJwt(UUID.randomUUID().toString());

		assertThatCode(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldRejectMissingJti() throws Exception
	{
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.issueTime(now).expirationTime(new Date(now.getTime() + 60_000))
				.build();
		SignedJWT jwt = sign(claims);

		assertThatThrownBy(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("jti");
	}

	@Test
	void shouldRejectBlankJti() throws Exception
	{
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.issueTime(now).expirationTime(new Date(now.getTime() + 60_000))
				.jwtID("   ")
				.build();
		SignedJWT jwt = sign(claims);

		assertThatThrownBy(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("jti");
	}

	@Test
	void shouldRejectMissingIat() throws Exception
	{
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = sign(claims);

		assertThatThrownBy(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("iat");
	}

	@Test
	void shouldRejectIatInFuture() throws Exception
	{
		Date farFuture = new Date(System.currentTimeMillis() + 120_000);
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.issueTime(farFuture)
				.expirationTime(new Date(farFuture.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = sign(claims);

		assertThatThrownBy(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("iat is in the future");
	}

	@Test
	void shouldRejectAssertionLifetimeExceedingCap() throws Exception
	{
		Date now = new Date();
		long capSeconds = JwtClientAssertionVerifier.MAX_ASSERTION_LIFETIME.toSeconds();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + (capSeconds + 60) * 1000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = sign(claims);

		assertThatThrownBy(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("lifetime")
				.hasMessageContaining("exceeds maximum");
	}

	@Test
	void shouldRejectReplayedJti() throws Exception
	{
		String jti = UUID.randomUUID().toString();
		SignedJWT jwt = buildValidJwt(jti);

		assertThatCode(() -> verifier.verifyJwt(jwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.doesNotThrowAnyException();

		SignedJWT replayedJwt = buildValidJwt(jti);
		assertThatThrownBy(() -> verifier.verifyJwt(replayedJwt, jwkSet, TOKEN_URI, CLIENT_ID))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("already been used");
	}

	@Test
	void shouldAcceptDifferentJtisIndependently() throws Exception
	{
		SignedJWT first = buildValidJwt(UUID.randomUUID().toString());
		SignedJWT second = buildValidJwt(UUID.randomUUID().toString());

		assertThatCode(() -> verifier.verifyJwt(first, jwkSet, TOKEN_URI, CLIENT_ID))
				.doesNotThrowAnyException();
		assertThatCode(() -> verifier.verifyJwt(second, jwkSet, TOKEN_URI, CLIENT_ID))
				.doesNotThrowAnyException();
	}

	private SignedJWT buildValidJwt(String jti) throws Exception
	{
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID).issuer(CLIENT_ID).audience(TOKEN_URI.toString())
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(jti)
				.build();
		return sign(claims);
	}

	private SignedJWT sign(JWTClaimsSet claims) throws Exception
	{
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(rsaKey));
		return jwt;
	}
}
