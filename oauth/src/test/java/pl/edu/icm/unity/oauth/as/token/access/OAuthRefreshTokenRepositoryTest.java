/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.types.basic.EntityParam;

public class OAuthRefreshTokenRepositoryTest
{
	@Test
	public void shouldRemoveHistoryWhenRemovingRefreshToken()
			throws JsonProcessingException, IllegalTypeException, IllegalIdentityValueException
	{
		TokensManagement tokensManagement = new MockTokensMan();

		OAuthToken token = new OAuthToken();
		token.setRefreshToken("ref");
		token.setFirstRefreshRollingToken("ref");
		token.setClientId(99l);
		tokensManagement.addToken(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN, "ref", new EntityParam(0l),
				token.getSerialized(), null, null);

		OAuthToken htoken = new OAuthToken();
		htoken.setRefreshToken("ref_his");
		htoken.setFirstRefreshRollingToken("ref");
		htoken.setClientId(99l);
		tokensManagement.addToken(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "ref_his",
				new EntityParam(0l), htoken.getSerialized(), null, null);

		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);
		rep.removeRefreshToken("ref", token, 0);

		assertThat(tokensManagement.getAllTokens(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN).size(), is(0));
		assertThat(tokensManagement.getAllTokens(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN).size(),
				is(0));
	}
}
