/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.profile;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.utils.Log;


/**
 * Implementation of the {@link UserProfileFetcher} which is downloading profile from a plain 
 * endpoint: GET request which returns JSON with attributes. Authorization is done with accessToken,
 * which can be sent either as a query parameter or in authZ header (depending on configurtion).
 * @author K. Benedyczak
 */
public class PlainProfileFetcher implements UserProfileFetcher
{
	private static final Logger log = Log.getLogger(pl.edu.icm.unity.server.utils.Log.U_SERVER_OAUTH,
			PlainProfileFetcher.class);
	
	@Override
	public Map<String, String> fetchProfile(BearerAccessToken accessToken, String userInfoEndpoint,
			BaseRemoteASProperties providerConfig) throws Exception
	{
		Map<String, String> ret = new HashMap<>();
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(userInfoEndpoint));
		
		ServerHostnameCheckingMode checkingMode = providerConfig.getEnumValue(
				BaseRemoteASProperties.CLIENT_HOSTNAME_CHECKING, 
				ServerHostnameCheckingMode.class);
		
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, providerConfig.getValidator(), checkingMode);
		ClientAuthnMode selectedMethod = providerConfig.getEnumValue(
				CustomProviderProperties.CLIENT_AUTHN_MODE, ClientAuthnMode.class);
		if (selectedMethod == ClientAuthnMode.secretPost)
			httpReq.setQuery("access_token=" + accessToken.getValue());
		else
			httpReq.setAuthorization(accessToken.toAuthorizationHeader());
		
		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: " + 
					resp.getContent());
		}
		if (log.isTraceEnabled())
			log.trace("Received user's profile:\n" + resp.getContent());

		if (resp.getContentType() == null || 
				!"application/json".equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-JSON content type: " + resp.getContentType());
		
		JSONObject profile = resp.getContentAsJSONObject();
	
		for (Entry<String, Object> entry: profile.entrySet())
		{
			if (entry.getValue() != null)
				ret.put(entry.getKey(), entry.getValue().toString());
		}
		return ret;
	}
}
