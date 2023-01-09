/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.rest.jwt.JWTUtils;

public class TokenIntrospectionRedirectTest
{
	@Test
	public void shouldRedirectToRemoteService() throws Exception
	{
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = mock(RemoteTokenIntrospectionService.class);
		TokenIntrospectionResource res = new TokenIntrospectionResource(remoteTokenIntrospectionService, null,
				OAuthTestUtils.ISSUER);
		SignedJWT jwts = SignedJWT.parse(
				JWTUtils.generate(new MockPKIMan().getCredential("MAIN"), new JWTClaimsSet.Builder().issuer("rem")
						.build()));
		res.introspectToken(jwts.serialize());
		verify(remoteTokenIntrospectionService).processRemoteIntrospection(any());
	}
	
	@Test
	public void shouldIntrospectByLocalService() throws Exception
	{
		LocalTokenIntrospectionService localTokenIntrospectionService = mock(LocalTokenIntrospectionService.class);
		TokenIntrospectionResource res = new TokenIntrospectionResource(null, localTokenIntrospectionService,
				OAuthTestUtils.ISSUER);
		SignedJWT jwts = SignedJWT.parse(
				JWTUtils.generate(new MockPKIMan().getCredential("MAIN"), new JWTClaimsSet.Builder().issuer(OAuthTestUtils.ISSUER)
						.build()));
		res.introspectToken(jwts.serialize());
		verify(localTokenIntrospectionService).processLocalIntrospection(any());
	}
}
