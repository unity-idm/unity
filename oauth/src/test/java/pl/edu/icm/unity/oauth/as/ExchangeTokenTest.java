/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;

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
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						"client3"),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode(), is(HTTPResponse.SC_BAD_REQUEST));
	}
	
	@Test
	public void shouldDenyToExchangeTokenWithIncorrectRequestedTokenType() throws Exception
	{

		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						"wrong",
						"client2"),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode(), is(HTTPResponse.SC_BAD_REQUEST));
	}


	@Test
	public void shouldExchangeTokenWithIdToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(Arrays.asList("openid", "foo", "bar",
				AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						AccessTokenResource.ID_TOKEN_TYPE_ID, "client2"),
				new Scope("openid foo bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse exchangeResp = wrapped.send();
		AccessTokenResponse exchangeParsedResp = AccessTokenResponse.parse(exchangeResp);
		assertThat(exchangeParsedResp.getTokens().getAccessToken(), notNullValue());
		assertThat(exchangeParsedResp.getCustomParameters().get("id_token"), notNullValue());
		assertThat(exchangeParsedResp.getCustomParameters().get("issued_token_type"),
				is(AccessTokenResource.ACCESS_TOKEN_TYPE_ID));

		// check new token info
		JSONObject parsed = getTokenInfo(exchangeParsedResp.getTokens().getAccessToken());
		assertThat(parsed.get("sub"), is("userA"));
		assertThat(parsed.get("client_id"), is("client2"));
		assertThat(parsed.get("aud"), is("client2"));
		assertThat(((JSONArray) parsed.get("scope")).get(0), is("foo"));
		assertThat(parsed.get("exp"), notNullValue());
	}
	
	@Test
	public void shouldExchangeAccessTokenWithoutIdToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchange(Arrays.asList("foo", "bar",
				AccessTokenResource.EXCHANGE_SCOPE), ca);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID, "client2"),
				new Scope("foo bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse exchangeResp = wrapped.send();
		AccessTokenResponse exchangeParsedResp = AccessTokenResponse.parse(exchangeResp);
		
		assertThat(exchangeParsedResp.getTokens().getAccessToken(), notNullValue());	
		assertThat(exchangeParsedResp.getCustomParameters().get("issued_token_type"), is(AccessTokenResource.ACCESS_TOKEN_TYPE_ID));
	
		// check new token info
		JSONObject parsed = getTokenInfo(exchangeParsedResp.getTokens().getAccessToken());
		assertEquals("userA", parsed.get("sub"));
		assertThat(parsed.get("sub"), is("userA"));
		assertThat(parsed.get("client_id"), is("client2"));
		assertThat(parsed.get("aud"), is("client2"));
		assertThat(((JSONArray) parsed.get("scope")).get(0), is("foo"));
		assertThat(parsed.get("exp"), notNullValue());
	}
	
	/**
	 * 
	 * @author P.Piernik Simply ExchangeGrant for using with nimbusDS
	 */
	private static final class ExchangeGrant extends AuthorizationGrant
	{
		private String subjectToken;
		private String subjectTokenType;
		private String requestedType;
		private String audience;

		public ExchangeGrant(GrantType type, String subjectToken, String subjectTokenType,
				String requestedType, String audience)
		{
			// only for compilance, not used in toParameters method
			super(type);

			this.subjectToken = subjectToken;
			this.subjectTokenType = subjectTokenType;
			this.requestedType = requestedType;
			this.audience = audience;
		}

		@Override
		public Map<String, List<String>> toParameters()
		{
			Map<String, List<String>> params = new LinkedHashMap<>();
			params.put("grant_type", Lists.newArrayList(AccessTokenResource.EXCHANGE_GRANT));
			params.put("subject_token", Lists.newArrayList(subjectToken));
			params.put("subject_token_type", Lists.newArrayList(subjectTokenType));
			params.put("requested_token_type", Lists.newArrayList(requestedType));
			params.put("audience", Lists.newArrayList(audience));
			return params;

		}

	}
}
