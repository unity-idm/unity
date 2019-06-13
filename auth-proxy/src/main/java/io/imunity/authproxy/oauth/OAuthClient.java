/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy.oauth;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.authproxy.AuthnConfiguration;
import io.imunity.authproxy.UserAttributes;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.utils.Log;

@Component
public class OAuthClient
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthClient.class);
	
	private AuthnConfiguration configuration;
	
	@Autowired
	OAuthClient(AuthnConfiguration configuration)
	{
		this.configuration = configuration;
	}

	public URI createRequest(String state) 
	{
		Scope scope = null;
		ResponseMode responseMode = null;
		AuthorizationRequest req = new AuthorizationRequest(
				configuration.authorizationEndpoint,
				new ResponseType(ResponseType.Value.CODE),
				responseMode,
				new ClientID(configuration.clientId),
				configuration.responseConsumerAddress,
				scope, 
				new State(state));
		return req.toURI();
	}

	public UserAttributes getAccessTokenAndProfile(String authorizationCode)
	{
		HTTPResponse accessTokenResp;
		try
		{
			accessTokenResp = retrieveAccessToken(authorizationCode);
		} catch (IOException e)
		{
			throw new AuthenticationException("Can't exchange authorization code for access token", e);
		}

		try
		{
			return getProfile(accessTokenResp);
		} catch (ParseException | IOException e)
		{
			throw new AuthenticationException("Can't fetch user's profile", e);
		}
	}
	
	private HTTPResponse retrieveAccessToken(String authorizationCode) throws IOException
	{
		ClientAuthentication clientAuthn = new ClientSecretBasic(new ClientID(configuration.clientId), 
				new Secret(configuration.clientSecret));
		AuthorizationCodeGrant authzCodeGrant = new AuthorizationCodeGrant(
				new AuthorizationCode(authorizationCode), 
				configuration.responseConsumerAddress); 
		TokenRequest request = new TokenRequest(
				configuration.tokenEndpoint,
				clientAuthn,
				authzCodeGrant);
		
		HTTPRequest httpRequest = new CustomHTTPSRequest(request.toHTTPRequest(), 
				configuration.certValidator, ServerHostnameCheckingMode.FAIL);
		httpRequest.setAccept(CommonContentTypes.APPLICATION_JSON.toString());
		
		if (log.isTraceEnabled())
		{
			String queryWithoutSecret = httpRequest.getQuery().replaceFirst(
					"client_secret=[^&]*", "client_secret=xxxxxx");
			log.trace("Exchanging authorization code for access token with request to: " + 
					httpRequest.getURL() + "?" + queryWithoutSecret);
		} else if (log.isDebugEnabled())
		{
			log.debug("Exchanging authorization code for access token with request to: " + 
					httpRequest.getURL());
		}
		HTTPResponse response = httpRequest.send();
		
		log.debug("Received answer: " + response.getStatusCode());
		if (response.getStatusCode() != 200)
		{
			log.error("Error when getting access token. Contents: " + response.getContent());
			throw new AuthenticationException("Internal authentication problem");
		}

		log.trace("Received token: " + response.getContent());
		return response;
	}
	
	private UserAttributes getProfile(HTTPResponse response) throws ParseException, IOException
	{
		JSONObject jsonResp = response.getContentAsJSONObject();
		if (!jsonResp.containsKey("token_type"))
			jsonResp.put("token_type", AccessTokenType.BEARER.getValue());
		AccessTokenResponse atResponse = AccessTokenResponse.parse(jsonResp);
		BearerAccessToken accessToken = extractAccessToken(atResponse);
		
		Map<String, List<String>> attributesFromToken = extractUserInfoFromStandardAccessToken(atResponse);

		UserProfileFetcher userAttributesFetcher = new UserProfileFetcher();
		UserAttributes fetchRet = userAttributesFetcher.fetchProfile(accessToken, 
				configuration.userInfoEndpoint, configuration.certValidator, attributesFromToken);
		fetchRet.getAttributes().putAll(attributesFromToken);
		
		log.debug("Received the following attributes from the OAuth provider: " + fetchRet.getAttributes());
		return fetchRet;
	}
	
	private Map<String, List<String>> extractUserInfoFromStandardAccessToken(AccessTokenResponse atResponse)
	{
		Map<String, List<String>> ret = new HashMap<>();
		Map<String, Object> customParameters = atResponse.getCustomParameters();
		for (Map.Entry<String, Object> e: customParameters.entrySet())
		{
			if (attributeIgnored(e.getKey()))
				continue;
			ret.put(e.getKey(), Arrays.asList(e.getValue().toString()));
		}
		return ret;
	}

	private boolean attributeIgnored(String key)
	{
		return key.equals("access_token") || key.equals("expires") || key.equals("expires_in") ||
				key.equals("id_token");
	}
	
	private BearerAccessToken extractAccessToken(AccessTokenResponse atResponse) throws AuthenticationException
	{
		AccessToken accessTokenGeneric = atResponse.getTokens().getAccessToken();
		if (!(accessTokenGeneric instanceof BearerAccessToken))
		{
			throw new AuthenticationException("OAuth provider returned an access token which is not "
					+ "the bearer token, it is unsupported and most probably a problem on "
					+ "the provider side. The received token type is: " + 
					accessTokenGeneric.getType().toString());
		}
		return (BearerAccessToken) accessTokenGeneric;
	}
}
