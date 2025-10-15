/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

/**
 * An integration test of token exchange flow
 * @author P.Piernik
 *
 */
public class ExchangeTokenTest extends TokenTestBase
{


	private AccessToken initExchange(List<String> scopes, ClientAuthentication ca) throws Exception
	{
		return init(scopes, ca).getTokens().getAccessToken();
	}

	@Test
	public void shouldDenyToExchangeTokenWithIncorrectAudience() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca2,
				new TokenExchangeGrant(aToken,
						TokenTypeURI.ACCESS_TOKEN, null, null, null, Audience.create(List.of("client3"))),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void shouldDenyToExchangeTokenWithIncorrectRequestedTokenType() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca2,
				new TokenExchangeGrant(aToken,
						TokenTypeURI.ACCESS_TOKEN, null, null, TokenTypeURI.parse("wrong"), Audience.create(List.of("client2"))),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);
	}


	@Test
	public void shouldExchangeTokenWithIdToken() throws Exception
	{
		super.setupOIDC(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(Arrays.asList("openid", "foo", "bar",
				AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca2,
				new TokenExchangeGrant(aToken,
						TokenTypeURI.ACCESS_TOKEN, null, null, TokenTypeURI.ID_TOKEN, Audience.create(List.of("client2"))),
				new Scope("openid foo bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse exchangeResp = wrapped.send();
		AccessTokenResponse exchangeParsedResp = AccessTokenResponse.parse(exchangeResp);
		assertThat(exchangeParsedResp.getTokens().getAccessToken()).isNotNull();
		assertThat(exchangeParsedResp.getCustomParameters().get("id_token")).isNotNull();
		assertThat(exchangeParsedResp.getTokens().getAccessToken().getIssuedTokenType().getURI().toASCIIString()).
				isEqualTo(AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		// check new token info
		JSONObject parsed = getTokenInfo(exchangeParsedResp.getTokens().getAccessToken());
		assertThat(parsed.get("sub")).isEqualTo("userA");
		assertThat(parsed.get("client_id")).isEqualTo("client2");
		assertThat(parsed.get("aud")).isEqualTo("client2");
		assertThat(((JSONArray) parsed.get("scope")).get(0)).isEqualTo("foo");
		assertThat(parsed.get("exp")).isNotNull();
	}
	
	@Test
	public void shouldExchangeAccessTokenWithoutIdToken() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(Arrays.asList("foo", "bar",
				AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca2, new TokenExchangeGrant(aToken,
						TokenTypeURI.ACCESS_TOKEN, null, null, null, Audience.create(List.of("client2"))),
				new Scope("foo", "bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse exchangeResp = wrapped.send();
		AccessTokenResponse exchangeParsedResp = AccessTokenResponse.parse(exchangeResp);
		
		assertThat(exchangeParsedResp.getTokens().getAccessToken()).isNotNull();	
		assertThat(exchangeParsedResp.getTokens().getAccessToken().getIssuedTokenType().getURI().toASCIIString()) 
				.isEqualTo(AccessTokenResource.ACCESS_TOKEN_TYPE_ID);
		
		// check new token info
		JSONObject parsed = getTokenInfo(exchangeParsedResp.getTokens().getAccessToken());
		assertEquals("userA", parsed.get("sub"));
		assertThat(parsed.get("sub")).isEqualTo("userA");
		assertThat(parsed.get("client_id")).isEqualTo("client2");
		assertThat(parsed.get("aud")).isEqualTo("client2");
		assertThat(((JSONArray) parsed.get("scope")).get(0)).isEqualTo("foo");
		assertThat(parsed.get("exp")).isNotNull();
	}
}
