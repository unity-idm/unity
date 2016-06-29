/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.profile;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.entity.ContentType;
import org.apache.log4j.Logger;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.config.OrcidProviderProperties;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.utils.Log;


/**
 * Implementation of the {@link UserProfileFetcher} for ORCID. ORCID supports regular profile 
 * fetching only with paid API. The unpaid version requires to first retrieve client credentials grant
 * and then, using the retrieved access token to retrieve the actual profile. In another words it is not
 * possible to retrieve profile using the user's access token.
 * 
 * @author K. Benedyczak
 */
public class OrcidProfileFetcher implements UserProfileFetcher
{
	private static final Logger log = Log.getLogger(pl.edu.icm.unity.server.utils.Log.U_SERVER_OAUTH,
			OrcidProfileFetcher.class);
	
	@Override
	public Map<String, String> fetchProfile(BearerAccessToken accessToken, String userInfoEndpoint,
			BaseRemoteASProperties providerConfig, Map<String, String> attributesSoFar) throws Exception
	{
		ServerHostnameCheckingMode checkingMode = providerConfig.getEnumValue(
				BaseRemoteASProperties.CLIENT_HOSTNAME_CHECKING, 
				ServerHostnameCheckingMode.class);
		
		AccessToken clientAccessToken = getClientAccessToken(providerConfig, checkingMode);
		
		JSONObject userBio = fetchUserBio(providerConfig, attributesSoFar, checkingMode, clientAccessToken);
		
		return convertToFlatAttributes(userBio);
	}
	

	private Map<String, String> convertToFlatAttributes(JSONObject profile)
	{
		Map<String, String> ret = new HashMap<>();
		convertToFlatAttributes("", profile, ret);
		return ret;
	}
	
	private Map<String, String> convertToFlatAttributes(String prefix, 
			JSONObject profile, Map<String, String> ret)
	{
		for (Entry<String, Object> entry: profile.entrySet())
		{
			if (entry.getValue() != null)
			{
				Object value = entry.getValue();
				if (value instanceof JSONObject)
				{
					convertToFlatAttributes(prefix + entry.getKey() + ".", 
							(JSONObject) value, ret);
				} else if (value instanceof JSONArray)
				{
					convertToFlatAttributes(prefix + entry.getKey() + ".", 
							(JSONArray) value, ret);
				} else
				{
					ret.put(prefix + entry.getKey(), value.toString());
				}
			}
		}
		return ret;
	}
	
	private Map<String, String> convertToFlatAttributes(String prefix, 
			JSONArray element, Map<String, String> ret)
	{
		for (int i=0; i<element.size(); i++)
		{
			Object value = element.get(i);
			if (value != null)
			{
				if (value instanceof JSONObject)
				{
					convertToFlatAttributes(prefix + i + ".", 
							(JSONObject) value, ret);
				} else if (value instanceof JSONArray)
				{
					convertToFlatAttributes(prefix + i + ".", 
							(JSONArray) value, ret);
				} else
				{
					ret.put(prefix + i, value.toString());
				}
			}
		}
		return ret;
	}

	private AccessToken getClientAccessToken(BaseRemoteASProperties providerConfig,
			ServerHostnameCheckingMode checkingMode) throws Exception
	{
		AuthorizationGrant clientGrant = new ClientCredentialsGrant();

		ClientID clientID = new ClientID(providerConfig.getValue(OrcidProviderProperties.CLIENT_ID));
		Secret clientSecret = new Secret(providerConfig.getValue(OrcidProviderProperties.CLIENT_SECRET));
		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);
		Scope scope = new Scope("/read-public");

		String accessTokenEndpoint = providerConfig.getValue(OrcidProviderProperties.ACCESS_TOKEN_ENDPOINT);
		URI tokenEndpoint = new URI(accessTokenEndpoint);
		TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant, scope);

		HTTPRequest httpRequest = new CustomHTTPSRequest(request.toHTTPRequest(), 
				providerConfig.getValidator(), checkingMode);

		HTTPResponse httpResponse = httpRequest.send();
		if (log.isTraceEnabled())
			log.trace("Received client credentials grant:\n" + httpResponse.getContent());
		
		TokenResponse response = TokenResponse.parse(httpResponse);

		if (!response.indicatesSuccess()) 
		{
			throw new AuthenticationException("User's authentication was successful "
					+ "but there was a problem authenticating server (with client credentials) "
					+ "to obtain user's profile: " + response.toHTTPResponse().getContent());
		}

		AccessTokenResponse successResponse = (AccessTokenResponse) response;
		return successResponse.getTokens().getAccessToken();
	}
	
	private JSONObject fetchUserBio(BaseRemoteASProperties providerConfig, 
			Map<String, String> attributesSoFar, ServerHostnameCheckingMode checkingMode,
			AccessToken clientAccessToken) throws Exception
	{
		String userid = attributesSoFar.get("orcid");
		if (userid == null)
			throw new AuthenticationException("Authentication was successful "
					+ "but the orcid id is missing in the received access token");
		
		String userBioEndpoint = "https://pub.orcid.org/v1.2/" + userid;
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(userBioEndpoint));
		CustomHTTPSRequest httpReq = new CustomHTTPSRequest(httpReqRaw, providerConfig.getValidator(), checkingMode);
		httpReq.setAuthorization(clientAccessToken.toAuthorizationHeader());
		httpReq.setAccept(ContentType.APPLICATION_JSON.getMimeType());
		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: " + 
					resp.getContent());
		}
		if (log.isTraceEnabled())
			log.trace("Received user's profile:\n" + resp.getContent());

		if (resp.getContentType() == null || !ContentType.APPLICATION_JSON.getMimeType().equals(
				resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-orcid-JSON content type: " + resp.getContentType());
		
		return resp.getContentAsJSONObject();
	}
	
}
