/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.URLUtils;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;

/**
 * Implementation of the {@link UserProfileFetcher} which is downloading profile
 * from a plain endpoint: GET request which returns JSON with attributes.
 * Authorization is done with accessToken, which can be sent either as a query
 * parameter or in authZ header (depending on configurtion).
 * 
 * @author K. Benedyczak
 */
public class PlainProfileFetcher implements UserProfileFetcher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			PlainProfileFetcher.class);

	@Override
	public AttributeFetchResult fetchProfile(BearerAccessToken accessToken,
			String userInfoEndpoint, BaseRemoteASProperties providerConfig,
			Map<String, List<String>> attributesSoFar) throws Exception
	{

		Map<String, List<String>> queryParams = new HashMap<>();
		URIBuilder uri = new URIBuilder(userInfoEndpoint);
		queryParams.putAll(uri.getQueryParams()
				.stream().collect(Collectors.toMap(NameValuePair::getName,
						nvp -> Lists.newArrayList(nvp.getValue()))));	
		uri.clearParameters();
		HTTPRequest httpReqRaw = new HTTPRequest(
				providerConfig.getClientHttpMethodForProfileAccess(),
				uri.build().toURL());

		ServerHostnameCheckingMode checkingMode = providerConfig.getEnumValue(
				BaseRemoteASProperties.CLIENT_HOSTNAME_CHECKING,
				ServerHostnameCheckingMode.class);

		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw,
				providerConfig.getValidator(), checkingMode);
		
		
		if (providerConfig.getClientAuthModeForProfileAccess() == ClientAuthnMode.secretPost)
			queryParams.put("access_token", Lists.newArrayList(accessToken.getValue()));
		else
			httpReq.setAuthorization(accessToken.toAuthorizationHeader());

		httpReq.setAccept("application/json");
		
		if (!queryParams.isEmpty())
		{
			httpReq.setQuery(URLUtils.serializeParameters(queryParams));
		}
		
		HTTPResponse resp = httpReq.send();

		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: "
					+ resp.getContent());
		}
		if (log.isTraceEnabled())
			log.trace("Received user's profile:\n" + resp.getContent());

		if (resp.getContentType() == null || !"application/json"
				.equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-JSON content type: " + resp.getContentType());

		JSONObject profile = resp.getContentAsJSONObject();	
	
		return ProfileFetcherUtils.fetchFromJsonObject(profile);
	}
}
